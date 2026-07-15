#!/usr/bin/env python3
import socket
import sys
import threading
import signal

DEFAULT_PORT = 26268
BANNER = "MagiskV Remote Access Shell (use Ctrl+C or 'exit' to disconnect)"


def spinner(msg):
    sys.stderr.write(f"\r* {msg}")
    sys.stderr.flush()


def receive_output(sock):
    try:
        while True:
            data = sock.recv(4096)
            if not data:
                break
            sys.stdout.buffer.write(data)
            sys.stdout.buffer.flush()
    except (ConnectionResetError, BrokenPipeError, OSError):
        pass
    finally:
        sys.stderr.write("\n! Connection closed by remote host\n")
        sys.exit(1)


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <host> [port]")
        print(f"       {sys.argv[0]} <host>:<port>")
        sys.exit(1)

    host_arg = sys.argv[1]
    port = DEFAULT_PORT

    if ":" in host_arg and sys.argv[1].count(":") == 1:
        host, port_str = host_arg.split(":")
        port = int(port_str)
    elif len(sys.argv) >= 3:
        host = host_arg
        port = int(sys.argv[2])
    else:
        host = host_arg

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(10)

    try:
        spinner(f"Connecting to {host}:{port}")
        sock.connect((host, port))
        sock.settimeout(None)
        sys.stderr.write(f"\r* Connected to {host}:{port}\n")
        sys.stderr.write(f"{BANNER}\n")
        sys.stderr.write("-" * 50 + "\n")
    except Exception as e:
        sys.stderr.write(f"\r! Connection failed: {e}\n")
        sys.exit(1)

    signal.signal(signal.SIGINT, lambda s, f: sys.exit(0))

    recv_thread = threading.Thread(target=receive_output, args=(sock,), daemon=True)
    recv_thread.start()

    try:
        for line in sys.stdin.buffer:
            sock.sendall(line)
    except (BrokenPipeError, OSError):
        pass
    finally:
        sock.close()


if __name__ == "__main__":
    main()
