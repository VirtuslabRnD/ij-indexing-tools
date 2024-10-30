package com.virtuslab.shared_indexes.config

import mainargs.{ParserForClass, TokensReader}
import os.{FilePath, Path, RelPath, SubPath}
import mainargs.TokensReader.{OptionRead, Simple, tryEither}
import org.slf4j.event.Level

case class MainConfig(
    generatorConfig: GeneratorConfig,
    indexStorageConfig: IndexStorageConfig,
    indexInputConfig: IndexInputConfig,
    s3Config: S3Config,
    loggingConfig: LoggingConfig,
    jarIndexesConfig: JarIndexesConfig
)

object MainConfig {
  private object PathReader extends Simple[Path] {
    override def shortName: String = "path"
    override def read(strs: Seq[String]) = tryEither {
      stringToPath(strs.last)
    }
  }

  private object PathsReader extends Simple[Seq[os.Path]] {
    override def shortName: String = "paths"
    override def read(strs: Seq[String]) = tryEither {
      strs.map(stringToPath)
    }
  }

  private def stringToPath(str: String) = {
    FilePath(str) match {
      case p: Path => p
      case r: RelPath => os.pwd / r
      case s: SubPath => os.pwd / s
    }
  }

  private object LogLevelReader extends Simple[Level] {
    override def shortName: String = "level"
    override def read(strs: Seq[String]) = tryEither {
      Level.valueOf(strs.last.toUpperCase)
    }
  }

  private implicit val pathReader: TokensReader[os.Path] = PathReader
  private implicit val pathSeqReader: TokensReader[Seq[os.Path]] = PathsReader
  private implicit val pathOptionReader: TokensReader[Option[os.Path]] = OptionRead(PathReader)
  private implicit val logLevelReader: TokensReader[Level] = LogLevelReader
  private implicit val generatorConfigReader: TokensReader[GeneratorConfig] = ParserForClass[GeneratorConfig]
  private implicit val indexStorageConfigReader: TokensReader[IndexStorageConfig] = ParserForClass[IndexStorageConfig]
  private implicit val indexInputConfigReader: TokensReader[IndexInputConfig] = ParserForClass[IndexInputConfig]
  private implicit val s3ConfigReader: TokensReader[S3Config] = ParserForClass[S3Config]
  private implicit val loggingConfigReader: TokensReader[LoggingConfig] = ParserForClass[LoggingConfig]
  private implicit val jarIndexesConfigReader: TokensReader[JarIndexesConfig] = ParserForClass[JarIndexesConfig]
  implicit val parser: ParserForClass[MainConfig] = ParserForClass[MainConfig]
}
