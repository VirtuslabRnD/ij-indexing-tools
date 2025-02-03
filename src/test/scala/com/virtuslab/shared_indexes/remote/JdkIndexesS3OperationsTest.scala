package com.virtuslab.shared_indexes.remote

import com.intellij.indexing.shared.cdn.upload.{CdnUploadDataEntry, CdnUploadEntry}
import com.intellij.indexing.shared.cdn.{CdnUpdatePlan, S3, S3CdnEntry}
import com.virtuslab.shared_indexes.core.storage.S3Upload
import org.scalamock.scalatest.MockFactory
import org.scalatest.funsuite.AnyFunSuite
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, PutObjectRequest, S3Object}

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.util.Try

class JdkIndexesS3OperationsTest extends AnyFunSuite with MockFactory {

  private val s3Client = stub[S3Client]
  private val s3 = new S3("http://example.s3", "test-bucket", s3Client, "/path/to/indexes", 1)

  test("update") {
    // Given
    val toUpload =
      new CdnUploadDataEntry("shared-index-jdk.metadata.json", "application/json", () => Array.emptyByteArray)
    val toRemove = new S3CdnEntry(
      s3,
      S3Object.builder().key("deleteme").size(56L).lastModified(Instant.parse("2024-11-06T10:15:30Z")).build()
    )
    val newEntries = Seq[CdnUploadEntry](toUpload).asJava
    val removeEntries = Seq(toRemove).asJava
    val updatePlan = new CdnUpdatePlan(newEntries, removeEntries)
    val s3Upload = new S3Upload(s3)

    // When
    s3Upload.updateS3Indexes(updatePlan)

    // Then
    (s3Client.putObject(_: PutObjectRequest, _: RequestBody)).verify(where { (req: PutObjectRequest, _: RequestBody) =>
      req.bucket() == "test-bucket" &&
      req.contentType() == "application/json" &&
      req.key() == "/path/to/indexes/shared-index-jdk.metadata.json"
    })

    (s3Client.deleteObject(_: DeleteObjectRequest)).verify(where { req: DeleteObjectRequest =>
      req.bucket() == "test-bucket" &&
      req.key() == "/path/to/indexes/deleteme"
    })

  }

  test("single server error") {
    // Given
    val newEntries = Seq(
      "shared-index-jdk.metadata.json" -> "application/json",
      "shared-index-jdk.ijx.xz" -> "application/xz",
      "shared-index-jdk.sha256" -> "application/octet-stream"
    ).map { case (key, contentType) =>
      new CdnUploadDataEntry(key, contentType, () => Array.emptyByteArray)
    }.asJava
    val removeEntries = Seq().asJava
    val updatePlan = new CdnUpdatePlan(newEntries, removeEntries)
    val s3Upload = new S3Upload(s3)
    (s3Client.putObject(_: PutObjectRequest, _: RequestBody)).when(*, *).throws(new Exception("Server error")).once()

    // When
    val exception = Try {
      s3Upload.updateS3Indexes(updatePlan)
    }.failed.get

    // Then
    Seq(
      "shared-index-jdk.metadata.json" -> "application/json",
      "shared-index-jdk.ijx.xz" -> "application/xz",
      "shared-index-jdk.sha256" -> "application/octet-stream"
    ).foreach { case (key, contentType) =>
      (s3Client.putObject(_: PutObjectRequest, _: RequestBody)).verify(where {
        (req: PutObjectRequest, _: RequestBody) =>
          req.contentType() == contentType &&
          req.key() == "/path/to/indexes/" + key
      })
    }
    val expectedErrorMessage =
      """Failed to upload 1 entries:
        |  UploadDataEntry(key=shared-index-jdk.metadata.json, type=application/json):
        |    Server error
        |""".stripMargin

    assert(exception.getMessage == expectedErrorMessage)
  }

}
