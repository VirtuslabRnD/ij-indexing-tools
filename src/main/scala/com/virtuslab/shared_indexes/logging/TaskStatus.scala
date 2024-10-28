package com.virtuslab.shared_indexes.logging

case class TaskStatus(status: String, enabled: Boolean) {
  private def statusText(newState: String) = s"\r$status: $newState"

  def start(): Unit = if (enabled) print(statusText("..."))

  def updateSpinner(newState: String): Unit = if (enabled) print(statusText(newState))

  def done(): Unit = if (enabled) println(statusText("DONE"))
}
