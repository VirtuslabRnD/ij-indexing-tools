package com.virtuslab.shared_indexes.remote

import com.intellij.indexing.shared.cdn.upload.{CdnUploadAnotherEntry, CdnUploadDataEntry, CdnUploadEntry}
import com.intellij.indexing.shared.cdn.{CdnEntry, CdnUpdatePlan, S3, S3CdnEntry}
import com.intellij.indexing.shared.local.LocalDiskEntry
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, PutObjectRequest}

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.file.Files
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try, Using}

class S3Upload(private val s3: S3, private val timeout: Duration = Duration(5, "min")) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val rootInBucket: String = s3.getRootInBucket
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(s3.getDispatcher.getExecutor)

  def updateS3Indexes(plan: CdnUpdatePlan): Unit = {
    if (plan.getNewEntries.isEmpty && plan.getRemoveEntries.isEmpty) {
      logger.warn("The update plan is empty, nothing to do!")
      return
    }

    val failedRemoves = removeFiles(plan.getRemoveEntries.asScala)
    val failedUploads = uploadFiles(plan.getNewEntries.asScala)
    val failureSummary = new StringBuilder()
    if (failedRemoves.nonEmpty) {
      failureSummary.append(s"Failed to remove ${failedRemoves.size} entries: \n")
      failedRemoves.foreach { case (entry, e) =>
        failureSummary.append(s"  $entry: \n    ${e.getMessage}\n")
      }
    }
    if (failedUploads.nonEmpty) {
      failureSummary.append(s"Failed to upload ${failedUploads.size} entries: \n")
      failedUploads.foreach { case (entry, e) =>
        failureSummary.append(s"  $entry: \n    ${e.getMessage}\n")
      }
    }
    if (failureSummary.nonEmpty) {
      throw new RuntimeException(failureSummary.toString())
    }
  }

  private def uploadFiles(files: Iterable[CdnUploadEntry]): Map[CdnUploadEntry, Throwable] = {
    logger.info("Uploading {} items...", files.size)
    val results = awaitAll(putObject)(files)
    collectFailures(files, results)
  }

  private def removeFiles(files: Iterable[CdnEntry]): Map[CdnEntry, Throwable] = {
    logger.info("Removing {} items...", files.size)
    val results = awaitAll(deleteObject)(files)
    collectFailures(files, results)
  }

  private def awaitAll[T, R](task: T => Future[R])(inputs: Iterable[T]): Iterable[Try[R]] = {
    val futures = inputs.map(task)
    val completions = futures.map(_.transform { case _: Try[R] => Success(()) })
    val allCompleted = Future.sequence(completions)
    var timeoutException: Option[TimeoutException] = None
    try {
      Await.ready(allCompleted, timeout)
    } catch {
      case e: TimeoutException => timeoutException = Some(e)
    }
    futures.map(_.value.getOrElse(Failure(timeoutException.get)))
  }

  private def collectFailures[T, R](keys: Iterable[T], results: Iterable[Try[R]]): Map[T, Throwable] = {
    (keys zip results).collect { case (file, Failure(e)) =>
      (file, e)
    }.toMap
  }

  private def putObject(entry: CdnUploadEntry): Future[Unit] = {
    require(
      !entry.getKey.startsWith(s"${rootInBucket}/"),
      s"Key must not start with S3 paths prefix (${rootInBucket}): ${entry.getKey}"
    )
    val req = PutObjectRequest.builder()
      .bucket(s3.getBucket)
      .key(keyInBucket(entry))
      .contentType(entry.getContentType)
      .build()
    Future {
      logger.info("S3PUT {}", entry)
      Using.resource(getInputStream(entry)) { istream =>
        s3.getClient.putObject(req, RequestBody.fromInputStream(istream, entry.getContentLength))
      }
      logger.info("S3PUT end {}", entry)
    }
  }

  private def deleteObject(entry: CdnEntry): Future[Unit] = {
    require(
      !entry.getKey.startsWith(s"${rootInBucket}/"),
      s"Key must not start with S3 paths prefix (${rootInBucket}): ${entry.getKey}"
    )
    require(entry.isInstanceOf[S3CdnEntry], s"It is only possible to remove S3 entries, but was: $entry")
    val req = DeleteObjectRequest.builder()
      .bucket(s3.getBucket)
      .key(keyInBucket(entry))
      .build()
    Future {
      logger.info("S3DEL {}", entry)
      s3.getClient.deleteObject(req)
    }
  }

  private def keyInBucket(entry: CdnEntry): String = {
    if (rootInBucket.isBlank) entry.getKey
    else s"${rootInBucket}/${entry.getKey}"
  }

  private def getInputStream(entry: CdnUploadEntry): InputStream = entry match {
    case de: CdnUploadDataEntry =>
      new ByteArrayInputStream(de.getContent)
    case ae: CdnUploadAnotherEntry =>
      ae.getItem match {
        case l: LocalDiskEntry => Files.newInputStream(l.getPath)
      }
  }

}
