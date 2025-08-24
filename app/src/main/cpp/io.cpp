#include <jni.h>
#include <string>
#include <android/log.h>
#include <dlfcn.h>
#include <regex>
#include <cstring>
#include <cstdlib>
#include <vector>
#include <map>
#include <functional>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdarg.h>
#include "lspc/Dobby/include/dobby.h"

#define LOG_TAG "bypass"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::vector<std::pair<std::string, std::vector<std::regex>>> redirect_rules;

static char* replace_path(const char* original_path) {
    if (original_path == nullptr) {
        return nullptr;
    }

    std::string path(original_path);

    for (const auto& rule : redirect_rules) {
        const std::string& target_path = rule.first;
        const std::vector<std::regex>& patterns = rule.second;

        for (const auto& pattern : patterns) {
            std::smatch match;
            if (std::regex_search(path, match, pattern)) {
                // 找到匹配，进行替换
                std::string new_path = std::regex_replace(path, pattern, target_path);
                LOGD("Redirect: %s -> %s", path.c_str(), new_path.c_str());
                return strdup(new_path.c_str());
            }
        }
    }

    return strdup(original_path);
}

#define DEFINE_ORIGINAL_FUNC(return_type, func_name, ...) \
    static return_type (*orig_##func_name)(__VA_ARGS__) = nullptr

DEFINE_ORIGINAL_FUNC(int, faccessat, int, const char*, int, int);
DEFINE_ORIGINAL_FUNC(int, fchmodat, int, const char*, mode_t, int);
DEFINE_ORIGINAL_FUNC(int, fstatat64, int, const char*, struct stat*, int);
DEFINE_ORIGINAL_FUNC(int, mknodat, int, const char*, mode_t, dev_t);
DEFINE_ORIGINAL_FUNC(int, mknod, const char*, mode_t, dev_t);
DEFINE_ORIGINAL_FUNC(int, fchownat, int, const char*, uid_t, gid_t, int);
DEFINE_ORIGINAL_FUNC(int, unlinkat, int, const char*, int);
DEFINE_ORIGINAL_FUNC(int, unlink, const char*);
DEFINE_ORIGINAL_FUNC(int, symlinkat, const char*, int, const char*);
DEFINE_ORIGINAL_FUNC(int, symlink, const char*, const char*);
DEFINE_ORIGINAL_FUNC(int, linkat, int, const char*, int, const char*, int);
DEFINE_ORIGINAL_FUNC(int, link, const char*, const char*);
DEFINE_ORIGINAL_FUNC(int, access, const char*, int);
DEFINE_ORIGINAL_FUNC(int, chmod, const char*, mode_t);
DEFINE_ORIGINAL_FUNC(int, chown, const char*, uid_t, gid_t);
DEFINE_ORIGINAL_FUNC(int, lstat, const char*, struct stat*);
DEFINE_ORIGINAL_FUNC(int, stat, const char*, struct stat*);
DEFINE_ORIGINAL_FUNC(int, mkdirat, int, const char*, mode_t);
DEFINE_ORIGINAL_FUNC(int, mkdir, const char*, mode_t);
DEFINE_ORIGINAL_FUNC(int, rmdir, const char*);
DEFINE_ORIGINAL_FUNC(ssize_t, readlinkat, int, const char*, char*, size_t);
DEFINE_ORIGINAL_FUNC(ssize_t, readlink, const char*, char*, size_t);
DEFINE_ORIGINAL_FUNC(int, truncate64, const char*, off_t);
DEFINE_ORIGINAL_FUNC(int, openat, int, const char*, int, ...);
DEFINE_ORIGINAL_FUNC(int, openat64, int, const char*, int, ...);
DEFINE_ORIGINAL_FUNC(int, open, const char*, int, ...);
DEFINE_ORIGINAL_FUNC(int, open64, const char*, int, ...);
DEFINE_ORIGINAL_FUNC(int, execve, const char*, char* const*, char* const*);
DEFINE_ORIGINAL_FUNC(int, chdir, const char*);

static int new_faccessat(int dirfd, const char* pathname, int mode, int flags) {
    char* new_path = replace_path(pathname);
    int result = orig_faccessat(dirfd, new_path, mode, flags);
    free(new_path);
    return result;
}

static int new_fchmodat(int dirfd, const char* pathname, mode_t mode, int flags) {
    char* new_path = replace_path(pathname);
    int result = orig_fchmodat(dirfd, new_path, mode, flags);
    free(new_path);
    return result;
}

static int new_fstatat64(int dirfd, const char* pathname, struct stat* statbuf, int flags) {
    char* new_path = replace_path(pathname);
    int result = orig_fstatat64(dirfd, new_path, statbuf, flags);
    free(new_path);
    return result;
}

static int new_mknodat(int dirfd, const char* pathname, mode_t mode, dev_t dev) {
    char* new_path = replace_path(pathname);
    int result = orig_mknodat(dirfd, new_path, mode, dev);
    free(new_path);
    return result;
}

static int new_mknod(const char* pathname, mode_t mode, dev_t dev) {
    char* new_path = replace_path(pathname);
    int result = orig_mknod(new_path, mode, dev);
    free(new_path);
    return result;
}

static int new_fchownat(int dirfd, const char* pathname, uid_t owner, gid_t group, int flags) {
    char* new_path = replace_path(pathname);
    int result = orig_fchownat(dirfd, new_path, owner, group, flags);
    free(new_path);
    return result;
}

static int new_unlinkat(int dirfd, const char* pathname, int flags) {
    char* new_path = replace_path(pathname);
    int result = orig_unlinkat(dirfd, new_path, flags);
    free(new_path);
    return result;
}

static int new_unlink(const char* pathname) {
    char* new_path = replace_path(pathname);
    int result = orig_unlink(new_path);
    free(new_path);
    return result;
}

static int new_symlinkat(const char* target, int newdirfd, const char* linkpath) {
    char* new_target = replace_path(target);
    char* new_linkpath = replace_path(linkpath);
    int result = orig_symlinkat(new_target, newdirfd, new_linkpath);
    free(new_target);
    free(new_linkpath);
    return result;
}

static int new_symlink(const char* target, const char* linkpath) {
    char* new_target = replace_path(target);
    char* new_linkpath = replace_path(linkpath);
    int result = orig_symlink(new_target, new_linkpath);
    free(new_target);
    free(new_linkpath);
    return result;
}

static int new_linkat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, int flags) {
    char* new_oldpath = replace_path(oldpath);
    char* new_newpath = replace_path(newpath);
    int result = orig_linkat(olddirfd, new_oldpath, newdirfd, new_newpath, flags);
    free(new_oldpath);
    free(new_newpath);
    return result;
}

static int new_link(const char* oldpath, const char* newpath) {
    char* new_oldpath = replace_path(oldpath);
    char* new_newpath = replace_path(newpath);
    int result = orig_link(new_oldpath, new_newpath);
    free(new_oldpath);
    free(new_newpath);
    return result;
}

static int new_access(const char* pathname, int mode) {
    char* new_path = replace_path(pathname);
    int result = orig_access(new_path, mode);
    free(new_path);
    return result;
}

static int new_chmod(const char* pathname, mode_t mode) {
    char* new_path = replace_path(pathname);
    int result = orig_chmod(new_path, mode);
    free(new_path);
    return result;
}

static int new_chown(const char* pathname, uid_t owner, gid_t group) {
    char* new_path = replace_path(pathname);
    int result = orig_chown(new_path, owner, group);
    free(new_path);
    return result;
}

static int new_lstat(const char* pathname, struct stat* statbuf) {
    char* new_path = replace_path(pathname);
    int result = orig_lstat(new_path, statbuf);
    free(new_path);
    return result;
}

static int new_stat(const char* pathname, struct stat* statbuf) {
    char* new_path = replace_path(pathname);
    int result = orig_stat(new_path, statbuf);
    free(new_path);
    return result;
}

static int new_mkdirat(int dirfd, const char* pathname, mode_t mode) {
    char* new_path = replace_path(pathname);
    int result = orig_mkdirat(dirfd, new_path, mode);
    free(new_path);
    return result;
}

static int new_mkdir(const char* pathname, mode_t mode) {
    char* new_path = replace_path(pathname);
    int result = orig_mkdir(new_path, mode);
    free(new_path);
    return result;
}

static int new_rmdir(const char* pathname) {
    char* new_path = replace_path(pathname);
    int result = orig_rmdir(new_path);
    free(new_path);
    return result;
}

static ssize_t new_readlinkat(int dirfd, const char* pathname, char* buf, size_t bufsiz) {
    char* new_path = replace_path(pathname);
    ssize_t result = orig_readlinkat(dirfd, new_path, buf, bufsiz);
    free(new_path);
    return result;
}

static ssize_t new_readlink(const char* pathname, char* buf, size_t bufsiz) {
    char* new_path = replace_path(pathname);
    ssize_t result = orig_readlink(new_path, buf, bufsiz);
    free(new_path);
    return result;
}

static int new_truncate64(const char* path, off_t length) {
    char* new_path = replace_path(path);
    int result = orig_truncate64(new_path, length);
    free(new_path);
    return result;
}

static int new_openat(int dirfd, const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    char* new_path = replace_path(pathname);
    int result = orig_openat(dirfd, new_path, flags, mode);
    free(new_path);
    return result;
}

static int new_openat64(int dirfd, const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    char* new_path = replace_path(pathname);
    int result = orig_openat64(dirfd, new_path, flags, mode);
    free(new_path);
    return result;
}

static int new_open(const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    char* new_path = replace_path(pathname);
    int result = orig_open(new_path, flags, mode);
    free(new_path);
    return result;
}

static int new_open64(const char* pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    char* new_path = replace_path(pathname);
    int result = orig_open64(new_path, flags, mode);
    free(new_path);
    return result;
}

static int new_execve(const char* pathname, char* const* argv, char* const* envp) {
    char* new_path = replace_path(pathname);
    int result = orig_execve(new_path, argv, envp);
    free(new_path);
    return result;
}

static int new_chdir(const char* path) {
    char* new_path = replace_path(path);
    int result = orig_chdir(new_path);
    free(new_path);
    return result;
}

static void install_hooks() {
    const char* libc_path = "/apex/com.android.runtime/lib64/bionic/libc.so";

    std::map<std::string, std::pair<void**, void*>> hook_functions = {
            {"faccessat", {(void**)&orig_faccessat, (void*)new_faccessat}},
            {"fchmodat", {(void**)&orig_fchmodat, (void*)new_fchmodat}},
            {"fstatat64", {(void**)&orig_fstatat64, (void*)new_fstatat64}},
            {"mknodat", {(void**)&orig_mknodat, (void*)new_mknodat}},
            {"mknod", {(void**)&orig_mknod, (void*)new_mknod}},
            {"fchownat", {(void**)&orig_fchownat, (void*)new_fchownat}},
            {"unlinkat", {(void**)&orig_unlinkat, (void*)new_unlinkat}},
            {"unlink", {(void**)&orig_unlink, (void*)new_unlink}},
            {"symlinkat", {(void**)&orig_symlinkat, (void*)new_symlinkat}},
            {"symlink", {(void**)&orig_symlink, (void*)new_symlink}},
            {"linkat", {(void**)&orig_linkat, (void*)new_linkat}},
            {"link", {(void**)&orig_link, (void*)new_link}},
            {"access", {(void**)&orig_access, (void*)new_access}},
            {"chmod", {(void**)&orig_chmod, (void*)new_chmod}},
            {"chown", {(void**)&orig_chown, (void*)new_chown}},
            {"lstat", {(void**)&orig_lstat, (void*)new_lstat}},
            {"stat", {(void**)&orig_stat, (void*)new_stat}},
            {"mkdirat", {(void**)&orig_mkdirat, (void*)new_mkdirat}},
            {"mkdir", {(void**)&orig_mkdir, (void*)new_mkdir}},
            {"rmdir", {(void**)&orig_rmdir, (void*)new_rmdir}},
            {"readlinkat", {(void**)&orig_readlinkat, (void*)new_readlinkat}},
            {"readlink", {(void**)&orig_readlink, (void*)new_readlink}},
            {"truncate64", {(void**)&orig_truncate64, (void*)new_truncate64}},
            {"openat", {(void**)&orig_openat, (void*)new_openat}},
            {"openat64", {(void**)&orig_openat64, (void*)new_openat64}},
            {"open", {(void**)&orig_open, (void*)new_open}},
            {"open64", {(void**)&orig_open64, (void*)new_open64}},
            {"execve", {(void**)&orig_execve, (void*)new_execve}},
            {"chdir", {(void**)&orig_chdir, (void*)new_chdir}}
    };

    for (const auto& func : hook_functions) {
        void* symbol = DobbySymbolResolver(libc_path, func.first.c_str());
        if (symbol) {
            int result = DobbyHook(symbol, func.second.second, func.second.first);
            if (result == 0) {
//                LOGD("Successfully hooked %s", func.first.c_str());
            } else {
//                LOGE("Failed to hook %s, error: %d", func.first.c_str(), result);
            }
        } else {
//            LOGE("Failed to find symbol for %s", func.first.c_str());
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_crack_vapp_core_RedirectIO_redirectIO(
        JNIEnv* env,
        jclass clazz,
        jobjectArray redirectRulesArray) {

    redirect_rules.clear();

    jsize outerLength = env->GetArrayLength(redirectRulesArray);
    for (jsize i = 0; i < outerLength; i++) {
        jobjectArray innerArray = (jobjectArray)env->GetObjectArrayElement(redirectRulesArray, i);
        jsize innerLength = env->GetArrayLength(innerArray);

        if (innerLength < 2) {
            continue;
        }

        jstring targetJStr = (jstring)env->GetObjectArrayElement(innerArray, 0);
        const char* targetPath = env->GetStringUTFChars(targetJStr, nullptr);
        std::string target_path_str(targetPath);
        env->ReleaseStringUTFChars(targetJStr, targetPath);

        std::vector<std::regex> patterns;
        for (jsize j = 1; j < innerLength; j++) {
            jstring patternJStr = (jstring)env->GetObjectArrayElement(innerArray, j);
            const char* pattern = env->GetStringUTFChars(patternJStr, nullptr);
            patterns.emplace_back(pattern);
            env->ReleaseStringUTFChars(patternJStr, pattern);
        }

        redirect_rules.emplace_back(target_path_str, patterns);
//        LOGD("Added rule: %s -> %lu patterns", target_path_str.c_str(), patterns.size());
    }

    install_hooks();
}