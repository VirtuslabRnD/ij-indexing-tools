package com.virtuslab.shared_indexes.config

case class IndexInputConfig(
    artifactPaths: Seq[os.Path] = Seq.empty,
    projectRoot: Option[os.Path] = None,
    commit: Option[String] = None
)
