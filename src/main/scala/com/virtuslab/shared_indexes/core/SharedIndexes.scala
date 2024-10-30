package com.virtuslab.shared_indexes.core

object SharedIndexes {

  /** Calls IntelliJ internal app to generate shared indexes for JDKs.
    *
    * To learn about available flags and how they work, start from
    * com.intellij.indexing.shared.generator.DumpSharedIndexCommand interface and check its implementations.
    */
  def dumpSharedIndex(
      ide: IntelliJ,
      workspace: Workspace,
      output: os.Path,
      subcommand: String,
      args: Seq[String]
  ): Unit = {
    val command = Seq(
      "dump-shared-index",
      subcommand,
      s"--output=$output",
      "--compression=xz"
    ) ++ args
    val result = ide.run(command, workspace.intelliJRunDir)
    if (result.exitCode != 0) {
      throw new RuntimeException(s"IntelliJ exited with code ${result.exitCode}")
    }
  }

}
