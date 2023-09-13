resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % "2.8.19")
addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.12.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.2.0")

addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "1.9.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-twirl"          % "1.5.1")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"       % "1.16")
addSbtPlugin("org.irundaia.sbt"  % "sbt-sassify"        % "1.4.13")
