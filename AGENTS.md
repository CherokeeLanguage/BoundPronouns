# AGENTS.md — BoundPronouns

Instructions for AI agents working in this repo. Keep this short; verify claims before trusting them.

## What this is

A Cherokee bound-pronouns flashcard trainer (libGDX). Multi-module Gradle:
`core` (shared logic/assets) + `desktop` (fat jar), `android` (APK), `ios` (RoboVM, rarely touched).
Desktop entrypoint: `com.cherokeelessons.bp.desktop.DesktopLauncher`.
`bin/` (audio build tooling), `skin-editor/`, `espeak-ng/`, `training-audio/` are auxiliary — not part of the app build.

## Build & run

- Desktop jar: `./gradlew desktop:dist` → `desktop/build/libs/BoundPronouns-<ver>.jar`
- Release APK: `./gradlew android:assembleRelease` → `android/build/outputs/apk/release/BoundPronouns-<ver>-release.apk`
- Full release (bumps version, tags, auto-commits deck/audio): `./build-release.sh`
- `version` file holds the integer build number (e.g. `342`); display version is derived (`342` → `3.42`) by `build-release.sh`, which rewrites `build.gradle`, `android/AndroidManifest.xml`, `ios/robovm.properties`.
- There are no unit test suites; verification here means a successful project build.

## Toolchain gotchas (hard-earned — read before building)

- Wrapper is Gradle 7.4.2. It rejects JDKs newer than 17 ("Unsupported class file major version"). Use JDK 17 — a vendored copy lives at `.tools/jdk` (gitignored); run builds as
  `JAVA_HOME=$PWD/.tools/jdk ./gradlew ...`
  (stop stale daemons with `./gradlew --stop` if a previous `JAVA_HOME` is pinned).
- The Android Gradle plugin used requires Gradle ≥ 7.5, so the build currently fails at configure time with "Minimum supported Gradle version is 7.5" — tracked as issue #57 (remedy: bump `gradle/wrapper/gradle-wrapper.properties`). Don't "fix" this by running a different Gradle.
- Android signing values in `gradle.properties` (`RELEASE_STORE_PASSWORD` etc.) are deliberate placeholders; the real store is a properties file not kept in the repo.

## Publishing (itch.io)

- Publish is manual: `./itch-push.sh` pushes the newest jar and newest release APK via butler to `cherokee-language/cherokee-bound-pronouns` (channels `win-linux-mac` and `android`).
- Butler is vendored at `.tools/butler/butler` (gitignored); credentials come from a one-time `butler login`. Publish target lives in `itch.conf` — real values are local-only; never commit them.
- The push script builds nothing; artifacts must already exist.
- The itch store-page description text lives at `docs/itch-page-description.md` (paste below the cut line into itch's Edit-game → Description). Update it if app features change; don't re-derive copy elsewhere.

## Android project layout quirk

`android/` uses legacy sourceDirs (`AndroidManifest.xml`, `src`, `res`, `assets` directly under `android/`, not the `src/main/...` layout). Generated changelog goes to `android/assets/text/git-changelog.txt`; the master deck is `android/assets/deck.json` (auto-committed by `build-release.sh`).

## Related

- Public site (Zola) with the app listing page: sibling repo `../CherokeeLessons.com` (`content/apps/bound-pronouns/index.md`).
- Support/issue tracker: https://github.com/CherokeeLanguage/BoundPronouns/issues
- Agent workflow rules for this workspace live in `.opencode/floor.md` — see `./.opencode/AGENTS.md`.
