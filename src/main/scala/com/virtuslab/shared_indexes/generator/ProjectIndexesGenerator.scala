package com.virtuslab.shared_indexes.generator

import com.virtuslab.shared_indexes.core.{IntelliJ, SharedIndexes, Workspace}
import com.virtuslab.shared_indexes.locator.ProjectLocator
import os.Path

class ProjectIndexesGenerator(
    intelliJ: IntelliJ,
    workspace: Workspace,
    projectRootOpt: Option[os.Path],
    commitOpt: Option[String]
) extends SharedIndexesGenerator[(Path, String)](intelliJ, workspace) {

  override protected val cacheDir: Path = workspace.projectIndexes

  override protected def findInputs(): (Path, String) = {
    val projectRoot = projectRootOpt.getOrElse(ProjectLocator.exampleProjectHome)
    val commit = commitOpt.getOrElse(ProjectLocator.getGitCommit(projectRoot))

    logger.info(s"Found project at $projectRoot with HEAD $commit")
    (projectRoot, commit)
  }

  override protected def process(inputs: (Path, String)): Unit = {
    val (projectRoot, commit) = inputs
    dumpProjectSharedIndex(intelliJ, projectRoot, commit, workspace)
  }

  private def dumpProjectSharedIndex(
      ide: IntelliJ,
      projectHome: os.Path,
      commit: String,
      workspace: Workspace
  ): Unit = {
    SharedIndexes.dumpSharedIndex(
      ide = ide,
      workspace = workspace,
      output = workspace.projectIndexes,
      subcommand = "project",
      args = Seq(
        s"--commit=$commit",
        s"--project-id=${projectHome.baseName}",
        s"--project-dir=$projectHome"
      )
    )
  }

}
