package com.avito.tech_budget.warnings

import com.avito.android.utils.FAKE_OWNERSHIP_EXTENSION
import com.avito.tech_budget.utils.dumpInfoExtension
import com.avito.tech_budget.utils.failureResponse
import com.avito.tech_budget.utils.successResponse
import com.avito.tech_budget.warnings.CollectWarningsTest.Companion.WARNING_CONTENT
import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.dir
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.kotlinClass
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.plugin.plugins
import com.avito.test.http.Mock
import com.avito.test.http.MockDispatcher
import com.avito.test.http.MockWebServerFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class UploadWarningsTest {

    private val mockDispatcher = MockDispatcher(unmockedResponse = successResponse())
    private val mockWebServer = MockWebServerFactory.create()
        .apply {
            dispatcher = mockDispatcher
        }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `compile without warnings - logs information about empty warnings`(@TempDir projectDir: File) {
        generateProject(projectDir, containsWarnings = false)
        uploadWarnings(projectDir).assertThat()
            .buildSuccessful()
            .outputContains("No warnings found")
    }

    @Test
    fun `compile with warnings - plugin disabled - no warnings uploaded`(@TempDir projectDir: File) {
        generateProject(projectDir, containsWarnings = true)

        uploadWarnings(projectDir, collectWarningsEnabled = false, expectFailure = true)
            .assertThat()
            .buildFailed()
    }

    @Test
    fun `compile with warnings - uploadWarnings success`(@TempDir projectDir: File) {
        generateProject(projectDir, containsWarnings = true)

        uploadWarnings(projectDir)
            .assertThat()
            .buildSuccessful()
            .outputContains("Found 2 warnings")
    }

    @Test
    fun `compile with warnings - set owners - owners sent to server`(@TempDir projectDir: File) {
        generateProject(projectDir, containsWarnings = true)

        val request = mockDispatcher.captureRequest { path.contains("/dumpWarnings") }
        uploadWarnings(projectDir)
            .assertThat()
            .buildSuccessful()

        request.Checks().singleRequestCaptured().bodyContains(
            """
                "owners":["Speed","Messenger"]
            """.trimIndent()
        )
    }
    @Test
    fun `compile with 2 warnings - restrict batch size to 1 - two requests sent`(@TempDir projectDir: File) {
        generateProject(projectDir, containsWarnings = true, restrictBatchSize = true)

        val request = mockDispatcher.captureRequest { path.contains("/dumpWarnings") }
        uploadWarnings(projectDir)
            .assertThat()
            .buildSuccessful()

        request.Checks().requestsCaptured(requestsCount = 2)
    }

    @Test
    fun `compile with warnings - without ownership ext - empty owners sent to server`(@TempDir projectDir: File) {
        generateProject(projectDir, containsWarnings = true, includesOwners = false)

        val request = mockDispatcher.captureRequest { path.contains("/dumpWarnings") }
        uploadWarnings(projectDir)
            .assertThat()
            .buildSuccessful()

        request.Checks().singleRequestCaptured().bodyContains(
            """
                "owners":[]
            """.trimIndent()
        )
    }

    @Test
    fun `compile with warnings - upload error - build failure`(@TempDir projectDir: File) {
        mockDispatcher.registerMock(
            Mock(
                requestMatcher = { path.contains("/dumpWarnings") },
                response = failureResponse()
            )
        )
        generateProject(projectDir)

        uploadWarnings(projectDir, expectFailure = true)
            .assertThat()
            .buildFailed()
            .outputContains("Upload warnings request failed")
    }

    @Test
    fun `configure extension - warnings found`(@TempDir projectDir: File) {
        val separator = "***"
        val outputDirectoryName = "differentOutput"
        generateProject(projectDir, outputDirectoryName = outputDirectoryName, separator = separator)

        uploadWarnings(projectDir)
            .assertThat()
            .buildSuccessful()
            .outputContains("Found 2 warnings")
    }

    private fun uploadWarnings(
        projectDir: File,
        expectFailure: Boolean = false,
        collectWarningsEnabled: Boolean = true
    ) =
        gradlew(
            projectDir,
            "uploadWarnings",
            "-Pcom.avito.android.tech-budget.enable=$collectWarningsEnabled",
            expectFailure = expectFailure
        )

    private fun generateProject(
        projectDir: File,
        containsWarnings: Boolean = true,
        outputDirectoryName: String? = null,
        separator: String? = null,
        includesOwners: Boolean = true,
        restrictBatchSize: Boolean = false
    ) = TestProjectGenerator(
        plugins = plugins {
            id("com.avito.android.gradle-logger")
            id("com.avito.android.tech-budget")
            id("com.avito.android.code-ownership")
        },
        useKts = true,
        buildGradleExtra = """
                techBudget {
                    ${dumpInfoExtension(mockWebServer.url("/").toString())}
                    ${collectWarningsExtension(outputDirectoryName, separator, restrictBatchSize)}
                }
                ${if (includesOwners) FAKE_OWNERSHIP_EXTENSION else ""}
            """.trimIndent(),
        modules = listOf(
            AndroidAppModule(
                name = "app",
                plugins = plugins {
                    id("com.avito.android.tech-budget")
                    id("com.avito.android.code-ownership")
                },
                useKts = true,
                buildGradleExtra = if (includesOwners) FAKE_OWNERSHIP_EXTENSION else "",
                mutator = {
                    if (containsWarnings) {
                        dir("src/main/kotlin/") {
                            kotlinClass("DeprecatedClass") { WARNING_CONTENT }
                        }
                    }
                }
            )
        )
    ).generateIn(projectDir)

    private fun collectWarningsExtension(outputDirectoryName: String?, separator: String?, restrictBatchSize: Boolean) =
            """
                    collectWarnings {
                        ${if (!outputDirectoryName.isNullOrEmpty()) {
                            "outputDirectory.set(project.file(\"$outputDirectoryName\"))"
                        } else ""}
                        ${if (!separator.isNullOrEmpty()) "warningsSeparator.set(\"$separator\")" else ""}
                        ${if (restrictBatchSize) { """
                            uploadWarningsBatchSize.set(1)
                            uploadWarningsParallelRequestsCount.set(1)
                        """.trimIndent() } else ""}
                    }
            """.trimIndent()
}