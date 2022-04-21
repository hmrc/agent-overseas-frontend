import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val compileDeps = Seq(
  "uk.gov.hmrc"           %% "bootstrap-frontend-play-28"    % "5.23.0",
  "uk.gov.hmrc"           %% "play-frontend-hmrc"            % "3.14.0-play-28",
  "uk.gov.hmrc"           %% "play-partials"                 % "8.3.0-play-28",
  "uk.gov.hmrc"           %% "agent-kenshoo-monitoring"      % "4.8.0-play-28",
  "uk.gov.hmrc"           %% "agent-mtd-identifiers"         % "0.35.0-play-28",
  "uk.gov.hmrc"           %% "mongo-caching"                 % "7.1.0-play-28",
  "uk.gov.hmrc"           %% "play-conditional-form-mapping" % "1.11.0-play-28",
  "uk.gov.hmrc"           %% "simple-reactivemongo"          % "8.0.0-play-28",
  "uk.gov.hmrc"           %% "domain"                        % "7.0.0-play-28",
  "org.typelevel"         %% "cats-core"                     % "2.6.1",
  "com.github.tototoshi"  %% "scala-csv"                     % "1.3.8"
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.26.1" % scope,
  "org.jsoup" % "jsoup" % "1.14.2" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "5.0.0-play-28" % scope,
  "com.vladsch.flexmark" %  "flexmark-all" % "0.35.10" % scope
)

lazy val root = Project("agent-overseas-frontend", file("."))
  .settings(
    name := "agent-overseas-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes"),
    PlayKeys.playDefaultPort := 9414,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.8" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.8" % Provided cross CrossVersion.full
    ),
    publishingSettings,
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    majorVersion := 0
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := true,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
