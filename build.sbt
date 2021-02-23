organization := "com.ovoenergy"
bintrayOrganization := Some("ovotech")
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

scalaVersion := "2.13.5"
crossScalaVersions := Seq(scalaVersion.value, "2.12.13")
releaseCrossBuild := true

libraryDependencies += "is.cir" %% "ciris" % "1.2.1"
