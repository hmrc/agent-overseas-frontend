import sbt._

object AppDependencies {

  private val mongoVersion = "0.74.0"
  private val bootstrapVer = "7.22.0"

  lazy val compile = Seq(
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-28"    % bootstrapVer,
    "uk.gov.hmrc"           %% "play-frontend-hmrc"            % "7.7.0-play-28",
    "uk.gov.hmrc"           %% "play-partials"                 % "8.4.0-play-28",
    "uk.gov.hmrc"           %% "agent-kenshoo-monitoring"      % "5.5.0",
    "uk.gov.hmrc"           %% "agent-mtd-identifiers"         % "1.15.0",
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-28"            % mongoVersion,
    "uk.gov.hmrc"           %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "org.typelevel"         %% "cats-core"                     % "2.9.0",
    "com.github.tototoshi"  %% "scala-csv"                     % "1.3.10"
  )

  lazy val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"      % "test, it",
    "org.scalatestplus"      %% "mockito-3-12"            % "3.2.10.0"   % "test, it",
    "com.github.tomakehurst" % "wiremock-jre8"            % "2.26.1"     % "test, it",
    "org.jsoup"              % "jsoup"                    % "1.15.4"     % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % mongoVersion % "test, it",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVer % "test, it",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.36.8"    % "test, it"
  )

}
