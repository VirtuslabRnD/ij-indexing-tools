package com.virtuslab.shared_indexes.util

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

object FileUtils {
  def listDirs(path: Path): Seq[Path] = {
    if (Files.exists(path)) {
      Files.list(path).filter(Files.isDirectory(_)).toList.asScala.toSeq
    } else {
      Nil
    }
  }

  def listFiles(path: Path): Seq[Path] = {
    Files.list(path).filter(Files.isRegularFile(_)).toList.asScala.toSeq
  }

  def name(path: Path): String = {
    path.getFileName.toString
  }

  def lastModified(path: Path): Long = {
    Files.getLastModifiedTime(path).toMillis
  }

  def deleteDir(path: Path): Unit = {
    os.remove.all(os.Path(path))
  }

  def extension(path: Path): String = {
    name(path).split('.').last
  }
}
