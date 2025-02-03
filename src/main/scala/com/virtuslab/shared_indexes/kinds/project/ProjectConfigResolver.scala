package com.virtuslab.shared_indexes.kinds.project

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.Path

object ProjectConfigResolver {
  def getGitCommit(projectHome: Path): String = {
    val process = new ProcessBuilder("git", "rev-parse", "HEAD")
      .directory(projectHome.toFile)
      .start()

    def getProcessOutput(is: InputStream): String = {
      val bis = new BufferedInputStream(is)
      new String(bis.readAllBytes()).trim
    }

    if (process.waitFor() != 0) {
      throw new RuntimeException(
        s"""Commit can not be read in $projectHome
           |Output: ${getProcessOutput(process.getInputStream)}
           |Error: ${getProcessOutput(process.getErrorStream)}
           |""".stripMargin
      )
    } else {
      getProcessOutput(process.getInputStream)
    }
  }
}
