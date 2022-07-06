organization := "com.ovoenergy"
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

scalaVersion := "3.1.2"
crossScalaVersions := Seq(scalaVersion.value, "2.13.6", "2.12.16")
releaseCrossBuild := true

libraryDependencies += "is.cir" %% "ciris" % "2.2.1"

publishTo := Some("Artifactory Realm" at "https://kaluza.jfrog.io/artifactory/maven")
ThisBuild / versionScheme := Some("early-semver")
