package com.virtuslab.shared_indexes.telemetry

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.indexing.diagnostic.JsonSharedIndexDiagnosticEvent.Attached
import com.intellij.util.indexing.diagnostic.dto.JsonPercentages
import com.intellij.util.indexing.diagnostic.{ProjectDumbIndexingHistory, ProjectIndexingActivityHistoryListener, ProjectScanningHistory, SharedIndexDiagnostic}
import com.virtuslab.shared_indexes.telemetry.IndexingStats.Durations
import it.unimi.dsi.fastutil.longs.LongCollection

import scala.collection.MapView
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters._

class IndexingStatsReporter extends ProjectIndexingActivityHistoryListener {
  private implicit class IterableOps[A](iterable: Iterable[A]) {
    def sumBy[B](f: A => B)(implicit num: Numeric[B]): B = {
      iterable.map(f).sum
    }
  }

  private val scanningRecord = new TrieMap[Long, ProjectScanningHistory]()

  override def onFinishedScanning(history: ProjectScanningHistory): Unit = {
    scanningRecord += history.getTimes.getScanningId -> history
  }

  override def onFinishedDumbIndexing(history: ProjectDumbIndexingHistory): Unit = {
    reportIndexingStats(history)
  }

  private def reportIndexingStats(history: ProjectDumbIndexingHistory): Unit = {
    val stats = history.getProviderStatistics.asScala
    val project = history.getProject
    val scans = findScansForIndexing(history)

    val numberOfScannedFiles = scans.values.sumBy(_.getScanningStatistics.asScala.sumBy(_.getNumberOfScannedFiles))
    val isIncremental = !scans.values.exists(_.getTimes.getScanningType.isFull)

    val indexingTimes = history.getTimes
    val scanningTimes = scans.values.map(_.getTimes)

    val startedAt = indexingTimes.getUpdatingStart.toEpochSecond
    val finishedAt = indexingTimes.getUpdatingEnd.toEpochSecond

    val totalIndexing = nanosToMillis(indexingTimes.getTotalUpdatingTime)
    val totalScanning = nanosToMillis(scanningTimes.sumBy(_.getTotalUpdatingTime))
    val totalPaused = indexingTimes.getPausedDuration.toMillis + scanningTimes.sumBy(_.getPausedDuration.toMillis)

    val scanningAndIndexingWithoutPauses = totalIndexing + totalScanning - totalPaused
    val numberOfIndexedFiles = stats.sumBy(_.getTotalNumberOfIndexedFiles)

    val sharedIndexEvents = findSharedIndexingEvents(project)
    val sharedIndexKindsUsed = sharedIndexEvents.map(_.getKind).distinct
    val averageUtilizedToFetchedRatio = computeAverageUtilizedToFetchedRatio(sharedIndexEvents)

    val numberOfFilesCoveredBySharedIndexes = stats.sumBy(_.getTotalNumberOfFilesFullyIndexedByExtensions)
    val sharedIndexCoverageRatio = numberOfFilesCoveredBySharedIndexes.toFloat / numberOfIndexedFiles

    val isSharedIndexesEnabled = Registry.is("shared.indexes.download", false)

    val report: IndexingStats = IndexingStats(
      startedAt = startedAt,
      finishedAt = finishedAt,
      durations = Durations(
        totalIndexing = totalIndexing,
        totalScanning = totalScanning,
        scanningAndIndexing = totalIndexing + totalScanning,
        scanningAndIndexingWithoutPauses = scanningAndIndexingWithoutPauses
      ),
      isSharedIndexesEnabled = isSharedIndexesEnabled,
      isIncremental = isIncremental,
      sharedIndexKindsUsed = sharedIndexKindsUsed,
      sharedIndexCoverageRatio = sharedIndexCoverageRatio,
      numberOfScannedFiles = numberOfScannedFiles,
      numberOfIndexedFiles = numberOfIndexedFiles,
      numberOfFilesCoveredBySharedIndexes = numberOfFilesCoveredBySharedIndexes,
      averageUtilizedToFetchedRatio = averageUtilizedToFetchedRatio
    )
    IndexingStatsLogPublisher.publish(report)
  }

  private def computeAverageUtilizedToFetchedRatio(sharedIndexEvents: Iterable[Attached.Success]): Float = {
    if (sharedIndexEvents.isEmpty) {
      0
    } else {
      sharedIndexEvents
        .flatMap(e => Seq(e.getFbMatch, e.getStubMatch))
        .foldLeft(new JsonPercentages()) { case (a, b) =>
          new JsonPercentages(a.getPart + b.getPart, a.getTotal + b.getTotal)
        }
        .getPartition
        .toFloat
    }
  }

  private def findScansForIndexing(history: ProjectDumbIndexingHistory): MapView[Long, ProjectScanningHistory] = {
    scanningRecord.view.filterKeys { id =>
      history.getTimes.getScanningIds.asInstanceOf[LongCollection].contains(id)
    }
  }

  private def findSharedIndexingEvents(project: Project): List[Attached.Success] = {
    SharedIndexDiagnostic.INSTANCE
      .readEvents(project)
      .asScala
      .collect { case e: Attached.Success => e }
      .toList
  }

  private def nanosToMillis(time: Long): Long = {
    time / 1_000_000L
  }
}
