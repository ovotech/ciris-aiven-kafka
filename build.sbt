organization := "com.ovoenergy"
bintrayOrganization := Some("ovotech")
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

scalaVersion := "2.12.6"
crossScalaVersions := Seq("2.11.12", scalaVersion.value)
releaseCrossBuild := true

scalacOptions += "-language:higherKinds"

libraryDependencies ++= Seq(
  "is.cir" %% "ciris-core" % "0.10.0",
  "com.github.pathikrit" %% "better-files" % "3.4.0"
)
