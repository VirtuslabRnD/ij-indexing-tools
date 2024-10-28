package com.virtuslab.shared_indexes.logging

import java.io.{OutputStream, PrintStream}

// TODO this is not perfect...
case class LoggingPrintStream(loggingFun: String => Unit) extends PrintStream(OutputStream.nullOutputStream()) {

  override def print(s: String): Unit = loggingFun(s)
}
