package com.virtuslab.shared_indexes.kinds.project

import com.virtuslab.shared_indexes.core.storage.{FileSystemSharedIndexStorage, SharedIndexStorage}
import com.virtuslab.shared_indexes.util.{FileUtils, Logging}

import java.nio.file.Path

class ProjectSharedIndexCleanup(indexesToKeep: Int, projectName: String) extends Logging {

  def cleanup(storage: SharedIndexStorage): Unit = {
    storage match {
      case storage: FileSystemSharedIndexStorage => cleanupOldEntries(storage.uploadPath)
      case _ => logger.warn("Cleanup is only supported for FileSystemExternalStorage for now")
    }
  }

  private def cleanupOldEntries(uploadPath: Path): Unit = {
    val commitDirs = FileUtils.listDirs(uploadPath.resolve(s"data/project/$projectName"))

    val indexesToKeepIncludingCurrentlyGenerated = indexesToKeep - 1

    if (commitDirs.size > indexesToKeepIncludingCurrentlyGenerated) {
      val dirsWithModifiedDate =
        commitDirs.map(dir =>
          dir -> FileUtils.listFiles(dir).find(FileUtils.name(_).contains(".ijx")).map(FileUtils.lastModified)
        )

      dirsWithModifiedDate.collect { case (invalidDir, None) =>
        logger.info(s"Removing invalid directory $invalidDir (no ijx file inside)")
        FileUtils.deleteDir(invalidDir)
      }

      val validDirsFromOldest = dirsWithModifiedDate
        .collect { case (validDir, Some(timestamp)) => validDir -> timestamp }
        .sortBy { case (_, timestamp) => timestamp }
        .map { case (dir, _) => dir }

      if (validDirsFromOldest.size > indexesToKeepIncludingCurrentlyGenerated) {
        val dirCountToRemove = validDirsFromOldest.size - indexesToKeepIncludingCurrentlyGenerated
        validDirsFromOldest.take(dirCountToRemove).foreach { path =>
          logger.info(s"Removing old index $path as it exceeds the limit of $indexesToKeep most recent indexes to keep")
          FileUtils.deleteDir(path)
        }
      }
    }
  }

}
