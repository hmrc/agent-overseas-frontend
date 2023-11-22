import CodeCoverageSettings.scoverageSettings
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val root = Project("agent-overseas-frontend", file("."))
  .settings(
    name := "agent-overseas-frontend",
    organization := "uk.gov.hmrc",
    majorVersion := 1,
    scalaVersion := "2.13.10",
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
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := true,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .settings(
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
