package com.avito.android.plugin.build_metrics.cache

import com.avito.test.gradle.TestResult
import com.avito.test.gradle.gradlew
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal abstract class HttpBuildCacheTestFixture {

    private lateinit var projectDir: File
    private lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setup(@TempDir tempDir: File) {
        this.projectDir = tempDir
        mockWebServer = MockWebServer()

        File(projectDir, "settings.gradle.kts").writeText(
            buildCacheBlock(mockWebServer.url("/").toString())
        )
        givenHttpBuildCache()
        setupProject(projectDir)
    }

    abstract fun setupProject(projectDir: File)

    private fun buildCacheBlock(url: String): String {
        return """
            buildCache {
                local {
                    isEnabled = false
                }
                remote<HttpBuildCache> {
                    setUrl("$url")
                    isEnabled = true
                    isPush = true
                    isAllowUntrustedServer = true
                }
            }
            """.trimIndent()
    }

    @AfterEach
    fun cleanup() {
        mockWebServer.shutdown()
    }

    // Overriding content is not supported yet, only statuses.
    // Load should return task specific outputs as zip entry in a vendor specific format.
    // See content of a cache entry for details.
    protected fun givenHttpBuildCache(
        loadHttpStatus: Int = 404,
        storeHttpStatus: Int = 200
    ) {
        mockWebServer.dispatcher = object : Dispatcher() {

            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.method) {
                    "GET" -> MockResponse().setResponseCode(loadHttpStatus)
                    "PUT" -> MockResponse().setResponseCode(storeHttpStatus)
                    else -> throw IllegalStateException("Unmocked method: ${request.method}")
                }
            }
        }
    }

    protected fun build(vararg tasks: String): TestResult {
        return gradlew(
            projectDir,
            *tasks,
            "-Pavito.build.metrics.enabled=true",
            "-Pavito.stats.enabled=false",
            "--build-cache",
            "--debug", // to read statsd logs from stdout
        )
    }
}
