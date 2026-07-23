#!/usr/bin/env python3
import argparse
import multiprocessing
import os
import platform
import re
import shutil
import stat
import subprocess
import sys
from pathlib import Path


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
clean_targets = {"app"}

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


def find_jdk():
    env = os.environ.copy()
    if "ANDROID_STUDIO" in env:
        studio = env["ANDROID_STUDIO"]
        jbr = Path(studio, "jbr", "bin")
        if not jbr.exists():
            jbr = Path(studio, "Contents", "jbr", "Contents", "Home", "bin")
        if jbr.exists():
            env["PATH"] = f'{jbr}{os.pathsep}{env["PATH"]}'

    no_jdk = False
    try:
        proc = subprocess.run(
            "javac -version",
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            env=env,
            shell=True,
        )
        no_jdk = proc.returncode != 0
    except FileNotFoundError:
        no_jdk = True

    if no_jdk:
        error(
            "Please set Android Studio's path to environment variable ANDROID_STUDIO,\n"
            + "or install JDK 21 and make sure 'javac' is available in PATH"
        )

    return env


def prepare_jni_dirs():
    # Create required jniLibs ABI subdirectories so AGP sync Release/Debug JNI tasks pass evaluation
    base_jni = Path("app", "core", "src", "main", "jniLibs")
    for abi in build_abis.keys():
        d = base_jni / abi
        d.mkdir(parents=True, exist_ok=True)
        # Create a dummy keeps file if directory is empty
        keep_file = d / ".keep"
        if not keep_file.exists():
            keep_file.touch()


def build_apk(module: str):
    ensure_paths()
    prepare_jni_dirs()
    env = find_jdk()
    props = args.config.resolve()

    os.chdir("app")
    build_type = "Release" if args.release else "Debug"
    proc = execv(
        [
            gradlew,
            f"{module}:assemble{build_type}",
            f"-PconfigPath={props}",
            f"-PabiList={','.join(build_abis.keys())}",
        ],
        env=env,
    )
    os.chdir("..")
    if proc.returncode != 0:
        error(f"Build {module} failed!")

    build_type = build_type.lower()
    paths = module.split(":")

    apk = f"{paths[-1]}-{build_type}.apk"
    source = Path("app", *paths, "build", "outputs", "apk", build_type, apk)
    target = config["outdir"] / apk
    mv(source, target)
    return target


def build_app():
    header("* Building the Magisk app")
    apk = build_apk(":apk")

    build_type = "release" if args.release else "debug"

    source = apk
    target = apk.parent / apk.name.replace("apk-", "app-")
    mv(source, target)
    header(f"Output: {target}")

    source = Path("app", "core", "src", build_type, "assets", "stub.apk")
    target = config["outdir"] / f"stub-{build_type}.apk"
    cp(source, target)


def build_stub():
    header("* Building the stub app")
    apk = build_apk(":stub")
    header(f"Output: {apk}")


def build_test():
    old_release = args.release
    args.release = True
    try:
        header("* Building the test app")
        source = build_apk(":test")
        target = source.parent / "test.apk"
        mv(source, target)
        header(f"Output: {target}")
    finally:
        args.release = old_release


def cleanup():
    ensure_paths()
    header("* Cleaning app")
    os.chdir("app")
    execv([gradlew, ":clean"], env=find_jdk())
    os.chdir("..")


def build_all():
    build_app()
    build_test()


def ensure_paths():
    global sdk_path, gradlew

    if "sdk_path" in globals():
        return

    try:
        sdk_path = Path(os.environ["ANDROID_HOME"])
    except KeyError:
        try:
            sdk_path = Path(os.environ["ANDROID_SDK_ROOT"])
        except KeyError:
            error("Please set Android SDK path to environment variable ANDROID_HOME")

    gradlew = Path.cwd() / "app" / "gradlew"
    if gradlew.exists() and not is_windows:
        gradlew.chmod(gradlew.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


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
    config["outdir"] = "out"

    if args.config.exists():
        config.update(parse_props(args.config))

    gradle_props = Path("app", "gradle.properties")
    if gradle_props.exists():
        for key, value in parse_props(gradle_props).items():
            if key.startswith("magisk."):
                config[key[7:]] = value

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


def parse_args():
    parser = argparse.ArgumentParser(description="Magisk build script")
    parser.set_defaults(func=lambda x: None)
    parser.add_argument(
        "-r", "--release", action="store_true", help="compile in release mode"
    )
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

    all_parser = subparsers.add_parser("all", help="build everything")
    app_parser = subparsers.add_parser("app", help="build the Magisk app")
    stub_parser = subparsers.add_parser("stub", help="build the stub app")
    test_parser = subparsers.add_parser("test", help="build the test app")
    clean_parser = subparsers.add_parser("clean", help="cleanup")

    all_parser.set_defaults(func=build_all)
    app_parser.set_defaults(func=build_app)
    stub_parser.set_defaults(func=build_stub)
    test_parser.set_defaults(func=build_test)
    clean_parser.set_defaults(func=cleanup)

    if len(sys.argv) == 1:
        parser.print_help()
        sys.exit(1)

    return parser.parse_args()


def main():
    global args
    args = parse_args()
    args.config = Path(args.config)
    load_config()
    args.func()


if __name__ == "__main__":
    main()
