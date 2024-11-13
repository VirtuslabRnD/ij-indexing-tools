package com.virtuslab.shared_indexes.locator

object JarLocator {

  def exampleDepJars(): Seq[os.Path] =
    os.proc(Seq("./gradlew", "--no-daemon", "-q", "listJars"))
      .call(cwd = RepositoryLocator.findRepoRoot() / "examples" / "multi-jar")
      .out.lines().map(os.Path(_))
      .toList

  def findSbtDepJars(projectRoot: os.Path): Seq[os.Path] = {
    val infoPrefix = "[info] "
    val classPathListPrefix = "List("
    val classPathListSuffix = ")"
    os.proc("sbt", "show dependencyClasspathFiles")
      .call(cwd = projectRoot)
      .out.lines()
      .dropWhile(line => !line.startsWith("[info] Compile / dependencyClasspathFiles"))
      .filter(_.contains(classPathListPrefix))
      .map {
        _.stripPrefix(infoPrefix)
          .trim
          .stripPrefix(classPathListPrefix)
          .stripSuffix(classPathListSuffix)
      }
      .flatMap(_.split(","))
      .map(_.trim)
      .map(os.Path(_))
  }
}
