package com.virtuslab.shared_indexes.kinds.jars

import com.virtuslab.shared_indexes.core.storage.{CdnBasedIndexStorage, FileSystemSharedIndexStorage, LocalIntelliJStorage, S3SharedIndexStorage, SharedIndexStorage}
import com.virtuslab.shared_indexes.core.{IntelliJRunner, SharedIndexesService, WorkspaceSetup}

import java.nio.file.Path

class JarsIndexesService(
    outputsDir: Path,
    ij: IntelliJRunner,
    ws: WorkspaceSetup,
    jarPaths: Seq[Path],
    indexKey: String,
    debug: Boolean
) extends SharedIndexesService("jars", outputsDir, ij, ws, debug) {

  override protected def store(generatedIndexesDir: Path, storage: SharedIndexStorage): Unit = {
    storage match {
      case _: FileSystemSharedIndexStorage =>
        logger.warn("Uploading to file system CDN is not supported for jars")
      case s3Storage: S3SharedIndexStorage =>
        new JarIndexesS3Operations(s3Storage.s3Api).upload(generatedIndexesDir, "jars")

      case otherStorage => otherStorage.store(generatedIndexesDir, debug)
    }
  }

  override protected def executeGeneration(): Unit = {
    logger.info(s"Indexing ${jarPaths.size} jars:")
    jarPaths.foreach(p => logger.info(p.toString))

    val args = Seq(
      s"--kind=jars",
      s"--chunk-name=$indexKey"
    ) ++ jarPaths.map(path => s"--jar=$path")

    dumpSharedIndex(args)
  }
}
