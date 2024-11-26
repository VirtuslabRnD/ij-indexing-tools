package com.virtuslab.indexing.data

import com.virtuslab.indexing.data.IndexingStats.Durations

case class IndexingStats(
    startedAt: Long,
    finishedAt: Long,
    durations: Durations,
    isSharedIndexesEnabled: Boolean,
    isIncremental: Boolean,
    sharedIndexKindsUsed: Seq[String],
    numberOfIndexedFiles: Int,
    numberOfFilesCoveredBySharedIndexes: Int
)

object IndexingStats {
  case class Durations(
      totalIndexing: Long,
      totalScanning: Long,
      scanningAndIndexingActual: Long,
      scanningAndIndexingWithoutPauses: Long
  )
}
