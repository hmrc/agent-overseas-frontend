import sbt.*

object AppDependencies {

  private val mongoVer: String = "2.6.0"
  private val bootstrapVer: String = "9.16.0"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30"    % bootstrapVer,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30"    % "12.7.0",
    "uk.gov.hmrc"           %% "agent-mtd-identifiers"         % "2.2.0",
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"            % mongoVer,
    "uk.gov.hmrc"           %% "play-conditional-form-mapping-play-30" % "3.3.0",
    "org.typelevel"         %% "cats-core"                     % "2.13.0",
    "uk.gov.hmrc"           %% "crypto-json-play-30"           % "8.2.0"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVer % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVer % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1"      % Test,
  )

}
