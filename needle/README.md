# Needle 3 runtime integration

This is the tool contract we intend to send to Needle 3. It is **not** an integrated model yet.

Official instructions: https://www.cactuscompute.com/blog/needle-supported-devices

Run on an Android ARM64 test environment or desktop host using the upstream CLI and downloaded `needle3.cact` model:

```bash
./needle --model needle3.cact --tools tools.json --prompt "Find videos larger than 500 MB, largest first"
```

For the Android production build, statically link the upstream Android `libneedle.a` through an NDK/JNI bridge using `needle.h` and load `needle3.cact` from app-private storage. Do not ship a model, engine, or wrapper unless its provenance, redistribution rights, and dependencies have been reviewed.

The app must parse `function_calls`, validate all arguments against a separate allowlist, apply a confidence gate, and require explicit user approval for file changes. Media access uses Android Photo Picker / SAF. No original file is deleted by compression.

**Privacy verification:** the upstream CLI reportedly enables telemetry by default. For CLI experiments set `NEEDLE_TELEMETRY=0` and `DO_NOT_TRACK=1`. Confirm native Android integration does not transmit data before advertising fully offline operation.
