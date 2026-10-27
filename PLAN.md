# PocketPilot — one-week vertical slice

## First milestone
- [ ] New Kotlin Android project with a modern Compose interface
- [ ] Scoped Android Photo Picker, optionally multiple selections
- [ ] Show selected videos sorted by size and total bytes
- [ ] Implement native Needle 3 bridge and demonstrate tool-call JSON on the actual phone
- [ ] Validate tool calls and display confirm/cancel before any modification
- [ ] Add on-device video transcoding with progress, cancel, before/after size, playable outputs
- [ ] Keep originals; send deletion requests through OS consent flow only
- [ ] Offline airplane-mode test and instrument startup/latency/RAM/battery
- [ ] 3 real Samsung device tests; crash checks and permission denial tests

## Monetization hypothesis
Free: select videos, insights, basic single video compression. Pro: queue and batch compression, saved presets, advanced workflows. No paywall until native reliability is proven.

## Definition of done
Installed APK running on Android hardware, Needle inference confirmed locally in airplane mode, compression works on an actual selected video and saves to a new output location with measured bytes saved, no destructive operation without explicit confirmation.

## Risks to validate
NDK ABI and C API packaging, benchmark claims vs real phones, model/runtime licenses, Play policy on permissions, subscription economics, thermal throttling, and codec availability.
