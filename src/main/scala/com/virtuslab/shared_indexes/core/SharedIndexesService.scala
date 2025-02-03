package com.virtuslab.shared_indexes.core

import com.virtuslab.shared_indexes.core.storage.SharedIndexStorage
import com.virtuslab.shared_indexes.util.Logging

import java.nio.file.{Files, Path}

abstract class SharedIndexesService(
    kind: String,
    outputsDir: Path,
    intelliJRunner: IntelliJRunner,
    workspaceSetup: WorkspaceSetup,
    debug: Boolean
) extends Logging {

  private val localOutputDir: Path = Files.createDirectories(outputsDir.resolve(kind))

  protected lazy val workspace: Path = workspaceSetup.setup().toAbsolutePath.normalize()

  final def store(generatedIndexesDir: Path, storages: Seq[SharedIndexStorage]): Unit = {
    storages.foreach { storage =>
      store(generatedIndexesDir, storage)
    }
  }

  protected def store(generatedIndexesDir: Path, storage: SharedIndexStorage): Unit = {
    storage.store(generatedIndexesDir, debug)
  }

  final def generateIndexes(): Path = {
    if (Files.list(localOutputDir).count() > 0 && debug) {
      logger.info("Indexes already exist, skipping generation")
    } else {
      executeGeneration()
    }
    localOutputDir
  }

  protected def executeGeneration(): Unit

  /** Calls IntelliJ internal app to generate shared indexes for JDKs.
    *
    * To learn about available flags and how they work, start from
    * com.intellij.indexing.shared.generator.DumpSharedIndexCommand interface and check its implementations.
    */
  protected def dumpSharedIndex(args: Seq[String], compression: String = "plain"): Unit = {
    val subcommand = kind
    val command = Seq(
      "dump-shared-index",
      subcommand,
      s"--output=$localOutputDir",
      s"--compression=$compression"
    ) ++ args

    val result = intelliJRunner.run(workspace, command)
    if (result.exitCode != 0) {
      throw new RuntimeException(s"IntelliJ exited with code ${result.exitCode}")
    }
  }
}
