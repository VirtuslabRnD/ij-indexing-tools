package com.virtuslab.indexing.listeners

import com.intellij.openapi.util.registry.Registry
import com.intellij.util.indexing.diagnostic.JsonSharedIndexDiagnosticEvent.Attached
import com.intellij.util.indexing.diagnostic.{
  ProjectDumbIndexingHistory,
  ProjectIndexingActivityHistoryListener,
  ProjectScanningHistory,
  SharedIndexDiagnostic
}
import com.virtuslab.indexing.data.IndexingStats
import com.virtuslab.indexing.data.IndexingStats.Durations
import com.virtuslab.indexing.publish.IndexingStatsLogPublisher
import it.unimi.dsi.fastutil.longs.LongCollection

import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters._

class IndexingStatsReporter extends ProjectIndexingActivityHistoryListener {
  private val scanningRecord = new TrieMap[Long, ProjectScanningHistory]()

  override def onFinishedScanning(history: ProjectScanningHistory): Unit = {
    scanningRecord += history.getTimes.getScanningId -> history
  }

  override def onFinishedDumbIndexing(history: ProjectDumbIndexingHistory): Unit = {
    val histories = scanningRecord.view.filterKeys { id =>
      history.getTimes.getScanningIds.asInstanceOf[LongCollection].contains(id)
    }
    val numberOfFilesCoveredBySharedIndexes = history.getProviderStatistics.asScala
      .map(_.getTotalNumberOfFilesFullyIndexedByExtensions).sum
    val numberOfIndexedFiles = history.getProviderStatistics.asScala
      .map(_.getTotalNumberOfIndexedFiles).sum
    val project = history.getProject
    val sharedIndexEvents = SharedIndexDiagnostic.INSTANCE.readEvents(project).asScala.toSeq

    val sharedIndexKindsUsed = sharedIndexEvents.collect { case e: Attached.Success =>
      e.getKind
    }

    val isIncremental = histories.values.exists { s =>
      s.getTimes.getScanningType.name().toLowerCase == "partial"
    }
    val indexingTimes = history.getTimes
    val scanningTimes = histories.values.map(_.getTimes)
    // TODO check if durations are interpreted correctly here
    val totalIndexing = indexingTimes.getTotalUpdatingTime / 1_000_000_000L
    val totalScanning = scanningTimes.map(_.getTotalUpdatingTime).sum / 1_000_000_000L
    val totalPaused = indexingTimes.getPausedDuration.getSeconds + scanningTimes.map(_.getPausedDuration.getSeconds).sum
    val report: IndexingStats = IndexingStats(
      startedAt = indexingTimes.getUpdatingStart.toEpochSecond,
      finishedAt = indexingTimes.getUpdatingEnd.toEpochSecond,
      durations = Durations(
        totalIndexing = totalIndexing,
        totalScanning = totalScanning,
        scanningAndIndexingActual = totalIndexing + totalScanning,
        scanningAndIndexingWithoutPauses = totalIndexing + totalScanning - totalPaused
      ),
      isSharedIndexesEnabled = Registry.is("shared.indexes.download"),
      isIncremental = isIncremental,
      sharedIndexKindsUsed = sharedIndexKindsUsed,
      numberOfIndexedFiles = numberOfIndexedFiles,
      numberOfFilesCoveredBySharedIndexes = numberOfFilesCoveredBySharedIndexes
    )
    // TODO add an extension point
    IndexingStatsLogPublisher.publish(report)
  }

}
