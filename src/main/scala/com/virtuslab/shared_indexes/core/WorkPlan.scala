package com.virtuslab.shared_indexes.core

import com.intellij.indexing.shared.cdn.CdnUpdatePlan
import com.intellij.indexing.shared.cdn.S3
import com.intellij.indexing.shared.cdn.S3Kt
import com.intellij.indexing.shared.local.Local_uploadKt
import com.virtuslab.shared_indexes.config.MainConfig
import com.virtuslab.shared_indexes.remote.S3Upload

case class WorkPlan(
    intelliJ: IntelliJ,
    workspace: Workspace,
    inputs: Seq[os.Path],
    commit: Option[String],
    indexBaseUrl: Option[String] = None,
    indexExportingStrategy: Option[CdnUpdatePlan => Unit] = None,
    s3: Option[S3] = None
)

object WorkPlan {

  def apply(config: MainConfig): WorkPlan =
    withServerUploadPlan(createBaseWorkPlan(config), config)

  private def createBaseWorkPlan(config: MainConfig): WorkPlan = {
    import config._
    val intelliJ = {
      import generatorConfig._
      new IntelliJ(ideaBinary, ideaCacheDir)
    }
    val workspace = new Workspace(generatorConfig.workDir)
    WorkPlan(intelliJ, workspace, indexInputConfig.inputs, indexInputConfig.commit)
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
          (Some(s3), new S3Upload(s3).updateS3Indexes _)
        case None =>
          // updateLocalIndexes executes the update plan in given basePath, which is the location that
          // the server should host
          val basePath = workspace.cdnPath.toNIO
          (None, Local_uploadKt.updateLocalIndexes(basePath, _))
      }
    }

    basePlan.copy(indexBaseUrl = Some(indexBaseUrl), indexExportingStrategy = Some(indexExportingStrategy), s3 = s3Opt)
  }
}
