package com.virtuslab.shared_indexes.locator

import org.slf4j.LoggerFactory
import os.Path

object ProjectLocator {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val exampleProjectHome: Path = RepositoryLocator.findRepoRoot() / "examples" / "full-project"

  def getGitCommit(projectHome: os.Path): String = {
    val process = os.proc("git", "rev-parse", "HEAD")
      .call(cwd = projectHome, stderr = os.Pipe)

      new ProcessBuilder()
      .directory(projectHome.toIO)
      .command("git", "rev-parse", "HEAD")
      .start()

    if (process.exitCode != 0) {
      logger.error(
        s"""Commit can not be read in $projectHome
             |Output: ${process.out.text().trim}
             |Error: ${process.err.text().trim}
             |""".stripMargin
      )

      sys.exit(1)
    } else {
      process.out.text().trim
    }
  }

}
