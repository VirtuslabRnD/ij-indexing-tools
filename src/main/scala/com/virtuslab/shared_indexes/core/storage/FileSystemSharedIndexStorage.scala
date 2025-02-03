package com.virtuslab.shared_indexes.core.storage

import com.intellij.indexing.shared.cdn._
import com.intellij.indexing.shared.local.{Local_contextKt, Local_snapshotKt, Local_uploadKt}

import java.nio.file.Path

class FileSystemSharedIndexStorage(
    serverUrl: String,
    val uploadPath: Path
) extends CdnBasedIndexStorage {

  protected def createContext(): CdnContext = {
    Local_contextKt.createContext(serverUrl)
  }

  protected def listExistingIndexes(): JList[CdnEntry] = {
    Local_snapshotKt.listLocalIndexes(uploadPath, uploadPath)
  }

  protected def executeUpdatePlan(plan: CdnUpdatePlan): Unit = {
    Local_uploadKt.updateLocalIndexes(uploadPath, plan)
  }

  override def toString: String = s"FileStorage(path=$uploadPath, url=$serverUrl)"
}
