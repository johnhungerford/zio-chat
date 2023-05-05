import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0-RC5"

addCommandAlias("testAll", "core/test; userService/test; messageService/test; eventService/test")

lazy val commonSettings = Seq(
    libraryDependencies ++= zio,
)

lazy val root: Project = (project in file("."))
    .aggregate(core, userService, messageService, eventService)

lazy val core = (project in file("modules/01-core/core"))
    .settings(
        commonSettings,
    )

lazy val userService = (project in file("modules/02-service/chat-user-service"))
    .dependsOn(core % "compile->compile;test->test")
    .settings(
        commonSettings,
    )

lazy val messageService = (project in file("modules/02-service/chat-message-service"))
    .dependsOn(core % "compile->compile;test->test")
    .settings(
        commonSettings,
    )

lazy val eventService = (project in file("modules/02-service/chat-event-service"))
    .dependsOn(core % "compile->compile;test->test", messageService)
    .settings(
        commonSettings,
    )