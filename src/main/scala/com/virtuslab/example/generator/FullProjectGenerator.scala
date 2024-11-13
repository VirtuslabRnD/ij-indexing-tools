package com.virtuslab.example.generator

import com.virtuslab.example.generator.FullProjectGenerator.logger
import com.virtuslab.shared_indexes.config.GeneratorConfig
import com.virtuslab.shared_indexes.core.Workspace
import com.virtuslab.shared_indexes.locator.ProjectLocator
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

case class FullProjectGenerator(moduleNumber:Int, maxIndex: Int, useFileUri: Boolean) {

  private val subprojects = for (id <- 1 to moduleNumber) yield s"subproject_$id"

  private val subprojectJson = s"""{
                                 |  "module-list": [
                                 |    ${subprojects.map(s => s"\"${ProjectLocator.exampleProjectHome / s}\"").mkString(",\n    ")}
                                 |  ]
                                 |}""".stripMargin

  private val generateBuildSbt =
    s"""
      |scalaVersion := "2.13.14"
      |
      |name := "full-project"
      |organization := "com.example"
      |version := "0.0.1"
      |
      |${subprojects.map(name => s"lazy val $name = project").mkString("\n")}
      |
      |libraryDependencies ++= Seq(
      |  "com.lihaoyi" %% "os-lib" % "0.10.5",
      |  "com.lihaoyi" %% "mainargs" % "0.7.5",
      |  "org.apache.spark" %% "spark-core" % "3.2.3",
      |
      |  "org.springframework.boot"           % "spring-boot-starter-web"           % "2.7.16",
      |  "org.hibernate"                      % "hibernate-core"                    % "5.6.15.Final",
      |  "com.fasterxml.jackson.core"         % "jackson-databind"                  % "2.15.2",
      |  "com.google.guava"                   % "guava"                             % "30.1.1-jre",
      |  "org.apache.commons"                 % "commons-lang3"                     % "3.13.0",
      |  "org.apache.httpcomponents.client5"  % "httpclient5"                       % "5.2.1",
      |  "org.slf4j"                          % "slf4j-api"                         % "1.7.36",
      |  "ch.qos.logback"                     % "logback-classic"                   % "1.2.11",
      |  "org.apache.logging.log4j"           % "log4j-core"                        % "2.20.0",
      |  "org.mockito"                        % "mockito-core"                      % "4.11.0",
      |  "org.junit.jupiter"                  % "junit-jupiter-api"                 % "5.8.2",
      |  "com.amazonaws"                      % "aws-java-sdk-bom"                  % "1.12.565",
      |  "com.amazonaws"                      % "aws-java-sdk-s3"                   % "1.12.565",
      |  "org.apache.camel"                   % "camel-core"                        % "3.14.5",
      |  "org.elasticsearch.client"           % "elasticsearch-rest-high-level-client" % "7.17.10",
      |  "org.apache.tika"                    % "tika-parsers"                      % "1.28.5",
      |  "org.apache.poi"                     % "poi-ooxml"                         % "5.2.3",
      |  "org.springframework.batch"          % "spring-batch-core"                 % "4.3.8",
      |  "com.zaxxer"                         % "HikariCP"                          % "4.0.3",
      |  "org.apache.kafka"                   % "kafka-clients"                     % "2.8.2",
      |  "org.mybatis"                        % "mybatis"                           % "3.5.10",
      |  "com.fasterxml.jackson.module"       % "jackson-module-kotlin"             % "2.15.2",
      |  "org.redisson"                       % "redisson"                          % "3.17.7",
      |  "net.sf.ehcache"                     % "ehcache"                           % "2.10.9.2",
      |  "org.apache.zookeeper"               % "zookeeper"                         % "3.7.1",
      |  "org.apache.hbase"                   % "hbase-client"                      % "2.4.13",
      |  "org.apache.hadoop"                  % "hadoop-common"                     % "3.3.4",
      |  "org.neo4j.driver"                   % "neo4j-java-driver"                 % "4.4.13",
      |  "com.rabbitmq"                       % "amqp-client"                       % "5.14.2",
      |  "org.apache.activemq"                % "activemq-all"                      % "5.17.5",
      |  "io.netty"                           % "netty-all"                         % "4.1.97.Final",
      |  "org.apache.httpcomponents"          % "httpclient"                        % "4.5.14",
      |  "com.google.code.gson"               % "gson"                              % "2.10.1",
      |  "commons-io"                         % "commons-io"                        % "2.13.0",
      |  "org.jsoup"                          % "jsoup"                             % "1.16.1",
      |  "org.bytedeco"                       % "javacv"                            % "1.5.8"
      |)
      |
      |""".stripMargin

  private val generateBuildProperties =
    """sbt.version=1.10.1
      |""".stripMargin

  private val generateFileIntelliJYaml =
    s"""sharedIndex:
       |  project:
       |    - url: ${(new Workspace(GeneratorConfig().workDir).cdnPath / "project" / "full-project").toNIO.toUri}
       |  consents:
       |    - kind: project
       |      decision: allowed
       |""".stripMargin

  private val generateS3ServerIntelliJYaml =
    s"""sharedIndex:
       |  project:
       |    - url: http://127.0.0.1:9000/shared-index/project/full-project
       |  consents:
       |    - kind: project
       |      decision: allowed
       |""".stripMargin
  private val methodLimit = 5
  private val methods = for (i <- 1 to methodLimit) yield generateMethod(i)

  private def imports(index: Int) =
    if (index <= methodLimit)
      for (i <- 1 until index) yield importString(i)
    else
      for (i <- (index - methodLimit) until index) yield importString(i)
  private def importString(index: Int) =
    s"""import com.example.class$index.Class$index
       |import com.example.object$index.Object$index""".stripMargin

  private def classMethodCalls(index: Int): Seq[String] = {
    def classMethodCall(classIndex: Int) = for (i <- 1 until methodLimit)
      yield s"def callClass${classIndex}Method$i() = Class$classIndex().method$i()"

    if (index <= methodLimit)
      (1 until index).flatMap(i => classMethodCall(i))
    else
      ((index - methodLimit) until index).flatMap(i => classMethodCall(i))
  }

  private def objectMethodCalls(index: Int): Seq[String] = {
    def objectMethodCall(objectIndex: Int) = for (i <- 1 until methodLimit)
      yield s"def callObject${objectIndex}Method$i() = Object$objectIndex.method$i()"

    if (index <= methodLimit)
      (1 until index).flatMap(i => objectMethodCall(i))
    else
      ((index - methodLimit) until index).flatMap(i => objectMethodCall(i))
  }

  private def generateClass(index: Int): String = {
    val packageString = s"com.example.class$index"

    val allMethods = methods ++ classMethodCalls(index) ++ objectMethodCalls(index)

    s"""package $packageString
      |
      |${imports(index).mkString(System.lineSeparator())}
      |
      |case class Class$index() {
      |
      |${allMethods.mkString(" " * 2, System.lineSeparator() * 2 + " " * 2, System.lineSeparator())}
      |
      |}
      |""".stripMargin
  }

  private def generateObject(index: Int): String = {
    val packageString = s"com.example.object$index"

    val allMethods = methods ++ classMethodCalls(index) ++ objectMethodCalls(index)

    s"""package $packageString
       |
       |${imports(index).mkString(System.lineSeparator())}
       |
       |object Object$index {
       |
       |${allMethods.mkString(" " * 2, System.lineSeparator() * 2 + " " * 2, System.lineSeparator())}
       |
       |}
       |""".stripMargin
  }

  private def generateMethod(index: Int): String = {
    s"def method$index(): Unit = println(\"method$index\")"
  }

  private def generateProject(): Unit = {
    val projectRoot = FullProjectGenerator.exampleProjectPath
    os.remove.all(projectRoot)
    os.makeDir(projectRoot)

    logger.info("Generating build.sbt")
    os.write.over(projectRoot / "build.sbt", generateBuildSbt)
    logger.info("Generating project/build.properties")
    os.makeDir.all(projectRoot / "project")
    os.write.over(projectRoot / "project" / "build.properties", generateBuildProperties)

    logger.info(s"Generating intellij.yaml")
    if (useFileUri)
      os.write.over(projectRoot / "intellij.yaml", generateFileIntelliJYaml)
    else
      os.write.over(projectRoot / "intellij.yaml", generateS3ServerIntelliJYaml)

    val moduleMaxIndex = maxIndex / subprojects.length

    for (subproject <- subprojects)  {
      val sourceRoot = projectRoot / subproject / "src" / "main" / "scala"
      os.makeDir.all(sourceRoot)

      for (i <- 1 to moduleMaxIndex) {
        val classFileContent = generateClass(i)
        val fileName = s"Class$i.scala"
        os.write.over(sourceRoot / fileName, classFileContent)
      }

      logger.info(s"Generated $moduleMaxIndex Scala Classes in $subproject")

      for (i <- 1 to moduleMaxIndex) {
        val objectFileContent = generateObject(i)
        val fileName = s"Object$i.scala"
        os.write.over(sourceRoot / fileName, objectFileContent)
      }

      logger.info(s"Generated $moduleMaxIndex Scala Objects in $subproject")

      splitIntoLevels(sourceRoot)
    }

    logger.info(s"Json list of modules: \n\n $subprojectJson")
  }

  @tailrec
  private def splitIntoLevels(srcRoot: os.Path, level: Int = 0): Unit = {
    import FullProjectGenerator.maxElementsOnLevel
    val ls = os.list(srcRoot)
    if (ls.size > maxElementsOnLevel) {
      ls.sortBy(_.baseName).iterator.sliding(maxElementsOnLevel, maxElementsOnLevel)
        .zipWithIndex
        .foreach { case (elements, id) =>
          val dir = srcRoot / s"dir-$level-$id"
          os.makeDir(dir)
          elements.foreach(os.move.into(_, dir, replaceExisting = true))
        }
      splitIntoLevels(srcRoot, level + 1)
    }
  }
}

object FullProjectGenerator {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val exampleProjectPath = ProjectLocator.exampleProjectHome
  private val moduleNumber = 15
  private val maxIndex = 15000
  private val useFileUri = false
  private val maxElementsOnLevel = 10

  def main(args: Array[String]): Unit = FullProjectGenerator(moduleNumber, maxIndex, useFileUri).generateProject()
}
