package com.virtuslab.shared_indexes.config

case class IndexInputConfig(
    inputs: Seq[os.Path] = Seq.empty,
    commit: Option[String] = None
)
