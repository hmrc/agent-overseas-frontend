import CodeCoverageSettings.scoverageSettings
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "agent-overseas-frontend",
    organization := "uk.gov.hmrc",
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Wconf:msg=match may not be exhaustive:is",
      "-Xlint:-missing-interpolator,_",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=*html:w",     // silence html warnings as they are wrong
      "-Wconf:src=routes/.*:s", // silence warnings from routes
      "-language:implicitConversions"),
    PlayKeys.playDefaultPort := 9414,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )