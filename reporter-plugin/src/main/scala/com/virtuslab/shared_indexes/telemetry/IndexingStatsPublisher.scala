package com.virtuslab.shared_indexes.telemetry

import com.intellij.openapi.diagnostic.Logger

trait IndexingStatsPublisher {
  def publish(stats: IndexingStats): Unit
}

object IndexingStatsLogPublisher extends IndexingStatsPublisher {
  private val logger = Logger.getInstance(classOf[IndexingStatsPublisher])

  override def publish(stats: IndexingStats): Unit = {
    logger.info(stats.toString)
  }
}
