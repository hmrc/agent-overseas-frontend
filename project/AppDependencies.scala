import sbt._

object AppDependencies {

  val mongoVersion = "0.74.0"

  lazy val compileDeps = Seq(
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-28"    % "7.12.0",
    "uk.gov.hmrc"           %% "play-frontend-hmrc"            % "5.1.0-play-28",
    "uk.gov.hmrc"           %% "play-partials"                 % "8.3.0-play-28",
    "uk.gov.hmrc"           %% "agent-kenshoo-monitoring"      % "4.8.0-play-28",
    "uk.gov.hmrc"           %% "agent-mtd-identifiers"         % "0.51.0-play-28",
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-28"            % mongoVersion,
    "uk.gov.hmrc"           %% "play-conditional-form-mapping" % "1.12.0-play-28",
    "org.typelevel"         %% "cats-core"                     % "2.6.1",
    "com.github.tototoshi"  %% "scala-csv"                     % "1.3.8"
  )

  def testDeps(scope: String) = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
    "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.26.1" % scope,
    "org.jsoup" % "jsoup" % "1.14.2" % scope,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % mongoVersion % scope,
    "com.vladsch.flexmark" %  "flexmark-all" % "0.35.10" % scope
  )

}
