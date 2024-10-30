package com.virtuslab.shared_indexes

import com.intellij.indexing.shared.cdn.CdnUpdatePlan
import com.intellij.indexing.shared.cdn.rebuild.CdnRebuildKt
import com.intellij.indexing.shared.cdn.upload.CdnUploadKt
import com.intellij.indexing.shared.local.Local_contextKt.createContext
import com.intellij.indexing.shared.local.Local_snapshotKt.listLocalIndexes
import com.virtuslab.shared_indexes.config.MainConfig
import com.virtuslab.shared_indexes.core._
import com.virtuslab.shared_indexes.generator.JarIndexesGenerator
import com.virtuslab.shared_indexes.generator.JdkIndexesGenerator
import com.virtuslab.shared_indexes.generator.ProjectIndexesGenerator
import com.virtuslab.shared_indexes.locator.JarLocator
import com.virtuslab.shared_indexes.logging.LoggingPrintStream
import com.virtuslab.shared_indexes.remote.JarIndexesS3Operations._
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Collections
import mainargs.ParserForMethods
import mainargs.main
import org.slf4j.LoggerFactory
import org.tukaani.xz.XZInputStream

object Main {

  private lazy val logger = LoggerFactory.getLogger(this.getClass)

  @main
  def jdk(config: MainConfig): Unit = {
    configureLogging(config)
    val workPlan = WorkPlan(config)
    import workPlan._
    val generatedIndexes = new JdkIndexesGenerator(intelliJ, workspace, inputs).generateIndexes()
    prepareFileTreeForIndexServer(generatedIndexes, indexBaseUrl.get, indexExportingStrategy.get)
    // after this, start the server, configure IJ with jdk indexes URL and test it
  }

  @main
  def jar(config: MainConfig): Unit = {
    configureLogging(config)
    val workPlan = WorkPlan(config)
    import workPlan._
    val key = commit.getOrElse {
      logger.warn("Commit is not provided, using a default value")
      "latest"
    }
    val generatedIndexes = new JarIndexesGenerator(intelliJ, workspace, inputs, key).generateIndexes()
    if (s3.isDefined) {
      if (config.jarIndexesConfig.upload.value) {
        deleteJarSharedIndexes(s3.get, key)
        uploadJarSharedIndexes(s3.get, generatedIndexes)
      }
      if (config.jarIndexesConfig.download.value) {
        // keep temp files for easier debugging
        val downloadDir = os.temp.dir(deleteOnExit = false)
        downloadJarSharedIndexes(s3.get, key, downloadDir)
        copyIndexesToIntelliJFolder(intelliJ.sharedIndexDir, downloadDir)
      }
    } else {
      copyIndexesToIntelliJFolder(intelliJ.sharedIndexDir, generatedIndexes)
    }
  }

  @main
  def project(config: MainConfig): Unit = {
    configureLogging(config)
    val workPlan = WorkPlan(config)
    import workPlan._
    if (inputs.size > 1) {
      throw new IllegalArgumentException("Project indexes generation is not supported for multiple projects")
    }
    val generatedIndexes = new ProjectIndexesGenerator(intelliJ, workspace, inputs.headOption, commit).generateIndexes()
    prepareFileTreeForIndexServer(generatedIndexes, indexBaseUrl.get, indexExportingStrategy.get)
    // For local testing without an S3 server:
    //        ShortcutsKt.startServerOnLocalIndexes(workspace.cdnPath.toNIO, 9000, "127.0.0.1")
    // Or:
    //        copyIndexesToIntelliJFolder(intelliJ, workspace.projectIndexes)
  }

  // This method has to be placed after all the @main methods
  // See https://github.com/com-lihaoyi/mainargs/issues/166
  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

  private def findJarsToIndex(workPlan: WorkPlan): Seq[os.Path] = {
    workPlan.inputs match {
      case Seq() =>
        logger.info("No inputs specified, using example jars")
        JarLocator.exampleDepJars()
      case Seq(dir) if os.isDir(dir) =>
        logger.info(s"Input is a directory, using sbt to resolve all dependency jars")
        JarLocator.findSbtDepJars(dir)
      case paths =>
        logger.info(s"Using specified inputs as jars (${paths.size})")
        paths
    }
  }

  private def prepareFileTreeForIndexServer(
      dataDir: os.Path,
      serverUrl: String,
      exportIndexes: CdnUpdatePlan => Unit
  ): Unit = {
    // The third-party code below calls 'println'
    // To keep the output clean we need to swap out the standard output
    val oldStdOut = System.out
    System.setOut(LoggingPrintStream(logger.debug))

    try {
      val ctx = createContext(serverUrl)
      // first parameter is where we look for index files, second is the base path that they
      // should be relative to
      val localIndexes = listLocalIndexes(dataDir.toNIO, dataDir.toNIO)

      val newEntries = CdnUploadKt.uploadNewEntries(ctx, localIndexes, Collections.emptyList())
      exportIndexes(newEntries)

      // For now, if we always build index from scratch, this is *probably* not needed.
      // See how these Cdn* methods are used in original code to understand how to implement
      // incremental updates on the server.
      val rebuild = CdnRebuildKt.rebuildCdnLayout(ctx, Collections.emptyList())
      exportIndexes(rebuild)
    } finally System.setOut(oldStdOut) // Swap stdout back
  }

  // Replaces shared indexes in IntelliJ installation with the ones generated by this tool
  // This is useful for testing without the server and works with Project Shared Indexes.
  private def copyIndexesToIntelliJFolder(dest: os.Path, dataDir: os.Path): Unit = {
    logger.info(s"Copying generated indexes to IntelliJ shared-index directory at $dest")
    os.list(dest).foreach(os.remove.all)
    os.copy(dataDir, dest, mergeFolders = true)
    os.list(dest).filter(_.last.endsWith(".ijx.xz")).foreach { compressedFile =>
      val decompressedFile = compressedFile / os.up / compressedFile.last.replace(".ijx.xz", ".ijx")
      os.remove.all(decompressedFile)

      val input = new XZInputStream(compressedFile.getInputStream)
      try {
        Files.copy(input, decompressedFile.toNIO, StandardCopyOption.REPLACE_EXISTING)
      } finally {
        input.close()
      }
      os.remove.all(compressedFile)
    }
  }

  private def configureLogging(config: MainConfig) = {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, config.loggingConfig.logLevel.name())
  }

}
