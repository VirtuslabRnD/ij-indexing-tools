package com.virtuslab.shared_indexes.config

import mainargs.Flag

// These flags are needed because mainargs doesn't support nested subcommands
// See https://github.com/com-lihaoyi/mainargs/issues/57
case class JarIndexesConfig(
    upload: Flag,
    download: Flag
)
