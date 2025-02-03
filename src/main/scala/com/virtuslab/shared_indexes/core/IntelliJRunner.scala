package com.virtuslab.shared_indexes.core

import java.nio.file.Path

final case class IntelliJResult(exitCode: Int)

trait IntelliJRunner {
  def run(workspace: Path, args: Seq[String]): IntelliJResult
}
