name := "key-value"

version := "0.2.0"
organization := "com.github.gekomad"
scalaVersion := "2.13.15"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test

val scalacOptions = Seq(
  "-encoding",
  "utf8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Wunused:implicits",
  "-Wunused:explicits",
  "-Wunused:imports",
  "-Wunused:locals",
  "-Wunused:params",
  "-Wunused:privates",
  "-Xfatal-warnings"
)
