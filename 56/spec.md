# Spec — Carry the release version on itch.io butler pushes (follow-up)

BoundPronouns#56 · local-authoritative

## Provenance

- **Developer statement (2026-11-05, chat):** "file a follow-up spec for versioning setup" — issued in the context of whether the itch push is wired into the repo's release versioning.
- **Observed repo facts (verified 2026-11-05):** `build-release.sh` is the version flow: it reads the integer from `version` (currently `342`), increments it, derives the `x.y` display version (`342 → 3.42`) with digit-split math, writes it into `build.gradle` (`version = '…'`), `android/AndroidManifest.xml` (`versionCode`/`versionName`), and `ios/robovm.properties`, writes the increment back to the `version` file, and tags the display version. `itch-push.sh` (deliverable of sibling spec #55) does not pass a user version today — the itch backend then assigns its own increasing integers, so the repo's release versions never surface on the itch channels.
- **butler reference (itch.io butler manual — "Pushing builds", fetched 2026-11-05):** `--userversion <str>` passes a user-supplied build version; user version strings carry no format requirement; `--userversion-file` reads it from a UTF-8 single-line file. Ordering itch.io uses for user versions is upload order.

## Problem

Initial itch.io pushes carry no user version, so the repo's release versions never surface on the itch channels. The developer asked for a follow-up spec wiring the itch pushes into the existing release-version flow.

## Scope

One repository (BoundPronouns): `itch-push.sh` derives the release version string and passes it to both butler pushes; README documents the version story. No change to `build-release.sh`'s bump logic, no CI changes. This spec sits on top of the #55 deliverable; it amends `itch-push.sh` only.

## Requirements

- **R-1 — Versioned pushes.** Both channel pushes carry `--userversion` equal to the current release display version derived from the `version` file using the same digit-split transform `build-release.sh` uses (`342 → "3.42"`).
- **R-2 — Single source of truth.** The version string comes only from the `version` file: the script contains no version literals, runs no bump, and regenerates nothing.
- **R-3 — Documented version story.** README documents: `build-release.sh` bumps + tags the release; `itch-push.sh` reads the `version` file and stamps both itch channels with that version.

## Out of scope

- Changes to `build-release.sh`'s version bump/tag logic or its trigger points.
- CI automation.
- Butlers' auto-versioning behavior beyond the `--userversion` pass-through.
- The #55 deliverable set itself (channel mechanics, config guardrails, README publish walkthrough) — owned by the sibling spec.

## Approach

`itch-push.sh` reads the integer from the `version` file and applies the same digit-split math `build-release.sh` applies, then passes `--userversion "$xversion"` on both channel pushes.

**Edge case (noted, not a requirement):** the `version` file and `build.gradle`'s `version = '…'` can drift apart when someone edits `build.gradle` directly outside `build-release.sh`'s run; the `version` file is the source (a `build-release.sh` run regenerates the display version everywhere), and the next release build reconciles.

## Success criteria

| ID | Criterion | Class | Instrument |
|----|-----------|-------|------------|
| SC-1 | After an executed `./itch-push.sh`, `butler status` for both channels reports the user version equal to the version-file-derived string | behavioral | executed push followed by `butler status <user/game:win-linux-mac>` and `butler status <user/game:android>` outputs (developer-assisted run; butler login is developer-side) |
| SC-2 | The script's only version source is the `version` file; no version literals; no bump logic | structural | grep/fact check of `itch-push.sh` |
| SC-3 | README documents the release → itch version flow | content | README section read |

## Impact

Touches `itch-push.sh` (issued by #55) and README. The itch channels' build listings carry the repo's release versions, keeping the itch page and the repo's tags/labels in lockstep. Risk: low — a read-only derivation plus an additional butler flag.

---

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)
