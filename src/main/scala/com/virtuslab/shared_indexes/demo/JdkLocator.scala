package com.virtuslab.shared_indexes.demo

import java.nio.file.Path

/** Locates installed JDKs for indexing by using gradle from the JDK indexing example project. JDKs have to be installed
  * manually, I used IntelliJ to do it. Gradle should be able to find JDKs installed with most methods. Note that it
  * will find all installed JDKs, not only the ones that are used for indexing.
  */
object JdkLocator {

  def findAllInstalledJdks(): List[Path] = {
    val gradleProcess = os.proc(
      Seq("./gradlew", "--no-daemon", "-q", "javaToolchains")
    ).call(cwd = RepositoryLocator.findRepoRoot() / "examples" / "multi-jdk")
    val output = gradleProcess.out.lines()

    val jdkLocationRegex = """^\s+\|\s+Location:\s+(.*)\s*$""".r

    output
      .collect { case jdkLocationRegex(path) => os.Path(path) }
      .filter(os.exists)
      .map(_.toNIO)
      .toList
  }
}
