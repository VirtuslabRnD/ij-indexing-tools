package com.virtuslab.shared_indexes.core.storage

import java.nio.file.Path

trait SharedIndexStorage {
  def store(generatedIndexesDir: Path, debug: Boolean): Unit
}