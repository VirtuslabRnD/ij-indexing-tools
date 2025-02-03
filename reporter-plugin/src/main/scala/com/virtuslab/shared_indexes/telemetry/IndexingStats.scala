package com.virtuslab.shared_indexes.telemetry

case class IndexingStats(
    startedAt: Long,
    finishedAt: Long,
    durations: IndexingStats.Durations,
    isSharedIndexesEnabled: Boolean,
    isIncremental: Boolean,
    sharedIndexKindsUsed: Seq[String],
    sharedIndexCoverageRatio: Float,
    numberOfScannedFiles: Int,
    numberOfIndexedFiles: Int,
    numberOfFilesCoveredBySharedIndexes: Int,
    averageUtilizedToFetchedRatio: Float
)

object IndexingStats {
  case class Durations(
      totalIndexing: Long,
      totalScanning: Long,
      scanningAndIndexing: Long,
      scanningAndIndexingWithoutPauses: Long
  )
}
