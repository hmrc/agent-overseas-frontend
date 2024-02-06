resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % "2.8.20")
addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.20.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.2.0")

addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "2.0.6")
addSbtPlugin("com.typesafe.sbt"  % "sbt-twirl"          % "1.5.1")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"       % "1.16")
addSbtPlugin("org.irundaia.sbt"  % "sbt-sassify"        % "1.4.13")

//fix for scoverage compile errors for scala 2.13.10
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
