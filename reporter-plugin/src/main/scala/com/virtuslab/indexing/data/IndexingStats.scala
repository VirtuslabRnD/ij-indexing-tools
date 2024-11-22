package com.virtuslab.indexing.data

case class IndexingStats(
    startedAt: Long,
    finishedAt: Long,
    isSharedIndexesEnabled: Boolean,
    isIncremental: Boolean,
    sharedIndexKindsUsed: Seq[String],
    numberOfIndexedFiles: Int,
    numberOfFilesCoveredBySharedIndexes: Int
)
