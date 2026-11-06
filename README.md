# BoundPronouns

Source for the Cherokee Bound Pronouns training flashcard app. Includes synthetic audio for each conjugated challenge.

The itch.io store-page description lives in [docs/itch-page-description.md](docs/itch-page-description.md) — paste it into the itch page's Edit-game → Description field.


Available via Google Play, Apple App Store, and direct download: (https://www.cherokeelessons.com/apps/bound-pronouns/)

## Publishing to itch.io (manual)

The desktop jar and the signed Android APK are pushed to itch.io manually from
`itch-push.sh` — the script only pushes already-built artifacts; it never
starts a build. One-time setup:

1. **Build** the artifacts with the existing gradle tasks
   (`./gradlew desktop:dist`, `./gradlew android:assembleRelease`) or the full
   `./build-release.sh`.
2. **Configure** `itch.conf` with the real itch.io target (`ITCH_USER`,
   `ITCH_GAME`). It ships with placeholder values; fill them in locally —
   never any credentials.
3. **Install butler** (once): it is either on your `PATH`, or vendored at
   `.tools/butler/butler` (gitignored). To vendor, run:
   ```bash
   curl -L https://broth.itch.zone/butler/linux-amd64/LATEST/archive/default -o butler.zip
   mkdir -p .tools/butler && unzip butler.zip -d .tools/butler && chmod +x .tools/butler/butler && rm butler.zip
   ```
4. **Authenticate** (once): run `butler login` — it opens a browser and stores
   credentials in butler's own credential store.

Then publish any time:

```bash
./itch-push.sh
```

The script finds the newest `BoundPronouns-*.jar` and newest
`*-release.apk`, pushes the jar to the `win-linux-mac` channel and the APK to
the `android` channel, and prints `butler status` receipts. Re-pushes are
idempotent patch pushes; the first push to a channel makes the download
appear on the game's page.

**Player-side note:** the desktop channel ships a plain jar — players need a
Java runtime (8+) installed on their machine (Android players are unaffected;
itch installs the APK directly).
