package com.mosrvs.pocketpilot

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

/** Loads needle3.cact from app-private storage and runs inference on one worker thread (engine is not thread-safe). */
class NeedleEngine(private val context: Context) {
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile var ready = false; private set
    @Volatile var loadError: String? = null; private set
    @Volatile var initPrefixTokens = 0; private set

    fun start(onDone: (Boolean) -> Unit) = worker.execute {
        try {
            if (!NeedleNative.libraryLoaded) throw IllegalStateException("libneedle_jni.so missing for this ABI (arm64-v8a only)")
            val model = File(context.filesDir, "needle3.cact")
            if (!model.exists() || model.length() == 0L) {
                context.assets.open("needle3.cact").use { i -> model.outputStream().use { i.copyTo(it) } }
            }
            val rc = NeedleNative.load(model.absolutePath)
            if (rc < 0) throw IllegalStateException("needle_load=$rc ${NeedleNative.lastError()}")
            val tools = context.assets.open("tools.json").bufferedReader().readText()
            val n = NeedleNative.init(SYSTEM_PROMPT, tools)
            if (n < 0) throw IllegalStateException("needle_init=$n ${NeedleNative.lastError()}")
            initPrefixTokens = n
            ready = true
        } catch (t: Throwable) {
            loadError = t.message ?: t.javaClass.simpleName
        }
        onDone(ready)
    }

    /** Returns (result, latencyMs) or throws. Callback runs on the worker thread. */
    fun run(prompt: String, onDone: (Result<Pair<NeedleResult, Long>>) -> Unit) = worker.execute {
        onDone(runCatching {
            check(ready) { "Needle not loaded: $loadError" }
            val t0 = System.nanoTime()
            NeedleNative.reset()
            val raw = NeedleNative.complete(prompt, 192) ?: error("needle_complete failed: ${NeedleNative.lastError()}")
            ToolCallParser.parse(raw) to (System.nanoTime() - t0) / 1_000_000
        })
    }

    companion object {
        const val SYSTEM_PROMPT = "You control a private on-device video assistant. Users have picked videos with the system picker. Only call a tool when the request matches one."
    }
}
