package com.avito.instrumentation.internal.finalizer

import com.avito.android.runner.report.LegacyReport
import com.avito.instrumentation.internal.TestRunResult
import com.avito.instrumentation.internal.finalizer.InstrumentationTestActionFinalizer.FinalizeAction
import com.avito.instrumentation.internal.report.HasNotReportedTestsDeterminer

internal class SendAvitoReport(
    private val avitoReport: LegacyReport
) : FinalizeAction {

    override fun action(testRunResult: TestRunResult) {
        if (testRunResult.notReported is HasNotReportedTestsDeterminer.Result.HasNotReportedTests) {
            val lostTests = testRunResult.notReported.lostTests
            avitoReport.sendLostTests(lostTests)
        }
        avitoReport.finish()
    }
}