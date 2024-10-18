package com.virtuslab.shared_indexes.config

import os.Path

case class GeneratorConfig(
    ideaBinary: Path = os.home / "Applications" / "IntelliJ IDEA Ultimate.app" / "Contents" / "MacOS" / "idea",
    ideaCacheDir: Path = os.home / "Library" / "Caches" / "JetBrains" / "IntelliJIdea2023.3",
    workDir: Path = os.pwd / "workspace"
)
