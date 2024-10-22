package com.virtuslab.shared_indexes.generator

import com.virtuslab.shared_indexes.core.{IntelliJ, JdkAliases, SharedIndexes, Workspace}
import com.virtuslab.shared_indexes.locator.JdkLocator
import os.Path

class JarIndexesGenerator(intelliJ: IntelliJ, workspace: Workspace, val jarPaths: Seq[os.Path])
    extends SharedIndexesGenerator[Seq[os.Path]](intelliJ, workspace) {

  override protected val cacheDir: Path = workspace.jarIndexes

  override protected def findInputs(): Seq[os.Path] = {
    logger.info(s"Found ${jarPaths.size} JDKs:")
    jarPaths.foreach { p => logger.info(p.toString()) }
    jarPaths
  }

  override protected def process(inputs: Seq[os.Path]): Unit = {
    SharedIndexes.dumpJarSharedIndex(intelliJ, inputs, workspace, "jars", SharedIndexes.key(jarPaths))
  }
}
