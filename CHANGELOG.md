<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gradle Task Graph Companion Changelog

## [Unreleased]

## [0.1.1]

### Added

- Review/star CTA: after 5 explicit Refresh clicks in the tool window
  (never counted for the passive initial scan when the tool window
  first opens), a one-time notification asks whether to rate the
  plugin on Marketplace, with a permanent "Don't ask again" option.

## [0.1.0]

### Added

- "Task Graph" tool window: visualizes the `dependsOn` graph between
  Gradle tasks (root project + every submodule), grouped by dependency
  layer.
- Real cycle detection between tasks, called out in its own top
  section, in red.
- Recognizes both Kotlin DSL and Groovy DSL task-declaration/`dependsOn`
  forms.
- 100% static text analysis, no Gradle daemon or real build evaluation,
  no network calls, no telemetry. Free.

[Unreleased]: https://github.com/GapHunterLabs/gradle-task-graph-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/gradle-task-graph-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/gradle-task-graph-companion/commits/0.1.0
