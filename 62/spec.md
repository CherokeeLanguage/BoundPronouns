# Spec — Add the itch.io publication info to the README (page URL + current availability)

BoundPronouns#62 · local-authoritative

## Provenance

- **Developer statements (2026-11-09, chat):** "the bound pronoun app needs remediation to include the needed itch publication information like URL and so forth"; "file additional specs as warranted."
- **Developer statement (2026-11-06, recorded in #59's provenance):** "the app is now only available via itch.io" — availability statements everywhere the text could imply other stores reflect itch.io as the app's sole current availability channel.
- **Observed repo facts (verified 2026-11-09):** README.md's availability line reads "Available via Google Play, Apple App Store, and direct download: (https://www.cherokeelessons.com/apps/bound-pronouns/)" — it predates the itch publication, names store channels the 2026-11-06 availability statement supersedes, and names no itch.io URL; the itch.io page URL appears nowhere in the README. `itch.conf.example` carries the real push target (`ITCH_USER=cherokee-language`, `ITCH_GAME=cherokee-bound-pronouns`); AGENTS.md's Publishing section carries the push target and the description pointer but not the landing URL; `docs/itch-page-description.md` is #59's description asset. The README's publishing walkthrough and player-side Java note are present and current (#55's deliverables).
- **Observed itch facts (verified 2026-11-09):** the page https://cherokee-language.itch.io/cherokee-bound-pronouns is live — "Cherokee Bound Pronouns by Cherokee Lessons", a downloadable app for Windows/macOS/Linux/Android, "Name your own price", carrying #59's description text.

## Problem

The app is published on itch.io, but the README — the repo's user-facing publication surface — does not say so: its availability line predates the itch publication, names store channels the developer has stated are no longer the app's availability (2026-11-06), and omits the itch.io page URL. A reader of the repo gets no pointer to where the app is actually available.

## Scope

One repository (BoundPronouns): README.md's availability/publication information. No change to the itch page itself, the push tooling, `itch.conf.example`, AGENTS.md, or the description asset.

## Requirements

- **R-1 — Itch URL present.** The README's availability/publication information states the app's itch.io publication and carries the published page URL `https://cherokee-language.itch.io/cherokee-bound-pronouns`.
- **R-2 — Availability matches the stated channel.** The README's availability statement reflects the developer's on-record availability statement (itch.io is the app's availability channel — 2026-11-06): it no longer lists Google Play or the Apple App Store as availability channels.

## Success criteria

| ID | Criterion | Class | Instrument |
|---|---|---|---|
| SC-1 | README.md carries the URL `https://cherokee-language.itch.io/cherokee-bound-pronouns` in its availability/publication information | structural | Read README.md; string match against the verified live page URL |
| SC-2 | README.md's availability statement names itch.io as the app's availability channel and lists neither Google Play nor the Apple App Store as availability channels | structural | Read README.md's availability statement |

## Approach

Rewrite README.md's availability line around the itch publication: the itch.io page URL, per the developer's availability statement. The site listing page pointer (https://www.cherokeelessons.com/apps/bound-pronouns/) stays — the page exists and is the app's public listing; its own link state is the sibling repo's concern (the site's phase-2 edits), not this spec's.

## Out of scope

- The itch.io page itself (description, pricing, metadata — #59's deliverable, live).
- The sibling site repo's app listing page (store links / phase-2 edits).
- AGENTS.md, `itch.conf.example`, `itch-push.sh`, `docs/` — current state verified accurate for their roles.
- Version wiring on pushes (#56, open sibling ticket).
- Historical changelog text (`android/assets/text/changelog.txt` — historical record, not an availability claim).

## Impact

One README edit; no code or tooling change; the README's publication story becomes truthful to the app's actual availability.
