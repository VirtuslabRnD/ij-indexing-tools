package com.virtuslab.shared_indexes.locator

import scala.annotation.tailrec

private[locator] object RepositoryLocator {
  @tailrec
  def findRepoRoot(dir: os.Path = os.pwd): os.Path = {
    if (os.exists(dir / ".git")) {
      dir
    } else {
      if (dir == os.root) {
        throw new IllegalStateException("Could not find repository root")
      } else {
        findRepoRoot(dir / os.up)
      }
    }
  }
}
