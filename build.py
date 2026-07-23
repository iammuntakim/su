import argparse
import glob
import multiprocessing
import os
import platform
import re
import shutil
import stat
import subprocess
import sys
import tarfile
import urllib.request
import zipfile
from pathlib import Path
from zipfile import ZipFile

def color_print(code, str):
    if no_color:
        print(str)
    else:
        str = str.replace("\n", f"\033[0m\n{code}")
        print(f"{code}{str}\033[0m")

def error(str):
    color_print("\033[41;39m", f"\n! {str}\n")
    sys.exit(1)

def header(str):
    color_print("\033[44;39m", f"\n{str}\n")

def vprint(str):
    if args.verbose > 0:
        print(str)

os_name = platform.system().lower()
is_windows = False
if os_name != "linux" and os_name != "darwin":
    is_windows = True
    os_name = "windows"
EXE_EXT = ".exe" if is_windows else ""

no_color = False
if is_windows:
    try:
        import colorama
        colorama.init()
    except ImportError:
        no_color = True

if not sys.version_info >= (3, 8):
    error("Requires Python 3.8+")

cpu_count = multiprocessing.cpu_count()

support_abis = {
    "armeabi-v7a": "thumbv7neon-linux-androideabi",
    "arm64-v8a": "aarch64-linux-android",
    "riscv64": "riscv64-linux-android",
}
abi_alias = {
    "arm": "armeabi-v7a",
    "arm32": "armeabi-v7a",
    "arm64": "arm64-v8a",
}
default_abis = support_abis.keys() - {"riscv64"}
support_targets = {"magisk", "magiskinit", "magiskboot", "magiskpolicy", "resetprop"}
default_targets = support_targets - {"resetprop"}
rust_targets = default_targets.copy()
clean_targets = {"native", "cpp", "rust"}
ondk_version = "r29.5"

config = {}
args: argparse.Namespace
build_abis: dict[str, str]
force_out = False

def mv(source: Path, target: Path):
    try:
        shutil.move(source, target)
        vprint(f"mv {source} -> {target}")
    except:
        pass

def cp(source: Path, target: Path):
    try:
        shutil.copyfile(source, target)
        vprint(f"cp {source} -> {target}")
    except:
        pass

def rm(file: Path):
    try:
        os.remove(file)
        vprint(f"rm {file}")
    except FileNotFoundError:
        pass

def rm_on_error(func, path, _):
    try:
        os.chmod(path, stat.S_IWRITE)
        os.unlink(path)
    except FileNotFoundError:
        pass

def rm_rf(path: Path):
    vprint(f"rm -rf {path}")
    if sys.version_info >= (3, 12):
        shutil.rmtree(path, ignore_errors=False, onexc=rm_on_error)
    else:
        shutil.rmtree(path, ignore_errors=False, onerror=rm_on_error)

def execv(cmds: list, env=None):
    out = None if force_out or args.verbose > 0 else subprocess.DEVNULL
    return subprocess.run(cmds, stdout=out, env=env, shell=is_windows)

def cmd_out(cmds: list):
    return (
        subprocess.run(
            cmds,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            shell=is_windows,
        )
        .stdout.strip()
        .decode("utf-8")
    )

def strip_binary(file_path: Path):
    strip_tool = ndk_path / "toolchains" / "llvm" / "prebuilt" / f"{os_name}-x86_64" / "bin" / f"llvm-strip{EXE_EXT}"
    if not strip_tool.exists():
        strip_tool = shutil.which("strip")
    if strip_tool and file_path.exists():
        vprint(f"Stripping: {file_path}")
        subprocess.run([str(strip_tool), "--strip-all", "--remove-section=.comment", "--remove-section=.note", str(file_path)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def clean_elf():
    cargo_toml = Path("tools", "elf-cleaner", "Cargo.toml")
    cmds = ["run", "--release", "--manifest-path", cargo_toml]
    if args.verbose == 0:
        cmds.append("-q")
    elif args.verbose > 1:
        cmds.append("--verbose")
    cmds.append("--")
    cmds.extend(glob.glob("native/out/lib/*/magisk"))
    cmds.extend(glob.glob("native/out/lib/*/magiskpolicy"))
    run_cargo(cmds)

def collect_ndk_build():
    for arch in build_abis.keys():
        arch_dir = Path("native", "libs", arch)
        out_dir = Path("native", "out", "lib", arch)
        out_dir.mkdir(parents=True, exist_ok=True)
        for source in arch_dir.iterdir():
            target = out_dir / source.name
            mv(source, target)
            strip_binary(target)

def run_ndk_build(cmds: list[str]):
    os.chdir("native")
    cmds.append("NDK_PROJECT_PATH=.")
    cmds.append("NDK_APPLICATION_MK=src/Application.mk")
    cmds.append(f"APP_ABI={' '.join(build_abis.keys())}")
    cmds.append(f"-j{cpu_count}")
    cmds.append("APP_CFLAGS+=-O3 -flto -fomit-frame-pointer -ffunction-sections -fdata-sections")
    cmds.append("APP_CPPFLAGS+=-O3 -flto -fomit-frame-pointer -ffunction-sections -fdata-sections")
    cmds.append("APP_LDFLAGS+=-Wl,-O3,--gc-sections,--icf=all,-z,now,-z,relro,--strip-all")
    if args.verbose > 1:
        cmds.append("V=1")
    cmds.append("MAGISK_DEBUG=0")
    proc = execv([ndk_build, *cmds])
    if proc.returncode != 0:
        error("Build binary failed!")
    os.chdir("..")

def build_cpp_src(targets: set[str]):
    cmds = []
    clean = False

    if "magisk" in targets:
        cmds.append("B_MAGISK=1")
        clean = True

    if "magiskpolicy" in targets:
        cmds.append("B_POLICY=1")
        clean = True

    if "magiskinit" in targets:
        cmds.append("B_PRELOAD=1")

    if "resetprop" in targets:
        cmds.append("B_PROP=1")

    if cmds:
        run_ndk_build(cmds)
        collect_ndk_build()

    cmds.clear()

    if "magiskinit" in targets:
        cmds.append("B_INIT=1")

    if "magiskboot" in targets:
        cmds.append("B_BOOT=1")

    if cmds:
        cmds.append("B_CRT0=1")
        run_ndk_build(cmds)
        collect_ndk_build()

    if clean:
        clean_elf()

def run_cargo(cmds: list[str]):
    ensure_paths()
    env = os.environ.copy()
    env["PATH"] = f"{rust_sysroot / 'bin'}{os.pathsep}{env['PATH']}"
    opt_rustflags = f"-Z threads={min(8, cpu_count)} -C opt-level=3 -C lto=fat -C codegen-units=1 -C panic=abort -C embed-bitcode=yes -C link-arg=-s -C link-arg=-Wl,--gc-sections -C link-arg=-Wl,--icf=all"
    if "CARGO_BUILD_RUSTFLAGS" in env:
        env["CARGO_BUILD_RUSTFLAGS"] += f" {opt_rustflags}"
    else:
        env["CARGO_BUILD_RUSTFLAGS"] = opt_rustflags

    if os_name == "darwin":
        env["DYLD_FALLBACK_LIBRARY_PATH"] = str(rust_sysroot / "lib")
    elif os_name == "linux":
        env["LD_LIBRARY_PATH"] = str(rust_sysroot / "lib")
    return execv(["cargo", *cmds], env)

def build_rust_src(targets: set[str]):
    targets = targets.copy()
    if "resetprop" in targets:
        targets.add("magisk")
    targets = targets & rust_targets
    if not targets:
        return

    os.chdir(Path("native", "src"))

    cmds = ["build", "-p", "", "-r"]
    profile = "release"

    if args.verbose == 0:
        cmds.append("-q")
    elif args.verbose > 1:
        cmds.append("--verbose")

    for triple in build_abis.values():
        cmds.append("--target")
        cmds.append(triple)

    for tgt in targets:
        cmds[2] = tgt
        proc = run_cargo(cmds)
        if proc.returncode != 0:
            error("Build binary failed!")

    os.chdir(Path("..", ".."))

    native_out = Path("native", "out", "lib")
    rust_out = Path("native", "out", "rust")
    for arch, triple in build_abis.items():
        arch_out = native_out / arch
        arch_out.mkdir(mode=0o755, parents=True, exist_ok=True)
        for tgt in targets:
            source = rust_out / triple / profile / f"lib{tgt}.a"
            target = arch_out / f"lib{tgt}-rs.a"
            mv(source, target)

def write_if_diff(file_name: Path, text: str):
    do_write = True
    if file_name.exists():
        with open(file_name, "r") as f:
            orig = f.read()
        do_write = orig != text
    if do_write:
        with open(file_name, "w") as f:
            f.write(text)

def dump_flag_header():
    flag_txt = "#pragma once\n"
    flag_txt += f'#define MAGISK_VERSION      "{config["version"]}"\n'
    flag_txt += f'#define MAGISK_VER_CODE     {config["versionCode"]}\n'
    flag_txt += "#define MAGISK_DEBUG        0\n"

    native_gen_path = Path("native", "out", "generated")
    native_gen_path.mkdir(mode=0o755, parents=True, exist_ok=True)
    write_if_diff(native_gen_path / "flags.h", flag_txt)

    rust_flag_txt = f'pub const MAGISK_VERSION: &str = "{config["version"]}";\n'
    rust_flag_txt += f'pub const MAGISK_VER_CODE: i32 = {config["versionCode"]};\n'
    write_if_diff(native_gen_path / "flags.rs", rust_flag_txt)

def ensure_toolchain():
    ensure_paths()
    try:
        with open(Path(ndk_path, "ONDK_VERSION"), "r") as ondk_ver:
            assert ondk_ver.read().strip(" \t\r\n") == ondk_version
    except:
        error('Unmatched NDK. Please install/upgrade NDK with "build.py ndk"')

    if sccache := shutil.which("sccache"):
        os.environ["RUSTC_WRAPPER"] = sccache
        os.environ["NDK_CCACHE"] = sccache
        os.environ["CARGO_INCREMENTAL"] = "0"
    if ccache := shutil.which("ccache"):
        os.environ["NDK_CCACHE"] = ccache

def package_native():
    header("* Packaging native.jar")
    lib_dir = Path("native", "out", "lib")
    zip_path = Path("native", "out", "native.zip")
    jar_path = Path("native", "out", "native.jar")
    
    with ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for root, dirs, files in os.walk(lib_dir):
            for file in files:
                full_path = Path(root, file)
                rel_path = full_path.relative_to(lib_dir.parent)
                zf.write(full_path, rel_path)
                
    if zip_path.exists():
        if jar_path.exists():
            rm(jar_path)
        mv(zip_path, jar_path)
        header(f"Output: {jar_path}")

def build_native():
    ensure_toolchain()
    Path("native", "out", "lib").mkdir(parents=True, exist_ok=True)

    if "targets" not in vars(args) or not args.targets:
        targets = default_targets
    else:
        targets = set(args.targets) & support_targets
        if not targets:
            return

    header("* Building: " + " ".join(targets))

    dump_flag_header()
    build_rust_src(targets)
    build_cpp_src(targets)
    package_native()

def cleanup():
    ensure_paths()
    if args.targets:
        targets: set[str] = set(args.targets) & clean_targets
        if "native" in targets:
            targets.add("cpp")
            targets.add("rust")
    else:
        targets = clean_targets

    if "cpp" in targets:
        header("* Cleaning C++")
        rm_rf(Path("native", "libs"))
        rm_rf(Path("native", "obj"))

    if "rust" in targets:
        header("* Cleaning Rust")
        rm_rf(Path("native", "out", "rust"))
        rm(Path("native", "src", "boot", "proto", "mod.rs"))
        rm(Path("native", "src", "boot", "proto", "update_metadata.rs"))
        for rs_gen in glob.glob("native/**/*-rs.*pp", recursive=True):
            rm(Path(rs_gen))

    if "native" in targets:
        header("* Cleaning native")
        rm_rf(Path("native", "out"))
        rm_rf(Path("tools", "elf-cleaner", "target"))

def ensure_paths():
    global sdk_path, ndk_root, ndk_path, rust_sysroot
    global ndk_build

    if "sdk_path" in globals():
        return

    try:
        sdk_path = Path(os.environ["ANDROID_HOME"])
    except KeyError:
        try:
            sdk_path = Path(os.environ["ANDROID_SDK_ROOT"])
        except KeyError:
            error("Please set Android SDK path to environment variable ANDROID_HOME")

    ndk_root = sdk_path / "ndk"
    ndk_path = ndk_root / "magisk"
    ndk_build = ndk_path / "ndk-build"
    rust_sysroot = ndk_path / "toolchains" / "rust"

def parse_props(file: Path) -> dict[str, str]:
    props = {}
    with open(file, "r") as f:
        for line in [l.strip(" \t\r\n") for l in f]:
            if line.startswith("#") or len(line) == 0:
                continue
            prop = line.split("=")
            if len(prop) != 2:
                continue
            key = prop[0].strip(" \t\r\n")
            value = prop[1].strip(" \t\r\n")
            if not key or not value:
                continue
            props[key] = value
    return props

def set_build_abis(abis: set[str]):
    global build_abis
    abis = {abi_alias.get(k, k) for k in abis}
    for k in abis - support_abis.keys():
        error(f"Unknown ABI: {k}")
    build_abis = {k: support_abis[k] for k in abis if k in support_abis}

def load_config():
    commit_hash = cmd_out(["git", "rev-parse", "--short=8", "HEAD"])

    config["version"] = commit_hash
    config["versionCode"] = 1000000
    config["outdir"] = Path("native", "out")

    if args.config.exists():
        config.update(parse_props(args.config))

    try:
        config["versionCode"] = int(config["versionCode"])
    except ValueError:
        error('Config error: "versionCode" is required to be an integer')

    config["outdir"] = Path(config["outdir"])
    config["outdir"].mkdir(mode=0o755, parents=True, exist_ok=True)

    if "abiList" in config:
        abis = set(re.split("\\s*,\\s*", config["abiList"]))
    else:
        abis = default_abis

    set_build_abis(abis)

def gen_ide():
    ensure_paths()
    set_build_abis({args.abi})

    dump_flag_header()

    os.chdir(Path("native", "src"))
    run_cargo(["check"])
    os.chdir(Path("..", ".."))

    rm_rf(Path("native", "compile_commands.json"))
    run_ndk_build(
        [
            "B_MAGISK=1",
            "B_INIT=1",
            "B_BOOT=1",
            "B_POLICY=1",
            "B_PRELOAD=1",
            "B_PROP=1",
            "B_CRT0=1",
            "compile_commands.json",
        ]
    )

def clippy_cli():
    ensure_toolchain()
    global force_out
    force_out = True
    if args.abi:
        set_build_abis(set(args.abi))
    else:
        set_build_abis(default_abis)

    os.chdir(Path("native", "src"))
    cmds = ["clippy", "--no-deps", "--target"]
    for triple in build_abis.values():
        run_cargo(cmds + [triple, "--release"])
    os.chdir(Path("..", ".."))

def cargo_cli():
    global force_out
    force_out = True
    if len(args.commands) >= 1 and args.commands[0] == "--":
        args.commands = args.commands[1:]
    os.chdir(Path("native", "src"))
    run_cargo(args.commands)
    os.chdir(Path("..", ".."))

def setup_ndk():
    ensure_paths()
    url = f"https://github.com/topjohnwu/ondk/releases/download/{ondk_version}/ondk-{ondk_version}-{os_name}.tar.xz"
    ndk_archive = url.split("/")[-1]
    ondk_path = Path(ndk_root, f"ondk-{ondk_version}")

    header(f"* Downloading and extracting {ndk_archive}")
    rm_rf(ondk_path)
    with urllib.request.urlopen(url) as response:
        with tarfile.open(mode="r|xz", fileobj=response) as tar:
            if hasattr(tarfile, "data_filter"):
                tar.extractall(ndk_root, filter="tar")
            else:
                tar.extractall(ndk_root)

    rm_rf(ondk_path)
    mv(ondk_path, ndk_path)

def setup_rustup():
    wrapper_dir = Path(args.wrapper_dir)
    rm_rf(wrapper_dir)
    wrapper_dir.mkdir(mode=0o755, parents=True, exist_ok=True)
    if "CARGO_HOME" in os.environ:
        cargo_home = Path(os.environ["CARGO_HOME"])
    else:
        cargo_home = Path.home() / ".cargo"
    cargo_bin = cargo_home / "bin"
    for src in cargo_bin.iterdir():
        tgt = wrapper_dir / src.name
        tgt.symlink_to(f"rustup{EXE_EXT}")

    wrapper_src = Path("tools", "rustup-wrapper")
    cargo_toml = wrapper_src / "Cargo.toml"
    cmds = ["build", "--release", f"--manifest-path={cargo_toml}"]
    if args.verbose > 1:
        cmds.append("--verbose")
    run_cargo(cmds)

    wrapper = wrapper_dir / (f"rustup{EXE_EXT}")
    wrapper.unlink(missing_ok=True)
    cp(wrapper_src / "target" / "release" / (f"rustup-wrapper{EXE_EXT}"), wrapper)
    wrapper.chmod(0o755)

def parse_args():
    parser = argparse.ArgumentParser(description="Native Binary Only Build Script")
    parser.set_defaults(func=lambda x: None)
    parser.add_argument(
        "-v", "--verbose", action="count", default=0, help="verbose output"
    )
    parser.add_argument(
        "-c",
        "--config",
        default="config.prop",
        help="custom config file (default: config.prop)",
    )
    subparsers = parser.add_subparsers(title="actions")

    native_parser = subparsers.add_parser("native", help="build native binaries")
    native_parser.add_argument(
        "targets",
        nargs="*",
        help=f"{', '.join(support_targets)}, or empty for defaults ({', '.join(default_targets)})",
    )

    clean_parser = subparsers.add_parser("clean", help="cleanup")
    clean_parser.add_argument(
        "targets", nargs="*", help="native, cpp, rust, or empty to clean all"
    )

    ndk_parser = subparsers.add_parser("ndk", help="setup Magisk NDK")

    cargo_parser = subparsers.add_parser(
        "cargo", help="call 'cargo' commands against the project"
    )
    cargo_parser.add_argument("commands", nargs=argparse.REMAINDER)

    clippy_parser = subparsers.add_parser("clippy", help="run clippy on Rust sources")
    clippy_parser.add_argument(
        "--abi", action="append", help="target ABI(s) to run clippy"
    )

    rustup_parser = subparsers.add_parser("rustup", help="setup rustup wrapper")
    rustup_parser.add_argument(
        "wrapper_dir", help="path to setup rustup wrapper binaries"
    )

    gen_parser = subparsers.add_parser("gen", help="generate files for IDE")
    gen_parser.add_argument("--abi", default="arm64-v8a", help="target ABI to generate")

    native_parser.set_defaults(func=build_native)
    cargo_parser.set_defaults(func=cargo_cli)
    clippy_parser.set_defaults(func=clippy_cli)
    rustup_parser.set_defaults(func=setup_rustup)
    gen_parser.set_defaults(func=gen_ide)
    clean_parser.set_defaults(func=cleanup)
    ndk_parser.set_defaults(func=setup_ndk)

    if len(sys.argv) == 1:
        parser.print_help()
        sys.exit(1)

    return parser.parse_args()

def main():
    global args
    args = parse_args()
    args.release = True
    args.config = Path(args.config)
    load_config()
    args.func()

if __name__ == "__main__":
    main()
    