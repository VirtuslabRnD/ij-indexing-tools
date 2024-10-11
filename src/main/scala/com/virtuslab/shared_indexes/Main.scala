package com.virtuslab.shared_indexes

import com.intellij.indexing.shared.cdn.S3_uploadKt.updateS3Indexes
import com.intellij.indexing.shared.cdn.rebuild.CdnRebuildKt
import com.intellij.indexing.shared.cdn.upload.CdnUploadKt
import com.intellij.indexing.shared.cdn.{CdnUpdatePlan, S3, S3Kt}
import com.intellij.indexing.shared.local.Local_contextKt.createContext
import com.intellij.indexing.shared.local.Local_snapshotKt.listLocalIndexes
import com.intellij.indexing.shared.local.Local_uploadKt.updateLocalIndexes
import com.virtuslab.shared_indexes.config.MainConfig
import com.virtuslab.shared_indexes.core._
import com.virtuslab.shared_indexes.locator.{JarLocator, JdkLocator, ProjectLocator}
import mainargs.{ParserForMethods, main}
import org.slf4j.LoggerFactory
import org.tukaani.xz.XZInputStream
import software.amazon.awssdk.services.s3.model.{
  DeleteObjectRequest,
  GetObjectRequest,
  ListObjectsV2Request,
  PutObjectRequest
}

import java.nio.file.{Files, StandardCopyOption}
import java.util.Collections

object Main {

  private val logger = LoggerFactory.getLogger(this.getClass)

  @main
  def jdk(config: MainConfig): Unit = {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, config.loggingConfig.logLevel.name())
    val baseWorkPlan = createBaseWorkPlan(config)
    val workPlan = withServerUploadPlan(baseWorkPlan, config)
    import workPlan._
    generateJdkSharedIndexes(intelliJ, workspace, artifactPaths)
    prepareFileTreeForIndexServer(workspace.jdkIndexes, indexBaseUrl.get, indexExportingStrategy.get)
    // after this, start the server, configure IJ with jdk indexes URL and test it
  }

  @main
  def jar(config: MainConfig): Unit = {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, config.loggingConfig.logLevel.name())
    val baseWorkPlan = createBaseWorkPlan(config)
    val workPlan = withServerUploadPlan(baseWorkPlan, config)
    val jarPaths = findJarsToIndex(workPlan)
    import workPlan._
    import config.jarIndexesConfig._
    if (upload.value) {
      generateJarSharedIndexes(intelliJ, workspace, jarPaths)
      deleteJarSharedIndexes(s3.get, SharedIndexes.key(jarPaths))
      uploadJarSharedIndexes(s3.get, workspace)
    }
    if (download.value) {
      // keep temp files for easier debugging
      val downloadDir = os.temp.dir(deleteOnExit = false)
      downloadJarSharedIndexes(s3.get, SharedIndexes.key(jarPaths), downloadDir)
      copyIndexesToIntelliJFolder(intelliJ, downloadDir)
    }
  }

  @main
  def project(config: MainConfig): Unit = {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, config.loggingConfig.logLevel.name())
    val baseWorkPlan = createBaseWorkPlan(config)
    val workPlan = withServerUploadPlan(baseWorkPlan, config)
    import workPlan._
    generateProjectSharedIndexes(intelliJ, workspace, projectRoot, commit)
    prepareFileTreeForIndexServer(workspace.projectIndexes, indexBaseUrl.get, indexExportingStrategy.get)
    // For local testing without an S3 server:
    //        ShortcutsKt.startServerOnLocalIndexes(workspace.cdnPath.toNIO, 9000, "127.0.0.1")
    // Or:
    //        copyIndexesToIntelliJFolder(intelliJ, workspace.projectIndexes)
  }

  // This method has to be placed after all the @main methods
  // See https://github.com/com-lihaoyi/mainargs/issues/166
  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

  case class WorkPlan(
      intelliJ: IntelliJ,
      workspace: Workspace,
      projectRoot: Option[os.Path],
      artifactPaths: Seq[os.Path],
      commit: Option[String],
      indexBaseUrl: Option[String] = None,
      indexExportingStrategy: Option[CdnUpdatePlan => Unit] = None,
      s3: Option[S3] = None
  )

  private def createBaseWorkPlan(config: MainConfig): WorkPlan = {
    import config._
    val intelliJ = {
      import generatorConfig._
      new IntelliJ(ideaBinary, ideaCacheDir)
    }
    val workspace = new Workspace(generatorConfig.workDir)
    WorkPlan(intelliJ, workspace, indexInputConfig.projectRoot, indexInputConfig.artifactPaths, indexInputConfig.commit)
  }

  private def withServerUploadPlan(basePlan: WorkPlan, config: MainConfig): WorkPlan = {
    import basePlan.workspace
    import config._
    val indexBaseUrl = indexStorageConfig.indexServerUrl match {
      case Some(url) =>
        s3Config.bucketName match {
          case Some(bucket) => url.replaceAll("/$", "") + "/" + bucket
          case None         => url
        }
      // If the server URL is not provided, use the workspace path instead.
      // For example, if the workspace is located at /var/foo/bar/baz,
      // the IntelliJ registry should look like this:
      // shared.indexes.jdk.download.url=file:///var/foo/bar/baz/jdk
      case None => "file://" + workspace.cdnPath
    }

    val (s3Opt, indexExportingStrategy): (Option[S3], CdnUpdatePlan => Unit) = {
      s3Config.bucketName match {
        case Some(bucketName) =>
          // To upload to S3, you need to provide access credentials.
          // The easiest way is setting up these environment variables:
          // AWS_ACCESS_KEY_ID=xxxxx123456789xxxxx
          // AWS_SECRET_ACCESS_KEY=xxxxx123456789xxxxx
          // (replace the values with valid credentials to your S3 service)
          // For other ways to supply credentials, see
          // com.amazonaws.auth.DefaultAWSCredentialsProviderChain

          // You must make sure the S3 API is accessible and the bucket exists
          val s3ApiUrl = indexStorageConfig.indexServerUrl
            .getOrElse(throw new IllegalStateException("Must provide the server address for uploading to S3"))
          val s3 = S3Kt.S3(s"$s3ApiUrl/$bucketName", bucketName, s3ApiUrl, "/", 10)
          (Some(s3), updatePlan => updateS3Indexes(s3, updatePlan))
        case None =>
          // updateLocalIndexes executes the update plan in given basePath, which is the location that
          // the server should host
          val basePath = workspace.cdnPath.toNIO
          (None, updatePlan => updateLocalIndexes(basePath, updatePlan))
      }
    }

    basePlan.copy(indexBaseUrl = Some(indexBaseUrl), indexExportingStrategy = Some(indexExportingStrategy), s3 = s3Opt)
  }

  private def generateJdkSharedIndexes(intelliJ: IntelliJ, workspace: Workspace, customJdkPaths: Seq[os.Path]): Unit = {
    // I only generate indexes if they are not already generated to easier work with
    // next step, i.e. layout. Alternatively the tool could have separate subcommands.
    if (os.list(workspace.jdkIndexes).isEmpty) {
      logger.info("Generating JDK indexes")
      val jdkPaths = if (customJdkPaths.isEmpty) JdkLocator.findAllInstalledJdks() else customJdkPaths

      logger.info(s"Found ${jdkPaths.size} JDKs:")
      jdkPaths.foreach { p => logger.info(p.toString()) }

      // I generate indexes one by one. Otherwise, IntelliJ merges
      // them into one big index which doesn't seem to be picked
      // up by the existing logic.
      for (jdkPath <- jdkPaths) {
        val aliases = JdkAliases.resolve(jdkPath)
        logger.info(s"Generating shared indexes for JDK $jdkPath with aliases $aliases")
        SharedIndexes.dumpJdkSharedIndexes(intelliJ, jdkPath, aliases, workspace)
      }
    } else {
      logger.info("JDK indexes already exist, skipping")
    }
  }

  private def findJarsToIndex(workPlan: WorkPlan): Seq[os.Path] = {
    import workPlan._
    (projectRoot match {
      case Some(dir) => JarLocator.getSbtDeps(dir)
      case None      => artifactPaths
    }) match {
      case Seq() => JarLocator.findAllJars()
      case paths => paths
    }
  }

  private def deleteJarSharedIndexes(s3: S3, indexKey: String): Unit = {
    logger.info(s"Deleting old JAR indexes for key $indexKey")
    val req1 = ListObjectsV2Request.builder()
      .bucket(s3.getBucket)
      .prefix("all-jars")
      .build()
    val contents = s3.getClient.listObjectsV2(req1).contents()
    contents.forEach { c =>
      // TODO should the matching here be exact or fuzzy?
      // If the *key* of an old index closely matches the new key,
      // we might want to remove it to save storage space.
      // On the other hand, if the storage space is unlimited,
      // we might want to keep the old indexes for the sake of older versions of the project.
      // Maybe it is better to have a separate scheduled job
      // which would automatically remove old indexes after a few weeks.
      val matches = c.key() contains indexKey
      if (matches) {
        val req2 = DeleteObjectRequest.builder()
          .bucket(s3.getBucket)
          .key(c.key())
          .build()
        s3.getClient.deleteObject(req2)
      }
    }
  }

  private def uploadJarSharedIndexes(s3: S3, workspace: Workspace): Unit = {
    logger.info("Uploading JAR indexes")
    os.list(workspace.jarIndexes).foreach { f =>
      // See com.intellij.indexing.shared.cdn.upload.CdnUploadEntry
      val contentType = f.ext match {
        case "xz"   => "application/xz"
        case "json" => "application/json"
        case _      => "application/octet-stream"
      }
      val request = PutObjectRequest.builder()
        .bucket(s3.getBucket)
        .key("all-jars/" + f.wrapped.getFileName.toString)
        .contentType(contentType)
        .build()
      s3.getClient.putObject(request, f.toNIO)
    }
  }

  private def downloadJarSharedIndexes(s3: S3, indexKey: String, destDir: os.Path): Unit = {
    logger.info(s"Downloading JAR indexes to $destDir")
    val req1 = ListObjectsV2Request.builder()
      .bucket(s3.getBucket)
      .prefix("all-jars")
      .build()
    val contents = s3.getClient.listObjectsV2(req1).contents()
    contents.forEach { c =>
      // TODO implement fuzzy matching
      if (c.key() contains indexKey) {
        val req2 = GetObjectRequest.builder()
          .bucket(s3.getBucket)
          .key(c.key())
          .build()
        val res = s3.getClient.getObjectAsBytes(req2).asInputStream()
        val dest = destDir / c.key().stripPrefix("all-jars/")
        println(dest)
        os.write(dest, res)
        res.close()
      }
    }
  }

  private def generateJarSharedIndexes(intellij: IntelliJ, workspace: Workspace, jarPaths: Seq[os.Path]): Unit = {
    // TODO make sure that the cached indexes actually correspond to our set of JARs
    if (os.list(workspace.jarIndexes).isEmpty) {
      logger.info(s"Found ${jarPaths.size} JARs:")
      jarPaths.foreach { p => logger.info(p.toString()) }
      SharedIndexes.dumpJarSharedIndex(intellij, jarPaths, workspace, "jars", SharedIndexes.key(jarPaths))
    } else {
      logger.info("JARs indexes already exist, skipping")
    }
  }

  private def generateProjectSharedIndexes(
      intellij: IntelliJ,
      workspace: Workspace,
      projectRootOpt: Option[os.Path],
      commitOpt: Option[String]
  ): Unit = {
    if (os.list(workspace.projectIndexes).isEmpty) {
      val projectRoot = projectRootOpt.getOrElse(ProjectLocator.exampleProjectHome)
      val commit = commitOpt.getOrElse(ProjectLocator.getGitCommit(projectRoot))

      logger.info(s"Found project at $projectRoot with HEAD $commit")
      SharedIndexes.dumpProjectSharedIndex(intellij, projectRoot, commit, workspace)
    } else {
      logger.info("Project indexes already exist, skipping")
    }
  }

  private def prepareFileTreeForIndexServer(
      dataDir: os.Path,
      serverUrl: String,
      exportIndexes: CdnUpdatePlan => Unit
  ): Unit = {
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

  private def copyIndexesToIntelliJFolder(intelliJ: IntelliJ, dataDir: os.Path): Unit =
    copyIndexesToIntelliJFolder(intelliJ.sharedIndexDir, dataDir)

}
