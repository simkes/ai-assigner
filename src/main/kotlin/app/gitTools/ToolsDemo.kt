package app.gitTools
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val githubToken  = System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN environment variable is not set")
    val tools = GitHubToolSet(githubToken)

    val commits = tools.gitLog(
        path = "components/AnimatedImage/demo/src/jvmMain/kotlin/org/jetbrains/compose/animatedimage/demo/Main.kt",
    )

    println(Json { prettyPrint = true }.encodeToString(Json.parseToJsonElement(commits)))

    val blame = tools.gitBlame(
        path = "components/AnimatedImage/demo/src/jvmMain/kotlin/org/jetbrains/compose/animatedimage/demo/Main.kt",
    )

    println(Json { prettyPrint = true }.encodeToString(Json.parseToJsonElement(blame)))
}
