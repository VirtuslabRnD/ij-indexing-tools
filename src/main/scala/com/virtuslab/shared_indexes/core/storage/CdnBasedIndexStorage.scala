package com.virtuslab.shared_indexes.core.storage

import com.intellij.indexing.shared.cdn.rebuild.CdnRebuildKt.rebuildCdnLayout
import com.intellij.indexing.shared.cdn.upload.CdnUploadKt.uploadNewEntries
import com.intellij.indexing.shared.cdn.{CdnContext, CdnEntry, CdnUpdatePlan}
import com.intellij.indexing.shared.local.Local_snapshotKt
import com.virtuslab.shared_indexes.util.Logging

import java.io.{OutputStream, PrintStream}
import java.nio.file.Path


trait CdnBasedIndexStorage extends SharedIndexStorage with Logging {

  protected type JList[A] = java.util.List[A]

  protected def listExistingIndexes(): JList[CdnEntry]
  protected def executeUpdatePlan(plan: CdnUpdatePlan): Unit
  protected def createContext(): CdnContext

  def store(generatedIndexesDir: Path, debug: Boolean): Unit = {
    logger.info(s"Generating index metadata and uploading to $this")

    // The third-party code below calls 'println'
    // To keep the output clean we need to swap out the standard output
    val oldStdOut = System.out
    if (debug) System.setOut(new PrintStream(OutputStream.nullOutputStream()))
    try {
      val ctx = createContext()

      val localIndexes = findLocalIndexes(generatedIndexesDir)
      val remoteIndexes = listExistingIndexes()
      val newEntries = uploadNewEntries(ctx, localIndexes, remoteIndexes)
      executeUpdatePlan(newEntries)

      val replaceMetadata = rebuildCdnLayout(ctx, listExistingIndexes())
      executeUpdatePlan(replaceMetadata)
    } finally System.setOut(oldStdOut)

    logger.info(s"Done uploading to $this")
  }

  private def findLocalIndexes(p: Path): java.util.List[CdnEntry] = {
    // first parameter is where we look for index files, second is the base path that they
    // should be relative to
    Local_snapshotKt.listLocalIndexes(p, p)
  }
}
