#include <jni.h>
#include <string>
#include <android/log.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <malloc.h>
#include <unistd.h>
#include <dirent.h>
#include <cstring>
#include <sys/prctl.h>
#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <sys/syscall.h>
#include <signal.h>
#include <ucontext.h>

#define LOG_TAG "bypass"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static char* g_target_apk_name = NULL;

void sig_callback(int signo, siginfo_t* info, void* data) {
    if (signo != SIGSYS) return;

    unsigned long syscall_number = ((ucontext_t*)data)->uc_mcontext.regs[8];
    unsigned long pathname_ptr = ((ucontext_t*)data)->uc_mcontext.regs[1];

    if (syscall_number == __NR_openat) {
        char pathname[256] = {0};
        if (pathname_ptr != 0) {
            size_t i;
            for (i = 0; i < sizeof(pathname) - 1; i++) {
                char c;
                if (syscall(__NR_read, __NR_process_vm_readv, &c, 1, pathname_ptr + i, 0) != 1) {
                    break;
                }
                if (c == '\0') break;
                pathname[i] = c;
            }
            pathname[i] = '\0';
        }

        if (strstr(pathname, "/data/app/") && strstr(pathname, "/base.apk")) {
//            ALOGE("detected access : %s", pathname);

            char redirect_path[512];
            snprintf(redirect_path, sizeof(redirect_path),
                     "/sdcard/BypassSignature/%s", g_target_apk_name);

//            ALOGE("redirect to: %s", redirect_path);

            ((ucontext_t*)data)->uc_mcontext.regs[1] = (unsigned long)strdup(redirect_path);
        }
    }
}

void init_seccomp_impl() {
    struct sock_filter filter[] = {
            BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),
            BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_openat, 0, 1),
            BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_TRAP),
            BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW)
    };

    struct sock_fprog prog = {
            .len = (unsigned short)(sizeof(filter) / sizeof(filter[0])),
            .filter = filter
    };

    struct sigaction sa;
    sigset_t sigset;
    sigfillset(&sigset);
    sa.sa_sigaction = sig_callback;
    sa.sa_mask = sigset;
    sa.sa_flags = SA_SIGINFO;
    sigaction(SIGSYS, &sa, NULL);

    prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0);
    prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog);
}

extern "C" JNIEXPORT void JNICALL
Java_com_crack_vapp_core_BypassSignature_initSeccomp(
        JNIEnv* env, jclass clazz, jstring targetApkName) {

    const char* apk_name = env->GetStringUTFChars(targetApkName, NULL);
    if (g_target_apk_name) free(g_target_apk_name);
    g_target_apk_name = strdup(apk_name);
    env->ReleaseStringUTFChars(targetApkName, apk_name);

    init_seccomp_impl();
}

bool bypassHiddenApi(JNIEnv* env) {
    jclass zygoteInitClass = env->FindClass("com/android/internal/os/ZygoteInit");
    if (zygoteInitClass == nullptr) {
//        ALOGE("not found class");
        env->ExceptionClear();
        return false;
    }

    jmethodID setApiBlackListApiMethod = env->GetStaticMethodID(
            zygoteInitClass,
            "setApiBlacklistExemptions",
            "([Ljava/lang/String;)V"
    );
    if (setApiBlackListApiMethod == nullptr) {
        env->ExceptionClear();
        setApiBlackListApiMethod = env->GetStaticMethodID(
                zygoteInitClass,
                "setApiDenylistExemptions",
                "([Ljava/lang/String;)V"
        );
    }
    if (setApiBlackListApiMethod == nullptr) {
//        ALOGE("not found method");
        return false;
    }

    jclass stringCLass = env->FindClass("java/lang/String");
    jstring newStr = env->NewStringUTF("L");
    jobjectArray newArray = env->NewObjectArray(1, stringCLass, NULL);
    env->SetObjectArrayElement(newArray, 0, newStr);

    env->CallStaticVoidMethod(zygoteInitClass, setApiBlackListApiMethod, newArray);

    env->DeleteLocalRef(newStr);
    env->DeleteLocalRef(newArray);

    return true;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    jint result = -1;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return result;
    }

    if (!bypassHiddenApi(env)) {
        return result;
    }

    return JNI_VERSION_1_6;
}