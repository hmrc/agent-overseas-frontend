import sbt.*

object AppDependencies {

  private val mongoVersion     = "1.8.0"
  private val bootstrapVersion = "8.5.0"
  private val playVersion      = "play-30"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playVersion"            % bootstrapVersion,
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playVersion"            % "9.6.0",
    "uk.gov.hmrc"           %% s"play-partials-$playVersion"                 % "9.1.0",
    "uk.gov.hmrc"           %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playVersion"                    % mongoVersion,
    "uk.gov.hmrc"           %% "agent-mtd-identifiers"                       % "2.0.0",
    "org.typelevel"         %% "cats-core"                                   % "2.10.0",
    "com.github.tototoshi"  %% "scala-csv"                                   % "1.3.10"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"            % "7.0.1"          % Test,
    "org.scalatestplus"      %% "mockito-5-10"                  % "3.2.18.0"       % Test,
    "org.jsoup"              %  "jsoup"                         % "1.17.2"         % Test,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % mongoVersion     % Test,
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"                  % "0.64.8"         % Test
  )

}
