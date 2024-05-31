resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("org.playframework" % "sbt-plugin"         % "3.0.2")
addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.21.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.5.0")

addSbtPlugin("org.scoverage"           % "sbt-scoverage" % "2.0.11")
addSbtPlugin("com.lucidchart"          % "sbt-scalafmt"  % "1.16")
//addSbtPlugin("org.irundaia.sbt"        % "sbt-sassify"   % "1.4.13")
addSbtPlugin("io.github.irundaia"      % "sbt-sassify"   % "1.5.2")