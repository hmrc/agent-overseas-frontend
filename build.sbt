import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "2.24.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.55.0-play-27",
  "uk.gov.hmrc" %% "play-ui" % "8.12.0-play-27",
  "uk.gov.hmrc" %% "auth-client" % "3.0.0-play-27",
  "uk.gov.hmrc" %% "play-partials" % "6.11.0-play-27",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "4.4.0",
  "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.19.0-play-27",
  "uk.gov.hmrc" %% "http-caching-client" % "9.1.0-play-27",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.3.0-play-26",
  "uk.gov.hmrc" %% "domain" % "5.9.0-play-27",
  "org.typelevel" %% "cats-core" % "2.1.1",
  "com.github.tototoshi" %% "scala-csv" % "1.3.5",
  "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-27"
)

def testDeps(scope: String) = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
  "org.scalatest" %% "scalatest" % "3.0.8" % scope,
  "org.mockito" % "mockito-core" % "3.2.4" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.27.1" % scope,
  "org.jsoup" % "jsoup" % "1.12.1" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "4.21.0-play-27" % scope
)

def tmpMacWorkaround(): Seq[ModuleID] =
  if (sys.props.get("os.name").fold(false)(_.toLowerCase.contains("mac")))
    Seq("org.reactivemongo" % "reactivemongo-shaded-native" % "0.16.1-osx-x86-64" % "runtime,test,it")
  else Seq()

lazy val root = Project("agent-overseas-frontend", file("."))
  .settings(
    name := "agent-overseas-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.10",
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-value-discard",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes"),
    PlayKeys.playDefaultPort := 9414,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= tmpMacWorkaround() ++ compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full
    ),
    publishingSettings,
    scoverageSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    scalafmtOnCompile in Compile := true,
    scalafmtOnCompile in Test := true,
    majorVersion := 0
  )
  .configs(IntegrationTest)
  .settings(
    Keys.fork in IntegrationTest := true,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    scalafmtOnCompile in IntegrationTest := true
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)