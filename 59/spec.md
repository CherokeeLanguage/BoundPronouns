# Spec — Add an itch.io store-page description (desktop Java requirement included)

BoundPronouns#59 · local-authoritative

## Provenance

- **Developer statements (2026-11-05, chat):** "create a spec to add an appropriate itch.io store description, including java requirements to run the app, etc." — one description for the itch.io page; the description must tell desktop players how to run the app (Java requirement); "etc." covers whatever else the page's empty description block needs to function as a store page.
- **Observed repo facts (verified 2026-11-06):** the itch.io page (https://cherokee-language.itch.io/cherokee-bound-pronouns, live with channels `win-linux-mac` and `android` per #55's push) carries **no description text** — only auto-derived metadata (genre "Educational", platform list, download links). The repo's only user-facing copy is README.md's single sentence ("Source for the Cherokee Bound Pronouns training flashcard app. Includes synthetic audio for each conjugated challenge.") and the launcher label `Bound Pronouns` (`android/res/values/strings.xml`). `docs/` holds pedagogy notes (SM2/Leitner spaced repetition, an app assessment), nothing authored as marketing copy.
- **Observed app facts (verified 2026-11-06, from the source tree):** flashcard challenge trainer for Cherokee bound pronouns; audio per conjugated challenge; study modes — syllabary-only, Latin-only, or both (docs/TODO.txt); session-length options — short (5 min), standard (10 min), long (15 min); per-card initial time limits — infinite, short (10 s), standard (15 s), long (30 s), x-long (60 s); multiple named study sessions (create, rename, delete). Desktop artifact is a fat jar (`BoundPronouns-*.jar`), compiled to Java 8 compatibility (`build.gradle` source/targetCompatibility 1.8) — the player needs a Java runtime, 8 or newer. Android artifact is a signed APK (min SDK 9 → Android 2.3+; installs from itch directly). Screenshots exist in `artwork/screen-shots/` but page design is out of scope.
- **butler/manual facts (itch.io docs, fetched 2026-11-05 during #55):** the page's description field is free markdown, set from the itch website's Edit-game page; the desktop channels' files are plain jars, so the description is where the Java requirement must reach the player.

## Problem

The itch.io page is live but its description block is empty: a visitor sees no statement of what the app is, what it does, what platforms it runs on, or what the desktop download requires to function. Without a description the page cannot convert a visitor into a player, and the desktop jar is not runnable without the unexplained Java prerequisite.

## Scope

One repository (BoundPronouns): one tracked markdown file carrying the store description text, plus guidance so the developer can set it on the itch website's Edit-game page. The deliverable is text; setting the itch page itself is a website action the developer performs.

## Requirements

- **R-1 — Tracked description asset.** A tracked markdown file (proposed: `docs/itch-page-description.md`) carries the store description text, self-contained and readable as markdown on the itch page renderer.
- **R-2 — Content floor.** The description covers:
  1. What the app is — a Cherokee bound-pronouns flashcard challenge trainer, in at most one opening paragraph;
  2. What players do — the study modes and options (syllabary / Latin / both; session lengths 5/10/15 min; per-card time limits; multiple named study sessions; audio with every conjugated challenge), as a short feature list the visitor can scan;
  3. Platform statements — Windows, macOS, Linux, and Android, one line;
  4. **The desktop run requirement** — the desktop download is a plain Java jar and needs a Java runtime (8 or newer) installed; this must be a visibly separate, unmissable line (not buried in prose), because without it a downloaded jar may appear broken to players without Java;
  5. The Android statement — itch delivers a standard APK the player installs directly (players may need to allow installs of unknown apps, one clause);
  6. Where the app is also available — Google Play, Apple App Store, cherokeelessons.com (one line, matching README's stated availability).
- **R-3 — Voice and length.** The text stays in the app's existing plain register (no invented claims, no marketing superlatives, no pricing statements — pricing is set on the page as name-your-own-price and stays owned by the itch website settings); every factual claim **about the app** is one the repo records (the observed app facts above, README's availability line); generic platform-side facts (the Android-side install flow, what a jar is) may be stated without repo provenance. Length: compact — a page-length block (roughly 150–350 words), not an essay.
- **R-4 — Setting guidance.** A short section (in the tracked file or its README pointer) states how to apply it: paste the description into the itch page's Edit-game → description field. The repo's README notes the file's existence and purpose in one line.
- **R-5 — No secrets, no dead links.** No credentials, tokens, or placeholder values in the text; links in the text must be the repo's documented live URLs (cherokeelessons.com, the itch page itself is optional) — nothing invented.

## Out of scope

- Setting the itch.io website's fields (description paste, genre, tags, classification "games vs assets", pricing) — all developer-side website actions; this spec delivers the text.
- Screenshots, cover art, page styling, trailer (page design artifacts; separate from the description field).
- Descriptions for other stores (Play, App Store) — surfaces with their own character limits and audiences; a separate spec if wanted.
- Any change to the app, build files, or `itch-push.sh` — the push script is untouched (butler pushes no description).

## Approach

One new tracked file (`docs/itch-page-description.md`), written from the observed app facts, plus a one-line README pointer. The file is the description verbatim (what the developer pastes), with application guidance kept in a clearly separated section at the top or bottom of the file, or in the README pointer's line — implementation detail at plan time.

## Success criteria

| ID | Criterion | Class | Instrument |
|----|-----------|-------|------------|
| SC-1 | A tracked markdown file exists carrying the store description; renderable as-is (no repo-internal jargon, no unfinished placeholders) | structural | file present, tracked by git; text read by reviewer |
| SC-2 | The description covers all six content-floor items (identity paragraph, scannable feature list, platform line, the visibly separate desktop Java 8+ requirement line, Android install clause, availability line) and no pricing/superlative claims | content | text read against the R-2 list, item by item |
| SC-3 | Every factual claim in the description traces to a repo-recorded fact (the observed app facts / README); no invented claims | content | each claim checked against R-2's fact list and README |
| SC-4 | The text is compact (≈150–350 words of description body) and states how to apply it (paste into the itch Edit-game description field); README notes the file in one line | content | word count + walkthrough line read |
| SC-5 | No credentials, tokens, placeholders, or dead links in the text | structural | grep + link check over the file |

## Impact

New file `docs/itch-page-description.md` plus one README line. Risk: low — no build, no runtime surface; the only externally visible effect is the developer pasting the text onto the itch page, which stays a developer action under their control.

---

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)
