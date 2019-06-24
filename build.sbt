name := "key-value"

version := "0.0.2"
organization := "com.github.gekomad"
scalaVersion := "2.12.8"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

//libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.18" % "test"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.6", "2.12.8", "2.13.0")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings"
)

scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq()
    case Some((2, 10)) => Seq()
    case _ => Seq("-Ywarn-unused-import", "-Ywarn-unused")
  })

