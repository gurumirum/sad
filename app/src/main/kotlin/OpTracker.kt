package gurumirum.sad.app

import com.github.ajalt.mordant.animation.Animation
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private const val UI_UPDATE_RATE: Long = 5000

class OpTracker(entries: Set<String>) {
    private val entryToStage = entries.associateWith { State() }.toSortedMap()
    private val genericReports: MutableList<Report> = Collections.synchronizedList(mutableListOf())

    private var totalReports = AtomicInteger()

    private var dirtyStatus = true

    class Updater(
        private val widget: Animation<Unit>,
        private val updateJob: Job
    ) {
        fun stop() {
            updateJob.cancel()
            widget.update(Unit)
            widget.stop()
        }
    }

    suspend fun startUpdate(scope: CoroutineScope, terminal: Terminal): Updater {
        val tableWidget = terminal.animation<Unit> {
            table {
                cellBorders = Borders.NONE
                tableBorders = Borders.TOP_BOTTOM
                header {
                    row("Name", "Stage")
                    cellBorders = Borders.BOTTOM
                }
                body {
                    for ((k, v) in entryToStage) {
                        row(k, v.stage)
                    }
                }
            }
        }
        val job = scope.launch {
            while (true) {
                if (dirtyStatus) {
                    dirtyStatus = false
                    tableWidget.update(Unit)
                }
                delay(UI_UPDATE_RATE)
            }
        }
        return Updater(tableWidget, job)
    }

    fun printReports(terminal: Terminal) {
        val totalReports = totalReports.get()
        if (totalReports <= 0) return

        fun Report.print() = terminal.println(
            "  " + message.replace("\n", "  \n"),
            stderr = error
        )

        terminal.println("$totalReports total report(s)\n")
        for ((path, state) in entryToStage) {
            if (state.reports.isEmpty()) continue
            terminal.println("Entry $path: ${state.reports.size} report(s)")
            for (report in state.reports) report.print()
        }
        if (genericReports.isNotEmpty()) {
            terminal.println("${genericReports.size} generic report(s)")
            for (report in genericReports) report.print()
        }
    }

    fun updateStatus(entry: String, stage: Stage) {
        val state = entryToStage[entry] ?: throw IllegalArgumentException("Entry $entry does not exist")
        state.stage = stage
        dirtyStatus = true
    }

    fun addReport(entry: String, message: String, error: Boolean) {
        val state = entryToStage[entry] ?: throw IllegalArgumentException("Entry $entry does not exist")
        state.reports += Report(message, error)
        totalReports.addAndGet(1)
    }

    fun addGenericReport(message: String, error: Boolean) {
        genericReports += Report(message, error)
        totalReports.addAndGet(1)
    }

    class State(
        @Volatile
        var stage: Stage = Stage.PROCESSING
    ) {
        val reports: MutableList<Report> = Collections.synchronizedList(mutableListOf())
    }

    class Report(val message: String, val error: Boolean)

    enum class Stage {
        PROCESSING,
        PROCESSING_FAILED,
        COMPRESSING,
        COMPRESSING_FAILED,
        SAVING,
        SAVING_FAILED,
        SKIPPED,
        FINISHED
    }
}
