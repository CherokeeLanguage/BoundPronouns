# Spec — Publish desktop and Android builds to itch.io via manual butler script

BoundPronouns#55 · local-authoritative

## Provenance

- **Developer statements (2026-11-05, chat):** "we need to set this up for publishing via itch.io"; answers to the clarification set: "(1) both deskop and apk/aab; unless users can use IPA, that's all that needs to get pushed.; (2) manual via script; (3) have an initial starter project page setup; (4) file a follow-up spec for versioning setup." A later developer instruction: "no fucking question tool" (channel discipline — no `question`-tool use).
- **Observed repo facts (verified 2026-11-05):** `./gradlew desktop:dist` (fat jar, `archivesBaseName = 'BoundPronouns'`, all runtime deps + libGDX desktop natives aggregated via `configurations.runtimeClasspath`) produced `desktop/build/libs/BoundPronouns-3.42.jar` in the existing build tree. `./gradlew android:assembleRelease` produced `android/build/outputs/apk/release/BoundPronouns-3.42-release.apk`, signed via `signingConfigs.release` in `android/build.gradle`. No AAB/bundle task exists anywhere in the build files. `version` file holds the integer release number (currently `342`); `build-release.sh` derives the `x.y` display version (`342` → `3.42`) for `build.gradle`, `android/AndroidManifest.xml`, and tags. Butler is not installed on this machine (`which butler` → missing).
- **Developer statements (2026-11-05, chat, second round):** "#55 approved for PR; I presume butler will be in `<repo-root>/.tools/` and the agent will have instructions to auto-source it if it is missing" — butler vendored at `.tools/butler/` (gitignored, same class as the playwright-cli card's vendored `.tools/node/`; the floor's vendored-tooling clause sanctions non-build CLI tooling), and the push script auto-sources it when butler is absent from `PATH`.
- **butler reference (itch.io butler manual — "Pushing builds", fetched 2026-11-05):** `butler push <file-or-directory> <user/game:channel>`; channel names containing `win`/`linux`/`mac`/`osx` get the corresponding platform tags, `android` gets tagged as an Android application; multi-platform channel naming is the documented convention for Java games (example given: `win-linux-mac-stable`); first push to a channel makes the file appear on the game's page, re-push updates it; uploads are idempotent patch pushes; `--userversion` is optional (version wiring handled by the sibling spec #56); 30 GB uncompressed backend cap.

## Problem

The app's build outputs — the desktop fat jar and the signed Android release APK — have no itch.io delivery path. The developer maintains a starter itch.io project page and wants builds pushed manually from a script at publish time, with no CI involvement.

## Scope

One repository (BoundPronouns): one new push script + one tracked target config + one vendored-butler download location (gitignored `.tools/butler/`, populated by a tracked vendoring script or documented command) + a README walkthrough. The script pushes already-built artifacts; it does not invoke the build itself.

## Requirements

- **R-1 — Channels and artifacts.** `itch-push.sh` (repo root) pushes:
  - the newest desktop fat jar in `desktop/build/libs/` matching `BoundPronouns-*.jar` (newest = last by file modification time, ties impossible in practice) → `"$ITCH_USER/$ITCH_GAME:win-linux-mac"`,
  - the newest APK in `android/build/outputs/apk/release/` matching `*-release.apk` (same modification-time rule) → `"$ITCH_USER/$ITCH_GAME:android"`.

  Channel names follow the butler manual's tag conventions (multi-platform channel for a Java game; `android` tagged as an Android application).
- **R-2 — Target config, no secrets.** The itch target is read from tracked `itch.conf` (`ITCH_USER`, `ITCH_GAME`) carrying placeholder values (`REPLACE_ME`) when filed. The script refuses to push while any placeholder stands. The config carries only non-secret target fields; butler's credentials live exclusively in butler's own credential store, written by a one-time developer-run `butler login`. No API key, token, or secret may enter any tracked file.
- **R-3 — Guided failure before any upload.** The script exits non-zero with a message naming the missing piece when (a) butler is on neither `PATH` nor at `.tools/butler/butler` (the script prefers `PATH`, falls back to the vendored copy), (b) `itch.conf` is unfilled, or (c) no matching artifact exists — in every case before any butler invocation. The no-artifact message names the build commands that produce the artifacts (`./gradlew desktop:dist`, `./gradlew android:assembleRelease`); the no-butler message names the vendoring command that populates `.tools/butler/` (download from `broth.itch.ovh/butler/linux-amd64/LATEST/unpack` into `.tools/butler/`) alongside the `butler login` pointer.
- **R-4 — Documented publish walkthrough.** `itch-push.sh` states its prerequisites inline in header comments; README gains a manual-publish walkthrough: build (existing gradle tasks or `build-release.sh`) → configure `itch.conf` → `butler login` once → run `./itch-push.sh`. The walkthrough states the player-side requirement of the desktop channel: a Java runtime (8+) on the player's machine, since the desktop channel ships a jar.

## Out of scope

- itch.io page creation, design, or promo assets (starter page exists; channel platform tags remain adjustable on the itch website's Edit-game page).
- AAB uploads — no bundle task exists, and AAB is a Play-exclusive format players cannot install from itch.
- iOS/IPA distribution — itch.io cannot deliver iOS app installs.
- CI automation (developer chose manual publishing).
- Version wiring (`--userversion`) — owned by the sibling spec #56; neither spec's scope includes the other's deliverables.
- Changes to `build-release.sh` or any Gradle build file.

## Approach

`itch-push.sh` mirrors the existing script style (`set -euo pipefail`, cd to script dir, guarded exit codes). Sequence:

1. Source `itch.conf`; fail fast on `REPLACE_ME` placeholders.
2. Resolve butler: `PATH` first, then `.tools/butler/butler`; fail with the vendoring + `login` pointers when neither exists.
3. Locate the newest matching jar and APK by glob; fail with the build commands when either is missing.
4. `butler push` each artifact to its channel, echoing the command and receipt.
5. Report `butler status` for both channels so the operator sees the landed builds.

`itch.conf` is plain `KEY=VALUE` shell-sourced; tracked with placeholders; real values the developer fills locally.

## Success criteria

| ID | Criterion | Class | Instrument |
|----|-----------|-------|------------|
| SC-1 | With butler authenticated and `itch.conf` filled, executing `./itch-push.sh` transfers both current artifacts to their channels and both channels report the push | behavioral | executed push log showing both `butler push` receipts + `butler status` on `…:win-linux-mac` and `…:android` reporting the new build (developer-assisted run; butler login is developer-side) |
| SC-2 | Butler on neither `PATH` nor `.tools/butler/`, or placeholder `itch.conf` values, or missing artifacts — each fault exits non-zero naming the missing piece (and, for the no-butler fault, the vendoring command), before any butler invocation | behavioral | three executed runs, one per fault branch, each showing the exit code and guidance message; each run requires its other prerequisites in place (the artifact-absence run needs butler present), disclosed per run |
| SC-3 | No itch credentials, API keys, or tokens in any tracked file; `itch.conf` carries only non-secret target fields | structural | grep over tracked files for itch/butler token patterns, expecting zero hits |
| SC-4 | README documents the manual publish walkthrough (build → configure → push), the `butler login` prerequisite, and the player-side Java requirement for the desktop channel | content | README section read |
| SC-5 | `itch-push.sh`'s header comments state its prerequisites (butler installed + `butler login` once + `itch.conf` filled with the real target) | content | script header comments read |

## Impact

New files only (`itch-push.sh`, `itch.conf`) plus a README section. First push to each channel makes the download appear on the game's page; re-pushes update it (butler manual). Risk: low — pushes are idempotent butler patching; no build, CI, or existing-script surface is touched. Prerequisite: butler installed (via `PATH`, or vendored at `.tools/butler/`) + `butler login` once on the developer machine.

---

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)
