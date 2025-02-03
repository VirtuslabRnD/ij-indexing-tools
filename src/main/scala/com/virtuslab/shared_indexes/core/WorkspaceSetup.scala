package com.virtuslab.shared_indexes.core

import java.nio.file.{Path, Paths}

trait WorkspaceSetup {
  def setup(): Path
}

object CwdWorkspace extends WorkspaceSetup {
  override def setup(): Path = Paths.get(".").toAbsolutePath
}
