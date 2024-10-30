package com.virtuslab.shared_indexes.generator

import com.virtuslab.shared_indexes.core.IntelliJ
import com.virtuslab.shared_indexes.core.SharedIndexes
import com.virtuslab.shared_indexes.core.Workspace
import os.Path

class JarIndexesGenerator(intelliJ: IntelliJ, workspace: Workspace, val jarPaths: Seq[os.Path], key: String)
    extends SharedIndexesGenerator[Seq[os.Path]](intelliJ, workspace) {

  override protected val cacheDir: Path = workspace.jarIndexes

  override protected def findInputs(): Seq[os.Path] = {
    logger.info(s"Found ${jarPaths.size} JDKs:")
    jarPaths.foreach(p => logger.info(p.toString()))
    jarPaths
  }

  override protected def process(inputs: Seq[os.Path]): Unit = {
    SharedIndexes.dumpSharedIndex(
      intelliJ,
      workspace,
      workspace.jarIndexes,
      "jars",
      args = Seq(
        s"--kind=jars",
        s"--chunk-name=$key"
      ) ++ inputs.map(path => s"--jar=$path")
    )
  }

}
