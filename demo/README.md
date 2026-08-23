# Demo data for screenshots

A realistic small multi-service Gradle project ("acmecorp-platform" —
not a real company, just a stand-in): an `api-gateway` and a
`billing-service` module, plus root-level aggregation tasks. It
deliberately includes one real accidental cycle between the two
services' custom tasks, so the screenshot shows off the cycle-detection
feature, not just an empty-cycles state.

## How to get the screenshot

1. Launch the plugin sandbox from the `gradle-task-graph-companion`
   folder: `./gradlew runIde`
2. In the sandbox IDE, open this `demo/` folder as the project.
3. Enter Full Screen (`View > Appearance > Enter Full Screen`, or search
   "Enter Full Screen" via Find Action).
4. Open the **Task Graph** tool window (bottom of the IDE). You should
   see a red "Cycles detected (1)" section listing
   `:api-gateway:generateClientStubs -> :billing-service:publishContract -> :api-gateway:generateClientStubs`,
   plus the full layered task list below it.
5. Take the screenshot (`Win+Shift+S` or your usual tool) with the tool
   window and its cycle section visible, and save it directly into
   `gradle-task-graph-companion/docs/screenshots/`.
6. Close the sandbox window when done.
