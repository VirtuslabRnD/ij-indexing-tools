package com.virtuslab.shared_indexes

import com.virtuslab.shared_indexes.core.{CwdWorkspace, DefaultIntelliJRunner, SharedIndexesService}
import com.virtuslab.shared_indexes.demo.{JarLocator, JdkLocator, ProjectLocator}
import com.virtuslab.shared_indexes.kinds.jars.JarsIndexesService
import com.virtuslab.shared_indexes.kinds.jdk.JdkIndexesService
import com.virtuslab.shared_indexes.kinds.project.ProjectIndexesService
import com.virtuslab.shared_indexes.util.Logging
import org.kohsuke.args4j.{CmdLineException, CmdLineParser}

import java.nio.file.Path

object Cli extends Logging {

  def main(args: Array[String]): Unit = {
    val cliArgs = new CliArgs
    val parser = new CmdLineParser(cliArgs)
    try {
      parser.parseArgument(args: _*)
      run(cliArgs)
    } catch {
      case e: CmdLineException =>
        logger.error(e.getMessage)
        parser.printUsage(System.err)
        sys.exit(1)
    }
  }

  def run(args: CliArgs): Unit = {
    try {
      val service = resolveService(args)
      val generatedIndexesDir = service.generateIndexes()
      service.store(generatedIndexesDir, args.uploadLocations)
    } catch {
      case e: Exception =>
        logger.error(e.getMessage)
        e.printStackTrace()
        sys.exit(1)
    }
  }

  private def resolveService(args: CliArgs): SharedIndexesService = {
    val ijRunner = new DefaultIntelliJRunner(args.ijBinary)
    val outputDir = args.localOutput
    val inputs = args.inputs
    val workspace = CwdWorkspace

    args.kind match {
      case SharedIndexKind.Jdk =>
        val inputs = if (args.demo && args.inputs.isEmpty) {
          JdkLocator.findAllInstalledJdks()
        } else {
          args.inputs
        }

        new JdkIndexesService(outputDir, ijRunner, workspace, inputs, args.regenerate, args.debug)

      case SharedIndexKind.Project =>
        val projectPath = if (args.demo && args.inputs.isEmpty) {
          ProjectLocator.exampleProjectHome
        } else {
          inputs match {
            case Seq(input) => input
            case _ => throw new RuntimeException("Specify exactly one path for project shared index generation")
          }
        }
        val resolveProjectName = args.projectName.map(name => (_: Path) => name)
        new ProjectIndexesService(
          outputDir,
          ijRunner,
          workspace,
          projectPath,
          resolveProjectName,
          args.debug,
          args.indexesToKeep
        )

      case SharedIndexKind.Jars =>
        val inputs = if (args.demo) {
          JarLocator.findExampleJars(args.inputs)
        } else {
          args.inputs
        }

        // Suggested key would be composed of name of sets of jars, like all-3rd-party-deps
        // and some hash of them, for example commit hash of project that contains these jars
        // in current versions example: "all-jars-1234567890abcdef"
        val indexKey = "bundle"
        new JarsIndexesService(outputDir, ijRunner, workspace, inputs, indexKey, args.debug)
    }
  }
}
