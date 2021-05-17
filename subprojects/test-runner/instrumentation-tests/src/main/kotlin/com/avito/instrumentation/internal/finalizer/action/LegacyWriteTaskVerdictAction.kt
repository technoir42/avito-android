package com.avito.instrumentation.internal.finalizer.action

import com.avito.instrumentation.internal.finalizer.TestRunResult
import com.avito.instrumentation.internal.finalizer.verdict.InstrumentationTestsTaskVerdict
import com.avito.instrumentation.internal.finalizer.verdict.LegacyVerdict
import com.avito.report.ReportLinkGenerator
import com.google.gson.Gson
import java.io.File

internal class LegacyWriteTaskVerdictAction(
    private val verdictDestination: File,
    private val gson: Gson,
    private val reportLinkGenerator: ReportLinkGenerator
) : LegacyFinalizeAction {

    override fun action(
        testRunResult: TestRunResult,
        verdict: LegacyVerdict,
    ) {
        val reportViewerUrl = reportLinkGenerator.generateReportLink()
        verdictDestination.writeText(
            gson.toJson(
                InstrumentationTestsTaskVerdict(
                    title = verdict.message,
                    reportUrl = reportViewerUrl,
                    problemTests = verdict.getCauseFailureTests()
                )
            )
        )
    }

    private fun LegacyVerdict.getCauseFailureTests() =
        when (this) {
            is LegacyVerdict.Success -> emptySet()
            is LegacyVerdict.Failure -> {
                val failedTestsVerdict = prettifiedDetails.failedTests
                    .map { test -> test.toTaskVerdictTest("FAILED") }

                val lostTestsVerdict = prettifiedDetails.lostTests
                    .map { test -> test.toTaskVerdictTest("LOST") }
                (failedTestsVerdict + lostTestsVerdict).toSet()
            }
        }

    private fun LegacyVerdict.Failure.Details.Test.toTaskVerdictTest(
        prefix: String
    ): InstrumentationTestsTaskVerdict.Test = InstrumentationTestsTaskVerdict.Test(
        testUrl = reportLinkGenerator.generateTestLink(name),
        title = "$name ${devices.joinToString(separator = ",")} $prefix"
    )
}