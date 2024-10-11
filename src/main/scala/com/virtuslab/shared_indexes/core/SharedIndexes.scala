package com.virtuslab.shared_indexes.core

object SharedIndexes {

  def dumpJdkSharedIndexes(
      ide: IntelliJ,
      jdkPath: os.Path,
      aliases: Seq[String],
      workspace: Workspace
  ): Unit = {
    dumpSharedIndex(
      ide,
      workspace,
      workspace.jdkIndexes,
      "jdk",
      args = Seq(
        s"--temp-dir=${workspace.generationTmp}",
        "--dump-project-roots", // TODO: let's see what it does
        s"--jdk-home=$jdkPath"
      ) ++ aliases.map(alias => s"--alias=$alias")
    )

  }

  def dumpJarSharedIndex(
      ide: IntelliJ,
      jarPaths: Seq[os.Path],
      workspace: Workspace,
      kind: String,
      chunkName: String
  ): Unit = {
    dumpSharedIndex(
      ide,
      workspace,
      workspace.jarIndexes,
      "jars",
      args = Seq(
        s"--kind=$kind",
        s"--chunk-name=$chunkName"
      ) ++ jarPaths.map(path => s"--jar=$path")
    )
  }

  def dumpProjectSharedIndex(
      ide: IntelliJ,
      projectHome: os.Path,
      commit: String,
      workspace: Workspace
  ): Unit = {
    dumpSharedIndex(
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

  /** Calls IntelliJ internal app to generate shared indexes for JDKs.
    *
    * To learn about available flags and how they work, start from
    * com.intellij.indexing.shared.generator.DumpSharedIndexCommand interface and check its implementations.
    */
  private def dumpSharedIndex(
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

  def key(jarPaths: Seq[os.Path]): String = {
    jarPaths.map(digest).hashCode().toHexString
  }

  private def digest(f: os.Path): Int = {
    // TODO implement fuzzy hashing
    f.last.hashCode
  }

  def bestMatch(availableKeys: Seq[String], key: String): Option[String] = {
    if (availableKeys contains key) Some(key) else None
  }

}
