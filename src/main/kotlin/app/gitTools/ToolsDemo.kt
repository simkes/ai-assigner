package app.gitTools
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val githubToken  = System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN environment variable is not set")
    val tools = GitHubToolSet(githubToken)

    val commits = tools.gitLog(
        owner = "kabachok-vpanike",
        repo  = "clusters",
        branch = "main",
        first  = 10
    )

    println(Json { prettyPrint = true }.encodeToString(Json.parseToJsonElement(commits)))

    val blame = tools.gitBlame(
        owner = "kabachok-vpanike",
        repo  = "clusters",
        path = "client/src/App.js",
        ref   = "main"
    )

    println(Json { prettyPrint = true }.encodeToString(Json.parseToJsonElement(blame)))
}
