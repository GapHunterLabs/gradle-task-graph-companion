package dev.gaphunter.gradletaskgraphcompanion.parse

/**
 * Extracts declared module paths (`:module`, `:group:module`, ...) from
 * the text of a `settings.gradle`/`settings.gradle.kts` file. Same
 * "hand-rolled scanner over a small, stable, line-oriented syntax"
 * pattern used catalog-wide (`CONSTITUTION.md` §6) -- copied verbatim
 * from `circular-dependency-companion`'s parser of the same name (each
 * plugin in this catalog is an independent Gradle project, no
 * cross-repo sharing).
 */
object GradleSettingsParser {

    private val INCLUDE_LINE = Regex("""\binclude\s*\(?\s*((?:['"][^'"]+['"]\s*,?\s*)+)\)?""")
    private val QUOTED_MODULE = Regex("""['"]([^'"]+)['"]""")

    fun parseIncludedModules(text: String): List<String> {
        val modules = mutableListOf<String>()
        for (lineMatch in INCLUDE_LINE.findAll(text)) {
            val argsText = lineMatch.groupValues[1]
            for (quoted in QUOTED_MODULE.findAll(argsText)) {
                modules.add(quoted.groupValues[1])
            }
        }
        return modules
    }
}
