#include <jni.h>
#include <android/log.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string>
#include <vector>
#include "needle.h"

#define TAG "PocketPilotNeedle"

namespace {
// The engine reads the .cact in place, so the mapping must outlive the process-global model.
void* g_map = nullptr;
size_t g_map_len = 0;

std::string str(JNIEnv* env, jstring s) {
    if (!s) return std::string();
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out(c);
    env->ReleaseStringUTFChars(s, c);
    return out;
}
}  // namespace

extern "C" {

JNIEXPORT jint JNICALL
Java_com_mosrvs_pocketpilot_NeedleNative_load(JNIEnv* env, jobject, jstring path) {
    std::string p = str(env, path);
    int fd = open(p.c_str(), O_RDONLY);
    if (fd < 0) return -1000;
    struct stat st;
    if (fstat(fd, &st) != 0 || st.st_size <= 0) { close(fd); return -1001; }
    void* map = mmap(nullptr, (size_t)st.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);
    if (map == MAP_FAILED) return -1002;
    int rc = needle_load((const unsigned char*)map, (unsigned long long)st.st_size);
    if (rc < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "needle_load failed: %s", needle_last_error());
        munmap(map, (size_t)st.st_size);
        return rc;
    }
    g_map = map;
    g_map_len = (size_t)st.st_size;
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_mosrvs_pocketpilot_NeedleNative_init(JNIEnv* env, jobject, jstring system, jstring tools) {
    std::string s = str(env, system), t = str(env, tools);
    return needle_init(s.empty() ? nullptr : s.c_str(), t.c_str(), nullptr);
}

JNIEXPORT jstring JNICALL
Java_com_mosrvs_pocketpilot_NeedleNative_complete(JNIEnv* env, jobject, jstring input, jint maxNew) {
    std::string in = str(env, input);
    std::vector<char> out(16384, 0);
    int rc = needle_complete(in.c_str(), nullptr, 0, maxNew, out.data(), (int)out.size());
    if (rc < 0) return nullptr;
    return env->NewStringUTF(out.data());
}

JNIEXPORT void JNICALL
Java_com_mosrvs_pocketpilot_NeedleNative_reset(JNIEnv*, jobject) { needle_reset(); }

JNIEXPORT jstring JNICALL
Java_com_mosrvs_pocketpilot_NeedleNative_lastError(JNIEnv* env, jobject) {
    const char* e = needle_last_error();
    return env->NewStringUTF(e ? e : "");
}

}  // extern "C"
