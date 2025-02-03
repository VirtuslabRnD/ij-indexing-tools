package com.virtuslab.shared_indexes.demo

import java.nio.file.Path

object ProjectLocator {
  val exampleProjectHome: Path = (RepositoryLocator.findRepoRoot() / "examples" / "full-project").toNIO
}
