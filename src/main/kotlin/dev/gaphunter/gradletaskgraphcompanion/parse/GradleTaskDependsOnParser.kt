package dev.gaphunter.gradletaskgraphcompanion.parse

/** One `dependsOn` edge found in a single build file, task names exactly as written (not yet module-qualified). */
data class RawTaskEdge(val from: String, val to: String)

/** Every task declared/referenced in a build file, plus every `dependsOn` edge found, both exactly as written. */
data class RawTaskParseResult(val declaredTasks: Set<String>, val edges: List<RawTaskEdge>)

/**
 * Line-oriented scanner (hand-rolled, same class of technique as
 * `DockerfileParser`/`NginxLexer`) that extracts task declarations and
 * their `dependsOn` targets from the text of one
 * `build.gradle`/`build.gradle.kts` file. 100% static text analysis --
 * never a real Gradle evaluation/daemon, so it only sees *explicit*
 * `dependsOn` declarations written in the build script, never the many
 * implicit task orderings a plugin like `java`/`application` wires up
 * internally (`build` depending on `test`/`assemble`, etc.) -- a real,
 * documented v0.1 limitation, not a bug (same honesty as
 * `unindexed-query-hint`-style "static heuristic, not a real query
 * planner" scoping decisions elsewhere in this catalog).
 *
 * **Task declaration/reference forms recognized** (the common ones,
 * not literally every Gradle DSL spelling -- same "cover the common
 * subset, document what's not" pattern as
 * `GradleBuildFileParser.parseProjectDependencies`):
 * - Kotlin DSL: `tasks.register("x")`, `tasks.register<Type>("x")`,
 *   `tasks.named("x")`, `tasks.getByName("x")`, `tasks.create("x")`,
 *   `val x by tasks.registering`.
 * - Groovy DSL: `task x(...)`, `task x { ... }`.
 *
 * **`dependsOn` targets recognized:** quoted string arguments
 * (`dependsOn("a", "b")`, Groovy `dependsOn 'a'`) are always trusted.
 * The Groovy single-line map form (`task x(dependsOn: y)` /
 * `task x(dependsOn: [y, z])`) is trusted even with a bare identifier --
 * that DSL slot is unambiguously a task reference by construction
 * (`dependsOn: build` referencing the standard, usually-not-declared-
 * in-this-file `build` task is a very common real idiom, not a corner
 * case). A bare identifier passed to the general `dependsOn(...)` call
 * form is more ambiguous (could be any variable), so it's only kept if
 * it matches a task name already declared somewhere in the same file --
 * filters out `dependsOn` used incidentally on an unrelated object
 * without needing real symbol resolution, same "match a known name,
 * don't resolve a symbol" discipline as `SqlSignalNames`/`HttpSignalNames`.
 */
object GradleTaskDependsOnParser {

    private val TASK_REGISTER_STYLE = Regex(
        """\btasks\s*\.\s*(?:register|named|getByName|create)\s*(?:<[^>]*>)?\s*\(\s*['"]([^'"]+)['"]""",
    )
    private val TASK_KOTLIN_BY_REGISTERING = Regex("""\bval\s+([A-Za-z_][A-Za-z0-9_]*)\s+by\s+tasks\s*\.\s*registering\b""")
    private val TASK_GROOVY_DECL = Regex("""^\s*task\s+([A-Za-z_][A-Za-z0-9_]*)\s*[({]""")

    private val DEPENDS_ON_CALL = Regex("""\bdependsOn\s*\(?\s*([^)\n]*)""")
    private val GROOVY_DEPENDSON_MAP_ARG = Regex("""\bdependsOn\s*:\s*(\[[^\]]*\]|[^,)\n]+)""")
    private val QUOTED_ARG = Regex("""['"]([^'"]+)['"]""")
    private val BARE_IDENTIFIER = Regex("""^[A-Za-z_][A-Za-z0-9_]*$""")

    /** A `dependsOn` target as written -- [quoted] distinguishes a real string-literal task reference (always trusted) from a bare identifier (only trusted if it turns out to match a task declared in the same file). */
    private data class RawTarget(val name: String, val quoted: Boolean)

    fun parse(text: String): RawTaskParseResult {
        val declared = linkedSetOf<String>()
        val rawEdges = mutableListOf<Pair<String, RawTarget>>() // from, target

        var currentTask: String? = null
        var blockDepth = 0
        var taskOpenDepth: Int? = null

        for (line in text.lineSequence()) {
            TASK_GROOVY_DECL.find(line)?.let { m ->
                val name = m.groupValues[1]
                declared += name
                currentTask = name
                taskOpenDepth = blockDepth
                GROOVY_DEPENDSON_MAP_ARG.find(line)?.let { dm ->
                    for (target in mapArgTargets(dm.groupValues[1])) rawEdges += name to target
                }
            }
            TASK_REGISTER_STYLE.find(line)?.let { m ->
                currentTask = m.groupValues[1]
                declared += currentTask!!
                taskOpenDepth = blockDepth
            }
            TASK_KOTLIN_BY_REGISTERING.find(line)?.let { m ->
                currentTask = m.groupValues[1]
                declared += currentTask!!
                taskOpenDepth = blockDepth
            }

            currentTask?.let { task ->
                DEPENDS_ON_CALL.find(line)?.let { m ->
                    for (target in rawTargets(m.groupValues[1])) rawEdges += task to target
                }
            }

            for (ch in line) {
                when (ch) {
                    '{' -> blockDepth++
                    '}' -> {
                        blockDepth--
                        val openDepth = taskOpenDepth
                        if (openDepth != null && blockDepth <= openDepth) {
                            currentTask = null
                            taskOpenDepth = null
                        }
                    }
                }
            }
        }

        // Quoted targets are trusted unconditionally -- a string literal
        // handed to dependsOn(...) is, by construction, meant as a task
        // name. A bare identifier is only trusted if it matches a task
        // declared somewhere in this same file, so an unrelated
        // dependsOn(...) call on some other DSL object doesn't leak a
        // random variable name into the graph as a fake task.
        val edges = rawEdges
            .filter { (_, target) -> target.quoted || target.name in declared }
            .map { (from, target) -> RawTaskEdge(from, target.name) }
            .distinct()

        return RawTaskParseResult(declared, edges)
    }

    /** Quoted args win if present (`dependsOn("a", "b")`); otherwise falls back to bare identifiers (Kotlin `dependsOn(x)`) -- only trusted later if [x] matches a task declared in the same file. */
    private fun rawTargets(argsText: String): List<RawTarget> {
        val quoted = QUOTED_ARG.findAll(argsText).map { RawTarget(it.groupValues[1], quoted = true) }.toList()
        if (quoted.isNotEmpty()) return quoted
        return argsText
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().trimEnd(')') }
            .filter { it.isNotEmpty() && BARE_IDENTIFIER.matches(it) }
            .map { RawTarget(it, quoted = false) }
    }

    /**
     * Same extraction as [rawTargets], but every result is marked
     * trusted (`quoted = true`) regardless of whether it was actually
     * quoted -- the Groovy `dependsOn: ...` map-argument slot is
     * unambiguously a task reference by construction, so a bare
     * identifier there (`dependsOn: build`) doesn't need the same
     * "must match a declared task" guard a general `dependsOn(...)`
     * call needs.
     */
    private fun mapArgTargets(argsText: String): List<RawTarget> =
        rawTargets(argsText).map { it.copy(quoted = true) }
}
