organization := "com.ovoenergy"
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

scalaVersion := "2.13.6"
crossScalaVersions := Seq(scalaVersion.value, "2.12.14")
releaseCrossBuild := true

libraryDependencies += "is.cir" %% "ciris" % "2.0.1"

publishTo := Some("Artifactory Realm" at "https://kaluza.jfrog.io/artifactory/maven")
ThisBuild / versionScheme := Some("early-semver")

