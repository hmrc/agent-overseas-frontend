import sbt.*

object AppDependencies {

  private val mongoVer: String = "2.12.0"
  private val bootstrapVer: String = "10.7.0"
  private val playVer: String = "play-30"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playVer"            % bootstrapVer,
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playVer"            % "12.32.0",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playVer"                    % mongoVer,
    "uk.gov.hmrc"           %% s"play-conditional-form-mapping-$playVer" % "3.5.0",
    "org.typelevel"         %% "cats-core"                             % "2.13.0",
    "uk.gov.hmrc"           %% s"crypto-json-$playVer"                   % "8.4.0",
    "uk.gov.hmrc"           %% s"domain-$playVer"                        % "11.0.0"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVer"  % bootstrapVer % Test,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVer" % mongoVer     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.2"      % Test,
  )

}
