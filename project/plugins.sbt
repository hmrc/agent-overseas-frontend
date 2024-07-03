resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("org.playframework"    % "sbt-plugin"            % "3.0.3")
addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"        % "3.22.0")
addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"    % "2.5.0")

addSbtPlugin("org.scoverage"        % "sbt-scoverage"         % "2.0.12")
addSbtPlugin("org.playframework.twirl"    % "sbt-twirl"       % "2.0.5")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"          % "2.5.0")
addSbtPlugin("io.github.irundaia"   % "sbt-sassify"           % "1.5.2")
