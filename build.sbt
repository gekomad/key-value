name := "key-value"

version      := "0.3.4"
organization := "com.github.gekomad"
scalaVersion := "3.7.4"

libraryDependencies += "org.typelevel"                %% "cats-effect"       % "3.6.3"
libraryDependencies += "org.typelevel"                %% "log4cats-slf4j"    % "2.7.1"
libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine"          % "3.2.3"
libraryDependencies += "ch.qos.logback"                % "logback-classic"   % "1.5.22" % Test
libraryDependencies += "org.typelevel"                %% "munit-cats-effect" % "2.1.0"  % Test

scalacOptions ++= Seq(
  "-Xmax-inlines",
  "300",
  "-language:postfixOps",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-old-syntax",
  "-rewrite",
  "-source",
  "3.7-migration",
  "-Xfatal-warnings",
  "-Wvalue-discard",
  "-Wunused:all"
)
