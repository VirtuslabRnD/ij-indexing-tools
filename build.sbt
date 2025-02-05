ThisBuild / scalaVersion := "2.13.14"

name := "ij-indexing-tools"
organization := "com.virtuslab"
version := "0.0.1"

resolvers += "JetBrains" at "https://packages.jetbrains.team/maven/p/ij/intellij-shared-indexes"

lazy val indexingTools = project.in(file(".")).settings(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "os-lib" % "0.10.5",
    "com.lihaoyi" %% "mainargs" % "0.7.5",
    "args4j" % "args4j" % "2.37",
    "com.jetbrains.intellij.indexing.shared" % "ij-shared-indexes-tool" % "0.9.13",
    "org.slf4j" % "slf4j-simple" % "2.0.13",
    "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    "org.scalamock" %% "scalamock" % "6.0.0" % Test
  )
)

lazy val reporterPlugin =
  project.in(file("reporter-plugin"))
    .enablePlugins(SbtIdeaPlugin)
    .settings(
      version := "0.0.1-SNAPSHOT",
      ThisBuild / intellijPluginName := "Indexing Statistics Reporter",
      ThisBuild / intellijBuild := "233.15619.7",
      ThisBuild / intellijPlatform := IntelliJPlatform.IdeaUltimate,
      Global / intellijAttachSources := true,
      Compile / javacOptions ++= "--release" :: "17" :: Nil,
      Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
      Test / unmanagedResourceDirectories += baseDirectory.value / "testResources",
      packageMethod := PackagingMethod.Standalone(),
      packageLibraryMappings := Seq.empty
    )
