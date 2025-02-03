package com.virtuslab.shared_indexes.kinds.project

import com.virtuslab.shared_indexes.core.storage.SharedIndexStorage
import com.virtuslab.shared_indexes.core.{IntelliJRunner, SharedIndexesService, WorkspaceSetup}
import com.virtuslab.shared_indexes.util.FileUtils

import java.nio.file.Path

class ProjectIndexesService(
    outputsDir: Path,
    ij: IntelliJRunner,
    ws: WorkspaceSetup,
    projectRoot: Path,
    resolveProjectName: Option[Path => String],
    debug: Boolean,
    indexesToKeep: Int
) extends SharedIndexesService("project", outputsDir, ij, ws, debug) {

  private lazy val absoluteProjectRoot =
    if (projectRoot.isAbsolute) projectRoot else workspace.resolve(projectRoot).normalize()

  private lazy val projectName = resolveProjectName.getOrElse(FileUtils.name _)(absoluteProjectRoot)

  override protected def store(generatedIndexesDir: Path, storage: SharedIndexStorage): Unit = {
    new ProjectSharedIndexCleanup(indexesToKeep, projectName).cleanup(storage)
    super.store(generatedIndexesDir, storage)
  }

  override protected def executeGeneration(): Unit = {
    val commit = ProjectConfigResolver.getGitCommit(absoluteProjectRoot)
    dumpSharedIndex(
      args = Seq(
        s"--commit=$commit",
        s"--project-id=$projectName",
        s"--project-dir=$absoluteProjectRoot"
      )
    )
  }
}
