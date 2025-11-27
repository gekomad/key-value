name := "key-value"

version := "0.3.2"
organization := "com.github.gekomad"
scalaVersion := "3.7.4"

libraryDependencies += "org.scalameta" %% "munit"       % "1.2.1" % Test
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.6.3" // only for CatsCache
