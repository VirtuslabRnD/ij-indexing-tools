package com.virtuslab.indexing.listeners

import com.intellij.openapi.util.registry.Registry
import com.intellij.util.indexing.diagnostic.{
  ProjectDumbIndexingHistory,
  ProjectIndexingActivityHistoryListener,
  ProjectScanningHistory,
  SharedIndexDiagnostic
}
import com.virtuslab.indexing.data.IndexingStats
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
    // FIXME read info about shared index kinds
    // val sharedIndexEvents = SharedIndexDiagnostic.readEvents(project)

    val isIncremental = histories.values.exists { s =>
      s.getTimes.getScanningType.name().toLowerCase == "partial"
    }
    val report: IndexingStats = IndexingStats(
      startedAt = history.getTimes.getUpdatingStart.toEpochSecond,
      finishedAt = history.getTimes.getUpdatingEnd.toEpochSecond,
      isSharedIndexesEnabled = Registry.is("shared.indexes.download"),
      isIncremental = isIncremental,
      sharedIndexKindsUsed = Seq.empty,
      numberOfIndexedFiles = numberOfIndexedFiles,
      numberOfFilesCoveredBySharedIndexes = numberOfFilesCoveredBySharedIndexes
    )
    // TODO add an extension point
    IndexingStatsLogPublisher.publish(report)
  }

}
