package com.virtuslab.shared_indexes.kinds.jdk

import com.virtuslab.shared_indexes.core.storage.SharedIndexStorage
import com.virtuslab.shared_indexes.core.{IntelliJRunner, SharedIndexesService, WorkspaceSetup}

import java.nio.file.{Files, Path}

class JdkIndexesService(
    outputsDir: Path,
    ij: IntelliJRunner,
    ws: WorkspaceSetup,
    jdkPaths: Seq[Path],
    regenerate: Boolean,
    debug: Boolean
) extends SharedIndexesService("jdk", outputsDir, ij, ws, debug) {

  override protected def store(generatedIndexesDir: Path, storage: SharedIndexStorage): Unit = {
    new JdkSharedIndexCleanup(regenerate).cleanup(storage)
    super.store(generatedIndexesDir, storage)
  }

  override protected def executeGeneration(): Unit = {
    // We generate indexes one by one. Otherwise, IntelliJ merges
    // them into one big index which doesn't seem to be picked
    // up by the existing logic.
    for (jdkPath <- jdkPaths) {
      val aliases = JdkAliases.resolve(jdkPath)
      logger.info(s"Generating shared indexes for JDK $jdkPath with aliases $aliases")
      val tempDir = Files.createDirectories(outputsDir.resolve("temp"))
      val args = Seq(s"--jdk-home=$jdkPath", s"--temp-dir=$tempDir") ++ aliases.map(alias => s"--alias=$alias")
      dumpSharedIndex(args)
    }
  }

}
