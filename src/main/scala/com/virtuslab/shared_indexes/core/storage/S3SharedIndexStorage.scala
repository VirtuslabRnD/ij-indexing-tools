package com.virtuslab.shared_indexes.core.storage

import com.intellij.indexing.shared.cdn._
import kotlin.coroutines.{Continuation, EmptyCoroutineContext}
import kotlinx.coroutines.{BuildersKt, Dispatchers}

// To upload to S3, you need to provide access credentials.
// The easiest way is setting up these environment variables:
// AWS_ACCESS_KEY_ID=123456789
// AWS_SECRET_ACCESS_KEY=987654321
// (replace the values with valid credentials to your S3 service)
// For other ways to supply credentials, see
// com.amazonaws.auth.DefaultAWSCredentialsProviderChain
//
// You must make sure the S3 API is accessible and the bucket exists
class S3SharedIndexStorage(
    apiUrl: String,
    bucketName: String
) extends CdnBasedIndexStorage {

  val s3Api: S3 = S3Kt.S3(s"$apiUrl/$bucketName", bucketName, apiUrl, "/", 10)

  override protected def listExistingIndexes(): JList[CdnEntry] = {
    val list = runCoroutine[JList[_ <: CdnEntry]](S3_snapshotKt.listS3Indexes(s3Api, _))
    list.asInstanceOf[JList[CdnEntry]]
  }

  override protected def createContext(): CdnContext = {
    S3_contextKt.createContext(s3Api)
  }

  protected def executeUpdatePlan(plan: CdnUpdatePlan): Unit = {
    new S3Upload(s3Api).updateS3Indexes(plan)
  }

  private def runCoroutine[A](
      block: Continuation[_ >: A] => AnyRef
  ): A = {
    BuildersKt.runBlocking(
      Dispatchers.getDefault.plus(EmptyCoroutineContext.INSTANCE),
      (_, cont: Continuation[_ >: A]) => block(cont)
    )
  }

  override def toString: String = s"S3Storage(url=$apiUrl, bucket=$bucketName)"
}
