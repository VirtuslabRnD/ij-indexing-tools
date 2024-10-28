package com.virtuslab.shared_indexes.core

import com.virtuslab.shared_indexes.core.IntelliJ.logger
import com.virtuslab.shared_indexes.logging.TaskStatus
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, InputStreamReader}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class IntelliJResult(exitCode: Int)

/** Class representing the IntelliJ installation. We probably will not need to deeply configure the instance.
  *
  * @param binary
  *   path to a binary file that runs IntelliJ, for example `idea.sh`
  * @param cacheDir
  *   path to a directory where IntelliJ keeps its cache, it is OS specific and can be overriden with properties. We
  *   might want to have some logic to resolve it from properties.
  */
class IntelliJ(
    val binary: os.Path,
    val cacheDir: os.Path
) {

  /** This is a path from which IntelliJ automatically loads shared indexes, at least for the project and jars (doesn't
    * work for JDK). Useful for quick testing without server setup.
    */
  val sharedIndexDir: os.Path = {
    val path = cacheDir / "shared-index"
    os.makeDir.all(path)
    path
  }

  /** Simply runs IntelliJ, streaming the output
    */
  def run(args: Seq[String], workingDir: os.Path): IntelliJResult = {
    val command = Seq(binary.toString) ++ args
    logger.info(command.mkString(" "))
    val process = new ProcessBuilder()
      .directory(workingDir.toIO)
      .command(command: _*)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start()

    def streamOutput(inputStream: java.io.InputStream, isError: Boolean): Future[Unit] = Future {
      val reader = new BufferedReader(new InputStreamReader(inputStream))
      var line: String = null
      val isStatusEnabled = !isError && !logger.isDebugEnabled()
      val taskStatus = TaskStatus("Generating shared indexes", isStatusEnabled)
      taskStatus.start()
      while ({ line = reader.readLine(); line != null }) {
        if (isError)
          logger.debug(line)
        else
          logger.debug(line)
          taskStatus.updateSpinner(line)

      }
      taskStatus.done()
    }(scala.concurrent.ExecutionContext.global)

    val stdoutFuture = streamOutput(process.getInputStream, isError = false)
    val stderrFuture = streamOutput(process.getErrorStream, isError = true)

    val code = process.waitFor() // TODO: timeout

    Await.result(stderrFuture, Duration.Inf)
    Await.result(stdoutFuture, Duration.Inf)

    IntelliJResult(code)
  }
}

object IntelliJ {
  private val logger = LoggerFactory.getLogger(this.getClass)
}
