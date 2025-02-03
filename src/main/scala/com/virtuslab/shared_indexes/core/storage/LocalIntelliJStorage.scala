package com.virtuslab.shared_indexes.core.storage

import com.virtuslab.shared_indexes.util.{FileUtils, Logging}
import org.tukaani.xz.XZInputStream

import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.zip.GZIPInputStream

// Useful for testing without the server and works with Project Shared Indexes.
class LocalIntelliJStorage(cacheDir: Path, removeExisting: Boolean = true) extends SharedIndexStorage with Logging {
  private val targetDir = cacheDir.resolve("shared-index")

  override def store(generatedIndexesDir: Path, debug: Boolean): Unit = {
    if (removeExisting) {
      FileUtils.listFiles(targetDir).foreach(Files.deleteIfExists)
      FileUtils.listDirs(targetDir).foreach(FileUtils.deleteDir)
    }

    FileUtils.listFiles(generatedIndexesDir).foreach { file =>
      val name = FileUtils.name(file)
      if (name.endsWith("ijx.xz")) {
        val input = new XZInputStream(Files.newInputStream(file))
        val output = targetDir.resolve(name.replace(".ijx.xz", ".ijx"))
        try {
          Files.copy(file, output, StandardCopyOption.REPLACE_EXISTING)
        } finally input.close()
      } else if (name.endsWith("ijx.gz")) {
        val input = new GZIPInputStream(Files.newInputStream(file))
        val output = targetDir.resolve(name.replace(".ijx.gz", ".ijx"))
        try {
          Files.copy(file, output, StandardCopyOption.REPLACE_EXISTING)
        } finally input.close()
      } else if (name.endsWith("ijx")) {
        Files.copy(file, targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING)
      }
    }
    logger.info(s"Done uploading to $this")
  }

  override def toString: String = s"LocalIntelliJStorage($targetDir)"
}
