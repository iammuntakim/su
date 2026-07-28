#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
#include <algorithm>

#include <base.hpp>
#include <core.hpp>

#include "hideroot.hpp"

using namespace std;

struct MountEntry {
    string source;
    string target;
    string fs_type;
    string fs_option;
};

static vector<MountEntry> parse_mountinfo() {
    vector<MountEntry> mounts;
    FILE *fp = fopen("/proc/self/mountinfo", "re");
    if (!fp) return mounts;

    char *line = nullptr;
    size_t len = 0;
    while (getline(&line, &len, fp) > 0) {
        MountEntry e;
        int id, parent, maj, min;
        char root[4096] = {}, target[4096] = {}, vfsOpt[4096] = {};
        char src[4096] = {}, fsType[4096] = {}, superOpt[4096] = {};

        int n = sscanf(line, "%d %d %d:%d %4095s %4095s %4095s %*s %4095s %4095s %4095s",
                       &id, &parent, &maj, &min, root, target, vfsOpt,
                       src, fsType, superOpt);
        if (n >= 7) {
            e.target = target;
            if (n >= 8) e.source = src;
            if (n >= 9) e.fs_type = fsType;
            if (n >= 10) e.fs_option = superOpt;
            mounts.push_back(move(e));
        }
    }
    free(line);
    fclose(fp);
    return mounts;
}

static void doUnmount() {
    auto mounts = parse_mountinfo();
    vector<string> targets;

    for (auto &m : mounts) {
        if (m.target.find("/data/adb") == 0 ||
            m.target.find("/debug_ramdisk") == 0) {
            targets.push_back(m.target);
        }
        if (m.source.find("k su") != string::npos ||
            m.source.find("apatch") != string::npos ||
            m.source.find("worker") != string::npos ||
            m.source == "magisk") {
            targets.push_back(m.target);
        }
    }

    sort(targets.begin(), targets.end(), greater<string>());
    for (auto &t : targets) {
        xumount2(t.c_str(), MNT_DETACH);
    }
}

static void doRemount() {
    auto mounts = parse_mountinfo();
    for (auto &m : mounts) {
        if (m.target == "/data") {
            string opts = m.fs_option;
            if (opts.find("errors=") == string::npos) {
                if (!opts.empty()) opts += ",";
                opts += "errors=continue";
            }
            xmount(m.source.c_str(), "/data", m.fs_type.c_str(),
                   MS_REMOUNT, opts.c_str());
            break;
        }
    }
}

static void doHideZygisk() {
    void *handle = dlopen("libnativebridge.so", RTLD_LAZY);
    if (!handle) return;

    struct NBCallbacks {
        uint32_t version;
        void *padding[5];
        bool (*isCompatibleWith)(uint32_t);
    };

    auto nbc_ptr = reinterpret_cast<NBCallbacks **>(
            dlsym(handle, "NativeBridgeItf"));
    if (nbc_ptr && *nbc_ptr) {
        auto *nbc = *nbc_ptr;
        auto *bytes = reinterpret_cast<volatile uint8_t *>(nbc);
        for (size_t off = sizeof(uint32_t); off < sizeof(NBCallbacks); ++off) {
            if (bytes[off] != 0) {
                const_cast<uint8_t *>(bytes)[off] = 0;
            }
        }
    }
    dlclose(handle);
}

static void doMrProp() {
    using setprop_t = int (*)(const char *, const char *);
    auto setprop = reinterpret_cast<setprop_t>(
            dlsym(RTLD_DEFAULT, "__system_property_set"));
    if (!setprop) return;

    setprop("ro.debuggable", "0");
    setprop("ro.secure", "1");
    setprop("ro.allow.mock.location", "0");
    setprop("ro.build.tags", "release-keys");
}

static void hideroot_child() {
    doUnmount();
    doRemount();
    doHideZygisk();
    doMrProp();
    _exit(0);
}

void exec_hideroot() {
    if (xunshare(CLONE_NEWNS) != 0) return;

    xmount(nullptr, "/", nullptr, MS_SLAVE | MS_REC, nullptr);

    pid_t pid = xfork();
    if (pid == 0) {
        hideroot_child();
    } else if (pid > 0) {
        int status;
        waitpid(pid, &status, 0);
    }
}
