package com.virtuslab.shared_indexes.generator
import com.virtuslab.shared_indexes.core.{IntelliJ, JdkAliases, SharedIndexes, Workspace}
import com.virtuslab.shared_indexes.locator.JdkLocator
import os.Path

class JdkIndexesGenerator(intelliJ: IntelliJ, workspace: Workspace, val customJdkPaths: Seq[os.Path])
    extends SharedIndexesGenerator[Seq[os.Path]](intelliJ, workspace) {

  override protected val cacheDir: Path = workspace.jdkIndexes

  override protected def findInputs(): Seq[os.Path] = {
    val jdkPaths = if (customJdkPaths.isEmpty) JdkLocator.findAllInstalledJdks() else customJdkPaths
    logger.info(s"Found ${jdkPaths.size} JDKs:")
    jdkPaths.foreach { p => logger.info(p.toString()) }
    jdkPaths
  }

  override protected def process(inputs: Seq[os.Path]): Unit = {
    // I generate indexes one by one. Otherwise, IntelliJ merges
    // them into one big index which doesn't seem to be picked
    // up by the existing logic.
    for (jdkPath <- inputs) {
      val aliases = JdkAliases.resolve(jdkPath)
      logger.info(s"Generating shared indexes for JDK $jdkPath with aliases $aliases")
      SharedIndexes.dumpJdkSharedIndexes(intelliJ, jdkPath, aliases, workspace)
    }
  }
}
