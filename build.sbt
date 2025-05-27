import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}
import CodeCoverageSettings.scoverageSettings

val appName = "agent-overseas-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.16"


val scalaCOptions = Seq(
//  "-Xfatal-warnings",
  "-Wconf:msg=match may not be exhaustive:is",
  "-Xlint:-missing-interpolator,_",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:src=*html:w",     // silence html warnings as they are wrong
  "-Wconf:src=routes/.*:s", // silence warnings from routes
  "-language:implicitConversions"
)


lazy val root = (project in file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9414,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    scalacOptions ++= scalaCOptions,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    Test / parallelExecution := false,
    scoverageSettings
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
