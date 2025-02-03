package com.virtuslab.shared_indexes.util

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
