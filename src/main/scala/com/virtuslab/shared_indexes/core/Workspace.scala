package com.virtuslab.shared_indexes.core

import os.Path

/**
 * Collects all paths that are used by the indexing process.
 * It might need to evolve as the tool is developed. For now
 * it is convenient.
**/
class Workspace(val root: os.Path) {

  val generationTmp: os.Path = dir("generation-tmp")
  val intelliJRunDir: os.Path = dir("intellij-run-dir")
  val jdkIndexes: os.Path = dir("jdk-indexes")
  val jarIndexes: os.Path = dir("jar-indexes")
  val projectIndexes: os.Path = dir("project-indexes")
  val cdnPath: Path = dir("cdn")

  private def dir(subpath: String): os.Path = {
    val path = root / subpath
    os.makeDir.all(path)
    path
  }
}
