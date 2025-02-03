package com.virtuslab.shared_indexes.kinds.jdk

import org.slf4j.LoggerFactory

import java.nio.file.{Files, Path}
import scala.util.{Failure, Success, Try}

// We generate aliases for JDK here to avoid issues with hashing described below.
// As aliases for JDKs installed without IntelliJ, Shared Index plugin only uses
// the feature version number like 1.8, 11 or 17. I added also aliases with more
// precise version like 11.1.0.11, 11.1.0 or 11.1 and for java < 9 I added version
// 8 (additionally to 1.8) just in case the logic changes, or we need something more
// precise. The aliases that include the distribution like openjdk-11 are only used
// by IJ for querying indexes for JDKs installed from IntelliJ. These extra aliases
// are read from `~/Library/Java/JavaVirtualMachines/.*.intellij` files which are
// created during JDK download, using the data provided in JB servers.
// If we ever find that precise hash is too specific and version number is
// too general, we will need to update generation logic to create aliases for distribution
// and trick IntelliJ into using them by creating .intellij files for our JDKs or in some
// other way.

// Context:
// At some point I had only jdk 8, 11 and 17 are working. JdkIndexLookupRequest
// has hash field populated for these jdks. For other JDKs it was set to null.
// The generated indexes from this script are only indexed by hash. Compare
// it to publicly available jdk index from the default url and see that it
// also contains aliases with version number.
// I managed to resolve it by clearing hashing cache which is located at
// ~/Library/Caches/JetBrains/IntelliJIdea2023.3/workspace/app.xml (or some other xml
// in that dir). I cleared entries in component `shared-index-jdk-hash` and
// it repopulated with hashes and shared indexes started to fetch.
private[jdk] object JdkAliases {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def resolve(path: Path): Seq[String] = {
    resolveVersion(path).map(aliasesFromVersion) match {
      case Failure(exception) =>
        logger.error(s"Could not resolve JDK $path: ", exception)
        Nil
      case Success(value) => value
    }
  }

  private def resolveVersion(path: Path): Try[String] = {
    val releaseFile = path.resolve("release")
    val versionFile = path.resolve("version.txt")

    if (Files.exists(releaseFile)) {
      Try {
        val properties = new java.util.Properties()
        val reader = Files.newInputStream(releaseFile)
        try properties.load(reader)
        finally reader.close()
        Option(properties.getProperty("JAVA_VERSION"))
          .map(_.replaceAll("^\"|\"$", ""))
          .getOrElse(throw new RuntimeException(s"Could not locate JAVA_VERSION in $releaseFile"))
      }
    } else if (Files.exists(versionFile)) {
      Try(Files.readString(versionFile).trim)
    } else {
      Failure(new RuntimeException(s"Could not locate version file"))
    }
  }

  private def aliasesFromVersion(version: String): Seq[String] = {
    val parts = version.split("\\.")
    val partsHandlingPreJava9 = if (parts(0) == "8") "1" +: parts else parts
    val preJava9Alias = if (version.startsWith("1.")) Seq(partsHandlingPreJava9(1)) else Seq.empty

    val versionPrefixes = partsHandlingPreJava9.inits
      .filter(xs => xs.length > 0 && xs.length <= 3)
      .map(_.mkString("."))
      .filterNot(_ == "1")
      .toSeq

    (versionPrefixes ++ preJava9Alias).distinct
  }
}
