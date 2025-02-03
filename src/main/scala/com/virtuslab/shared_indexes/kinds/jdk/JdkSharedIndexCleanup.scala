package com.virtuslab.shared_indexes.kinds.jdk

import com.virtuslab.shared_indexes.core.storage.{FileSystemSharedIndexStorage, SharedIndexStorage}
import com.virtuslab.shared_indexes.util.{FileUtils, Logging}

import java.nio.file.Path

private[jdk] class JdkSharedIndexCleanup(regenerate: Boolean) extends Logging {

  def cleanup(storage: SharedIndexStorage): Unit = {
    if (regenerate) {
      storage match {
        case storage: FileSystemSharedIndexStorage => cleanupOldEntries(storage.uploadPath)
        case _ => logger.warn("Cleanup is only supported for FileSystemExternalStorage for now")
      }
    }
  }

  private def cleanupOldEntries(uploadPath: Path): Unit = {
    val jdkDirs = FileUtils.listDirs(uploadPath.resolve("data/jdk"))
    jdkDirs.foreach { path =>
      logger.info(s"Removing old jdk index $path as regeneration was requested")
      FileUtils.deleteDir(path)
    }
  }
}
