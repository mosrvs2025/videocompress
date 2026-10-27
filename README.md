# PocketPilot — offline-first Android media assistant

An experimental, privacy-first Android utility that combines storage insights, file selection, safe video compression, and on-device AI intent routing with Cactus Needle 3.

## First user experience
1. Try the official Needle 3 browser demo at https://www.cactuscompute.com/needle (this is a model demo, **not** PocketPilot).
2. Launch PocketPilot, grant access only to media you select.
3. Ask: "Show videos larger than 500 MB" or "Help me compress this video for sharing."
4. Review proposed operations and explicitly approve before compression or any file changes.

## Implementation direction
- Android: Kotlin + Jetpack Compose; Android Photo Picker and Storage Access Framework (avoid blanket storage access).
- Local model: Cactus Needle 3 Android ARM64 engine via JNI bridge after validating its upstream C API and bundled licenses.
- Actions: list selected videos, sort by size, suggest compression presets, compress a user-selected video, verify output. No automatic deletion.
- Voice: optionally integrate Whistle after the text workflow works.
- Monetization hypothesis: free manual compression and basic local AI actions; Pro batch compression, workflow presets, and queued jobs. Validate customer demand before subscriptions.

The `prototype/needle-media-assistant` branch contains work-in-progress architecture and tests. No APK or model runtime is bundled yet.

Upstream: https://github.com/cactus-compute/needle and https://huggingface.co/Cactus-Compute/needle3

## Status (v0.3.0 debug build)
**Verified (built/unit-tested in CI-like Linux env):** Gradle build succeeds; APK contains `lib/arm64-v8a/libneedle_jni.so` (links upstream Needle 3 `libneedle.a`, Apache-2.0) and `assets/needle3.cact` (fetched from Hugging Face at build time); 8 JVM unit tests pass (tool-call validation allowlist, rule router).

**NOT verified (no Android device was available):** native library loading and `needle_load/init/complete` on a phone; real Needle output shape (parser assumes `function_calls[{name,arguments}]` + `confidence`); Needle latency/RAM; Media3 compression on real video, output playability and size savings; airplane-mode and permission-denial tests. The rule-based router is used automatically if Needle fails to load.
