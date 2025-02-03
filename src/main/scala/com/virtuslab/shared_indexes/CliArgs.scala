package com.virtuslab.shared_indexes
import com.virtuslab.shared_indexes.core.storage.{FileSystemSharedIndexStorage, LocalIntelliJStorage, S3SharedIndexStorage, SharedIndexStorage}
import org.kohsuke.args4j
import org.kohsuke.args4j.spi.{PathOptionHandler, StringArrayOptionHandler}

import java.nio.file.{Files, Path, Paths}

object SharedIndexKind extends Enumeration {
  val Jdk = Value("jdk")
  val Project = Value("project")
  val Jars = Value("jars")

  def fromString(s: String): SharedIndexKind.Value = {
    s match {
      case "jdk"     => Jdk
      case "project" => Project
      case "jars"    => Jars
      case _ => throw new IllegalArgumentException(s"Unknown shared index kind: $s. Allowed values: jdk, project, jars")
    }
  }
}

class CliArgs {
  @args4j.Option(
    name = "--ij-binary",
    usage =
      "IntelliJ binary name or absolute path to the binary, like idea.sh/ Example (macos): \"/Users/$USER/Applications/IntelliJ IDEA Ultimate 2023.3.8.app/Contents/MacOS/idea\""
  )
  var ijBinary: String = "idea"

  @args4j.Option(
    name = "--local-output",
    usage = "Path to a directory where indexes will be saved before being moved to the server",
    handler = classOf[PathOptionHandler]
  )
  var localOutput: Path = Paths.get(sys.props("java.io.tmpdir"), "shared-indexes")

  @args4j.Option(
    name = "--file-server-storage",
    usage = "Path to store shared indexes to be hosted by file server",
    handler = classOf[StringArrayOptionHandler]
  )
  private var fileServerStorage: Array[String] = Array.empty[String]

  @args4j.Option(
    name = "--file-server-url",
    usage = "File server url where indexes will be hosted",
    handler = classOf[StringArrayOptionHandler]
  )
  private var serverUrl: Array[String] = Array.empty[String]

  @args4j.Option(
    name = "--s3-bucket",
    usage = "Name of s3 bucket where indexes will be uploaded",
    handler = classOf[StringArrayOptionHandler]
  )
  private var s3Bucket: Array[String] = Array.empty[String]

  @args4j.Option(
    name = "--s3-url",
    usage = "Url to s3 api where indexes should be uploaded (without bucket name)",
    handler = classOf[StringArrayOptionHandler]
  )
  private var s3Url: Array[String] = Array.empty[String]

  @args4j.Option(
    name = "--intellij-cache-dir",
    usage =
      "IntelliJ cache directory location to extract indexes to for direct use. Example (macos): /Users/$USER/Library/Caches/JetBrains/IntelliJIdea2023.3",
    handler = classOf[StringArrayOptionHandler]
  )
  private var intelliJCacheDir: Array[String] = Array.empty[String]

  lazy val uploadLocations: Seq[SharedIndexStorage] = {
    if (serverUrl.length != fileServerStorage.length) {
      throw new RuntimeException("Provide one server url per file storage in matching order")
    }

    val fsStorages =
      serverUrl.zip(fileServerStorage.map(Paths.get(_))).map((new FileSystemSharedIndexStorage(_, _)).tupled)

    if (s3Bucket.length != s3Url.length) {
      throw new RuntimeException("Provide one s3 server url per bucket in matching order")
    }

    val s3Storages = s3Url.zip(s3Bucket).map((new S3SharedIndexStorage(_, _)).tupled)

    val localStorages = intelliJCacheDir.map(Paths.get(_)).map(new LocalIntelliJStorage(_))

    fsStorages ++ s3Storages ++ localStorages
  }

  @args4j.Option(
    name = "--inputs",
    usage = "Inputs to the indexing process, e.g. path to a project, paths to jars, paths to jdks",
    required = false,
    handler = classOf[StringArrayOptionHandler]
  )
  private var _inputs: Array[String] = _
  lazy val inputs: Seq[Path] = {
    val inputs = Option(_inputs).map(_.toSeq).getOrElse(Nil).map(Paths.get(_))
    val missingPaths = inputs.filter(p => p.isAbsolute && !Files.exists(p))
    if (missingPaths.nonEmpty) {
      throw new RuntimeException(s"Following input paths do not exist:\n${missingPaths.mkString("\n")}")
    }
    inputs
  }

  @args4j.Option(
    name = "--kind",
    usage = "Kind of indexes to generate. One of: jdk, project, jars",
    required = true
  )
  private var _kind: String = _
  lazy val kind: SharedIndexKind.Value = SharedIndexKind.fromString(_kind)

  @args4j.Option(
    name = "--indexes-to-keep",
    usage = "Number of most recent project indexes to keep on the server"
  )
  var indexesToKeep: Int = 5

  @args4j.Option(
    name = "--project-name",
    usage = "Use this project name instead of the one inferred from the project path"
  )
  private var _projectName: String = _
  lazy val projectName: Option[String] = Option(_projectName)

  @args4j.Option(
    name = "--debug",
    usage = "Get debug logs"
  )
  var debug: Boolean = false

  @args4j.Option(
    name = "--regenerate",
    usage = "Remove all existing indexes for this kind and generate them again. Currently only works with jdk kind"
  )
  var regenerate: Boolean = false

  @args4j.Option(
    name = "--demo",
    usage = "Use demo mode i.e. generate indexes for example projects, auto detected jdks, etc."
  )
  var demo: Boolean = false

}
