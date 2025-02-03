package com.virtuslab.shared_indexes.core

import com.virtuslab.shared_indexes.util.Logging

import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.Path
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class DefaultIntelliJRunner(binary: String) extends IntelliJRunner with Logging {

  override def run(workspace: Path, args: Seq[String]): IntelliJResult = {
    val command = Seq(binary) ++ args
    logger.info(command.mkString(" "))
    val process = new ProcessBuilder()
      .directory(workspace.toFile)
      .command(command: _*)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start()

    def streamOutput(inputStream: java.io.InputStream, isError: Boolean): Future[Unit] = Future {
      val reader = new BufferedReader(new InputStreamReader(inputStream))
      var line: String = null
      while ({ line = reader.readLine(); line != null }) {
        logger.info(line)
      }
    }(scala.concurrent.ExecutionContext.global)

    val stdoutFuture = streamOutput(process.getInputStream, isError = false)
    val stderrFuture = streamOutput(process.getErrorStream, isError = true)

    val code = process.waitFor()

    Await.result(stderrFuture, Duration.Inf)
    Await.result(stdoutFuture, Duration.Inf)

    IntelliJResult(code)
  }
}
