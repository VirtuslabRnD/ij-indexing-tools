package com.virtuslab.shared_indexes.locator

object JarLocator {

  def exampleDepJars(): Seq[os.Path] =
    os.proc(Seq("./gradlew", "--no-daemon", "-q", "listJars"))
      .call(cwd = RepositoryLocator.findRepoRoot() / "examples" / "multi-jar")
      .out.lines().map(os.Path(_))
      .toList

  def findSbtDepJars(projectRoot: os.Path): Seq[os.Path] = {
    val depPathPrefix = "[info] *"
    os.proc("sbt", "show dependencyClasspathFiles")
      .call(cwd = projectRoot)
      .out.lines()
      .filter(_.startsWith(depPathPrefix))
      .map(_.stripPrefix(depPathPrefix).trim)
      .map(os.Path(_))
  }

}
