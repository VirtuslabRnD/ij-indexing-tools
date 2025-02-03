package com.virtuslab.shared_indexes.kinds.jars

import com.intellij.indexing.shared.cdn.S3
import com.virtuslab.shared_indexes.util.{FileUtils, Logging}
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, GetObjectRequest, ListObjectsV2Request, PutObjectRequest}

import java.nio.file.{Files, Path}

/** Utility class that might be used to integrate jar indexes upload and download with S3.
  *
  * s3 - api from JetBrains CDN, you can obtain it by creating (new S3SharedIndexStorage(...)).s3Api
  *
  * subPath - path to the folder in the bucket where indexes are stored, for example "all-3rd-party-jars"
  *
  * indexKey - identifies specific index, can be a commit hash, depending what is used to generate indexes it will be
  * the name of all 3 files (<indexKey>.ijx, <indexKey>.sha256, <indexKey>.metadata.json)
  */
class JarIndexesS3Operations(s3: S3) extends Logging {

  def delete(subPath: String, indexKeySubstring: String): Unit = {
    logger.info(s"Deleting old JAR indexes for key $indexKeySubstring")
    val req1 = ListObjectsV2Request.builder()
      .bucket(s3.getBucket)
      .prefix(subPath)
      .build()
    val contents = s3.getClient.listObjectsV2(req1).contents()
    contents.forEach { c =>
      val matches = c.key().contains(indexKeySubstring)
      if (matches) {
        val req2 = DeleteObjectRequest.builder()
          .bucket(s3.getBucket)
          .key(c.key())
          .build()
        s3.getClient.deleteObject(req2)
      }
    }
  }

  def upload(localIndexesDir: Path, subPath: String): Unit = {
    logger.info("Uploading JAR indexes")
    FileUtils.listFiles(localIndexesDir).foreach { file =>
      // See com.intellij.indexing.shared.cdn.upload.CdnUploadEntry
      val contentType = FileUtils.extension(localIndexesDir) match {
        case "xz"   => "application/xz"
        case "json" => "application/json"
        case _      => "application/octet-stream"
      }
      val request = PutObjectRequest.builder()
        .bucket(s3.getBucket)
        .key(subPath + "/" + FileUtils.name(file))
        .contentType(contentType)
        .build()
      s3.getClient.putObject(request, file)
    }
  }

  def download(subPath: String, indexKeySubstring: String, destDir: Path): Unit = {
    logger.info(s"Downloading JAR indexes to $destDir")
    val req1 = ListObjectsV2Request.builder()
      .bucket(s3.getBucket)
      .prefix(subPath)
      .build()
    val contents = s3.getClient.listObjectsV2(req1).contents()
    contents.forEach { c =>
      if (c.key().contains(indexKeySubstring)) {
        val req2 = GetObjectRequest.builder()
          .bucket(s3.getBucket)
          .key(c.key())
          .build()
        val res = s3.getClient.getObjectAsBytes(req2).asInputStream()
        val dest = destDir.resolve(c.key().stripPrefix(subPath + "/"))
        Files.copy(res, dest)
        res.close()
      }
    }
  }

}
