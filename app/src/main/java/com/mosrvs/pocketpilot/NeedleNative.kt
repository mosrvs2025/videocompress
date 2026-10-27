package com.mosrvs.pocketpilot

/** JNI surface over the upstream Cactus Needle 3 C API (libneedle.a, Apache-2.0). */
object NeedleNative {
    val libraryLoaded: Boolean = try {
        System.loadLibrary("needle_jni"); true
    } catch (e: UnsatisfiedLinkError) { false }

    external fun load(path: String): Int
    external fun init(system: String, toolsJson: String): Int
    external fun complete(input: String, maxNewTokens: Int): String?
    external fun reset()
    external fun lastError(): String
}
