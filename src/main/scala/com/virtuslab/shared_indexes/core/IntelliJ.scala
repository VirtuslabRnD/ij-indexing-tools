package com.virtuslab.shared_indexes.core

import com.virtuslab.shared_indexes.core.IntelliJ.logger
import org.slf4j.LoggerFactory

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
      .inheritIO()
      .start()

    val code = process.waitFor() // TODO: timeout

    IntelliJResult(code)
  }
}

object IntelliJ {
  private val logger = LoggerFactory.getLogger(this.getClass)
}
