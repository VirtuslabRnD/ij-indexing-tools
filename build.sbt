scalaVersion := "2.13.14"

name := "ij-indexing-tools"
organization := "com.virtuslab"
version := "0.0.1"

resolvers += "JetBrains" at "https://packages.jetbrains.team/maven/p/ij/intellij-shared-indexes"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "os-lib" % "0.10.5",
  "com.lihaoyi" %% "mainargs" % "0.7.5",
  "com.jetbrains.intellij.indexing.shared" % "ij-shared-indexes-tool" % "0.9.9",
  "org.slf4j" % "slf4j-simple" % "2.0.13"
)
