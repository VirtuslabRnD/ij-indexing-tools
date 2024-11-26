package com.virtuslab.indexing.publish

import com.intellij.openapi.diagnostic.Logger
import com.virtuslab.indexing.data.IndexingStats

trait IndexingStatsPublisher {
  def publish(stats: IndexingStats): Unit
}

object IndexingStatsLogPublisher extends IndexingStatsPublisher {

  private val logger = Logger.getInstance(classOf[IndexingStatsPublisher])
  override def publish(stats: IndexingStats): Unit = {
    // TODO make sure the log format is reasonable
    logger.info(stats.toString)
  }
}
