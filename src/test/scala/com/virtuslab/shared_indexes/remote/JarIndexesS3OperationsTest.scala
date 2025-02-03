package com.virtuslab.shared_indexes.remote

import com.intellij.indexing.shared.cdn.S3
import com.virtuslab.shared_indexes.kinds.jars.JarIndexesS3Operations
import org.scalamock.scalatest.MockFactory
import org.scalatest.funsuite.AnyFunSuite
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{S3Object, DeleteObjectRequest => DelObjReq, DeleteObjectResponse => DelObjRes, GetObjectRequest => GetObjReq, GetObjectResponse => GetObjRes, ListObjectsV2Request => ListObjReq, ListObjectsV2Response => ListObjRes, PutObjectRequest => PutObjReq}

import java.nio.file.Path

class JarIndexesS3OperationsTest extends AnyFunSuite with MockFactory {

  private val s3Client = stub[S3Client]
  private val s3 = new S3("http://example.s3", "test-bucket", s3Client, "/path/to/indexes", 1)
  test("delete") {
    // Given
    val existingObjects = ListObjRes.builder().contents(
      Seq("index-key", "other-index-key", "not-matching-key").map(k => S3Object.builder().key(k).build()): _*
    ).build()
    (s3Client.listObjectsV2(_: ListObjReq)).when(*) returns existingObjects
    (s3Client.deleteObject(_: DelObjReq)).when(*) returns DelObjRes.builder().build()
    // When
    new JarIndexesS3Operations(s3).delete("all-jars", "index-key")

    // Then
    (s3Client.listObjectsV2(_: ListObjReq)) verify where[ListObjReq] {
      // TODO Should rootInBucket affect the prefix? Currently it is just ignored
      req => req.bucket() == "test-bucket" && req.prefix() == "all-jars"
    }
    (s3Client.deleteObject(_: DelObjReq)).verify(where[DelObjReq] { req =>
      req.bucket() == "test-bucket" && req.key() == "index-key"
    })
    (s3Client.deleteObject(_: DelObjReq)).verify(where[DelObjReq] { req =>
      req.bucket() == "test-bucket" && req.key() == "other-index-key"
    })
    (s3Client.deleteObject(_: DelObjReq)).verify(where[DelObjReq] { req =>
      req.key() == "not-matching-key"
    }).never()
  }

  test("upload") {
    // Given
    val indexesDir = os.temp.dir(deleteOnExit = true)
    os.write(indexesDir / "shared-index.ijx.xz", Array[Byte]())
    os.write(indexesDir / "shared-index.metadata.json", Array[Byte]())
    os.write(indexesDir / "shared-index.sha256", Array[Byte]())
    // (s3Client.putObject(_: PutObjectRequest, _: Path)).when(*, *).returns(null)

    // When
    new JarIndexesS3Operations(s3).upload(indexesDir.toNIO, "all-jars")

    // Then
    (s3Client.putObject(_: PutObjReq, _: Path)).verify(where { (req: PutObjReq, file: Path) =>
      req.bucket() == "test-bucket" &&
      req.contentType() == "application/xz" &&
      req.key() == "all-jars/shared-index.ijx.xz" &&
      os.Path(file) == indexesDir / "shared-index.ijx.xz"
    })
    (s3Client.putObject(_: PutObjReq, _: Path)).verify(where { (req: PutObjReq, file: Path) =>
      req.bucket() == "test-bucket" &&
      req.contentType() == "application/json" &&
      req.key() == "all-jars/shared-index.metadata.json" &&
      os.Path(file) == indexesDir / "shared-index.metadata.json"
    })
    (s3Client.putObject(_: PutObjReq, _: Path)).verify(where { (req: PutObjReq, file: Path) =>
      req.bucket() == "test-bucket" &&
      req.contentType() == "application/octet-stream" &&
      req.key() == "all-jars/shared-index.sha256" &&
      os.Path(file) == indexesDir / "shared-index.sha256"
    })
  }

  test("download") {
    // Given
    val indexesDir = os.temp.dir(deleteOnExit = true)
    val existingObjects = ListObjRes.builder().contents(
      Seq("index-key", "other-index-key", "not-matching-key").map(k => S3Object.builder().key(k).build()): _*
    ).build()
    (s3Client.listObjectsV2(_: ListObjReq)).when(*) returns existingObjects
    val dummyResponse = GetObjRes.builder().build()
    (s3Client.getObjectAsBytes(_: GetObjReq)).when(*) returns ResponseBytes.fromByteArray(dummyResponse, Array())
    // When
    new JarIndexesS3Operations(s3).download("all-jars", "index-key", indexesDir.toNIO)

    // Then
    (s3Client.listObjectsV2(_: ListObjReq)) verify where[ListObjReq] { req =>
      req.bucket() == "test-bucket" && req.prefix() == "all-jars"
    }
    (s3Client.getObjectAsBytes(_: GetObjReq)).verify(where[GetObjReq] { req =>
      req.bucket() == "test-bucket" && req.key() == "index-key"
    })
    (s3Client.getObjectAsBytes(_: GetObjReq)).verify(where[GetObjReq] { req =>
      req.bucket() == "test-bucket" && req.key() == "other-index-key"
    })
    (s3Client.getObjectAsBytes(_: GetObjReq)).verify(where[GetObjReq] { req =>
      req.bucket() == "test-bucket" && req.key() == "not-matching-key"
    }).never()
  }
}
