package com.virtuslab.shared_indexes.demo

import com.virtuslab.shared_indexes.util.Logging

import java.nio.file.{Files, Path, Paths}

object JarLocator extends Logging {

  def findExampleJars(inputs: Seq[Path]): Seq[Path] = {
    inputs match {
      case Seq() =>
        logger.info("No inputs specified, using example jars")
        JarLocator.exampleDepJars()
      case Seq(path) if Files.isDirectory(path) =>
        logger.info(s"Input is a directory, using sbt to resolve all dependency jars")
        JarLocator.findSbtDepJars(path)
      case paths =>
        logger.info(s"Using specified inputs as jars (${paths.size})")
        paths
    }
  }

  private def exampleDepJars(): Seq[Path] = {
    val cmd = Seq("./gradlew", "--no-daemon", "-q", "listJars")
    val cwd = RepositoryLocator.findRepoRoot() / "examples" / "multi-jar"
    logger.info(s"Querying gradle for jars - running ${cmd.mkString(" ")} in $cwd. It may take some time...")
    os.proc(cmd)
      .call(cwd = cwd)
      .out.lines().map(Paths.get(_))
      .toList
  }

  private def findSbtDepJars(projectRoot: Path): Seq[Path] = {
    val depPathPrefix = "[info] *"
    os.proc("sbt", "show dependencyClasspathFiles")
      .call(cwd = os.Path(projectRoot))
      .out.lines()
      .filter(_.startsWith(depPathPrefix))
      .map(_.stripPrefix(depPathPrefix).trim)
      .map(Paths.get(_))
  }

}
