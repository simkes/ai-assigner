package app.gitTools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@LLMDescription("Git utilities implemented *entirely* with GitHub's GraphQL API – commit history (git log) and file blame.")
class GitHubToolSet(
    private val githubToken: String
) : ToolSet {

    private val client = OkHttpClient()
    private val json   = Json { ignoreUnknownKeys = true }
    private val media  = "application/json".toMediaType()

    /* ──────────────────────────────────────────────────────────────── */
    /* git log via GraphQL                                             */
    /* ──────────────────────────────────────────────────────────────── */

    @Tool(customName = "git_log")
    @LLMDescription(
        "Return commit history for a branch (or tag/commit SHA) using GraphQL. " +
                "Parameters: owner, repo, branch (default 'main'), first (count), path (optional)."
    )
    suspend fun gitLog(
        owner: String,
        repo: String,
        branch: String? = "main",
        first: Int = 30,
        path: String? = null
    ): String = withContext(Dispatchers.IO) {

        val query = """
            query CommitHistory(${'$'}owner: String!, ${'$'}repo: String!, ${'$'}branch: String!, ${'$'}first: Int!, ${'$'}path: String) {
              repository(owner: ${'$'}owner, name: ${'$'}repo) {
                ref(qualifiedName: ${'$'}branch) {
                  target {
                    ... on Commit {
                      history(first: ${'$'}first, path: ${'$'}path) {
                        edges {
                          node {
                            oid
                            messageHeadline
                            committedDate
                            author { name email }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val payload = buildJsonObject {
            put("query", query)
            put("variables", buildJsonObject {
                put("owner", owner)
                put("repo", repo)
                put("branch", branch ?: "main")
                put("first", first)
                path?.let { put("path", it) }
            })
        }

        val request = Request.Builder()
            .url("https://api.github.com/graphql")
            .addHeader("Authorization", "Bearer $githubToken")
            .post(payload.toString().toRequestBody(media))
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("git_log failed: HTTP ${resp.code}")
            resp.body?.string() ?: "{}"
        }
    }

    /* ──────────────────────────────────────────────────────────────── */
    /* git blame via GraphQL                                           */
    /* ──────────────────────────────────────────────────────────────── */

    @Tool(customName = "git_blame")
    @LLMDescription(
        "Return blame information for a file on GitHub. " +
                "Parameters: owner, repo, path, ref (branch / commit, default 'main')."
    )
    suspend fun gitBlame(
        @LLMDescription("Repository owner (e.g. 'torvalds')") owner: String,
        @LLMDescription("Repository name (e.g. 'linux')") repo: String,
        @LLMDescription("Path to file (e.g. 'README.md')") path: String,
        @LLMDescription("Branch or commit reference (default 'main')") ref: String? = "main"
    ): String = withContext(Dispatchers.IO) {
        val effectiveRef = ref ?: "main"
        val query = """
      query Blame(${'$'}owner:String!, ${'$'}repo:String!, ${'$'}ref:String!, ${'$'}path:String!) {
        repository(owner:${'$'}owner, name:${'$'}repo) {
          object(expression:${'$'}ref) {
            ... on Commit {
              blame(path:${'$'}path) {
                ranges {
                  startingLine
                  endingLine
                  commit {
                    oid
                    committedDate
                    messageHeadline
                    author { name email }
                  }
                }
              }
            }
          }
        }
      }
    """.trimIndent()
        val variables = buildJsonObject {
            put("owner", owner)
            put("repo", repo)
            put("ref", effectiveRef)
            put("path", path)
        }
        val payload = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }
        val body = payload.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.github.com/graphql")
            .addHeader("Authorization", "Bearer $githubToken")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("git_blame failed: HTTP ${response.code}")
            response.body?.string() ?: "{}"
        }
    }
}
