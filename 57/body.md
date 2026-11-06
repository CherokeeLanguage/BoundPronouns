## Observed failure (executed output, 2026-11-05)

`./gradlew desktop:dist android:assembleRelease` fails at settings/build evaluation:

```
A problem occurred evaluating root project 'BoundPronouns'.
> Failed to apply plugin 'com.android.internal.version-check'.
   > Minimum supported Gradle version is 7.5. Current version is 7.4.2.
     If using the gradle wrapper, try editing the distributionUrl to
     gradle-7.5-all.zip
```

Additional environment facts:

- Installed JDKs (21, 25) are unsupported by the wrapper's Gradle 7.4.2
  (`Unsupported class file major version 65` / `69`).
- Machine-provisioned JDK 17 (`./.tools/jdk`, gitignored, `JAVA_HOME`)
  gets Gradle past the Java gate; the failure above is purely the
  Gradle-vs-AGP floor.

## Remedy

Bump `gradle/wrapper/gradle-wrapper.properties` `distributionUrl` to
`gradle-7.5.x` (or later) — verify `desktop:dist` and
`android:assembleRelease` still run on JDK 17 — and (optionally) document
the vendored JDK at `.tools/jdk/` + `JAVA_HOME` as the supported way to run
the build on machines whose system JDKs are unsupported.

## Context

Discovered while executing BoundPronouns#55's SC-1 (itch.io push run); the
push itself completed using the tree's current 3.42 release artifacts. The
remedy is a Gradle build-file change, out of #55's declared scope.
