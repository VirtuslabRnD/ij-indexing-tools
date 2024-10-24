package com.virtuslab.shared_indexes.generator

import com.virtuslab.shared_indexes.core.{IntelliJ, Workspace}
import org.slf4j.{Logger, LoggerFactory}

abstract class SharedIndexesGenerator[I](protected val intelliJ: IntelliJ, protected val workspace: Workspace) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected val cacheDir: os.Path

  protected def findInputs(): I
  protected def process(inputs: I): Unit
  def generateIndexes(): os.Path = {
    if (os.list(cacheDir).isEmpty) {
      process(findInputs())
    } else {
      // FIXME verify that the cached indexes actually correspond to our inputs
      logger.info("Indexes already exist, skipping generation")
    }
    cacheDir
  }
}
