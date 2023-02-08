organization := "com.ovoenergy"
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

scalaVersion := "3.2.1"
crossScalaVersions := Seq(scalaVersion.value, "2.13.10", "2.12.17")
releaseCrossBuild := true

libraryDependencies += "is.cir" %% "ciris" % "3.1.0"
libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

publishTo := Some("Artifactory Realm" at "https://kaluza.jfrog.io/artifactory/maven")
ThisBuild / versionScheme := Some("early-semver")
