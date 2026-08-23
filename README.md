# Gradle Task Graph Companion

Tool window that visualizes the real `dependsOn` graph between Gradle
tasks in your project — root project plus every submodule declared in
`settings.gradle(.kts)` — grouped into dependency layers, with any real
cycle called out in its own top section, in red.

## Why it exists

Reading a Gradle project's task wiring today means grepping across every
`build.gradle(.kts)` by hand, or running `gradle :taskName:dependencies`
one task at a time from the terminal. Neither shows the whole picture at
once, and neither flags an accidental `dependsOn` cycle between custom
tasks (a real, silent footgun: Gradle only fails at execution time, with
a stack-trace-shaped error, not while you're editing the build script).

## Why built this way

- **100% static text analysis of your build files — never a real Gradle
  evaluation or daemon.** Same principle as `circular-dependency-companion`'s
  module graph: safe, fast, and it can't accidentally trigger a real
  build just to draw a picture of one.
- **Same layered-list rendering as `circular-dependency-companion`**,
  reused deliberately: a hand-drawn node-and-edge diagram is a real
  graph-layout algorithm on its own — meaningfully riskier scope than
  this catalog's proven `Tree`/`DefaultMutableTreeNode` pattern — while
  a layered list still shows every edge and makes cycles impossible to
  miss.
- **Manual refresh only.** Re-scanning every build file on each
  keystroke would be wasted work for a graph that only meaningfully
  changes when a `dependsOn` declaration is edited and saved.

## v0.1 scope — stated honestly, not exhaustively

- Only **explicit** `dependsOn` declarations written in your build
  scripts are shown. The many *implicit* task orderings a plugin like
  `java`/`application` wires up internally (`build` depending on
  `test`/`assemble`, etc.) are invisible without actually running
  Gradle — real, documented limitation, not a bug.
- Recognizes the common Kotlin DSL forms (`tasks.register("x") {
  dependsOn("y") }`, `tasks.named(...)`, `tasks.getByName(...)`,
  `tasks.create(...)`, `val x by tasks.registering`) and Groovy DSL
  forms (`task x(dependsOn: y)`, `task x(dependsOn: [y, z])`) — not
  literally every spelling Gradle's DSL allows.
- A bare (unquoted) task reference inside a general `dependsOn(...)`
  call is only trusted if it matches a task declared in the same file
  (avoids false positives from an unrelated `dependsOn` call on some
  other DSL object); the Groovy `dependsOn: y` map-argument form is
  always trusted, since that slot is unambiguously a task reference.

## Usage

Open the **Task Graph** tool window (bottom of the IDE). It scans on
open; use the refresh button in the toolbar after editing a build file.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
