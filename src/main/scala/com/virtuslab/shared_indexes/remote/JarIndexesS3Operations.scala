package com.virtuslab.shared_indexes.remote

import com.intellij.indexing.shared.cdn.S3
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.s3.model.{
  DeleteObjectRequest,
  GetObjectRequest,
  ListObjectsV2Request,
  PutObjectRequest
}

object JarIndexesS3Operations {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def deleteJarSharedIndexes(s3: S3, indexKey: String): Unit = {
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

  def uploadJarSharedIndexes(s3: S3, jarIndexesDir: os.Path): Unit = {
    logger.info("Uploading JAR indexes")
    os.list(jarIndexesDir).foreach { f =>
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

  def downloadJarSharedIndexes(s3: S3, indexKey: String, destDir: os.Path): Unit = {
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

}
