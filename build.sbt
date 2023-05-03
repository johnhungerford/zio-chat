import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0-RC5"

lazy val commonSettings = Seq(
    libraryDependencies ++= zio,
)

lazy val root: Project = (project in file("."))
    .aggregate(core)

lazy val core = (project in file("modules/01-core/core"))
    .settings(
        commonSettings,
    )

lazy val userService = (project in file("modules/02-service/chat-user-service"))
    .dependsOn(core % "compile->compile;test->test")
    .settings(
        commonSettings,
    )
