# Privacy Policy — Gradle Task Graph Companion

**Effective date:** 2026-08-23

Gradle Task Graph Companion is a Gap Hunter Labs plugin for IntelliJ
Platform IDEs. This policy is short because the plugin's design makes it
short: there is nothing to disclose beyond what's below.

## What this plugin collects

**Nothing.** Gradle Task Graph Companion does not collect, store,
transmit, or sell any data — no source code, no file contents, no file
paths, no usage analytics, no telemetry, no crash reports, no personally
identifiable information. Your project's `build.gradle(.kts)`/
`settings.gradle(.kts)` files are read only in memory for as long as the
IDE is open, and only long enough to compute the task dependency graph.

## Network access

**None.** Gradle Task Graph Companion makes zero network calls during
normal operation, and never launches a real Gradle build or daemon —
every graph shown is computed directly from build-file text already
present on your local disk.

## Third parties

None. Gradle Task Graph Companion has no third-party SDKs, no analytics
libraries, no ad networks, no external dependencies that phone home.

## Changes to this policy

If this ever changes, this file will be updated and the change will be
noted in the plugin's `CHANGELOG.md`.

## Contact

Questions about this policy: **gaphunterlabs@gmail.com**
