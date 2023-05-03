import sbt._

object Dependencies {
    val zioVersion = "2.0.13"
    val zioLoggingVersion = "2.1.11"
    val zioPreludeVersion = "1.0.0-RC19"
    val zioJsonVersion = "0.5.0"
    val zioDirectVersion = "1.0.0-RC7"

    val zio = Seq(
        "dev.zio" %% "zio" % zioVersion,
        "dev.zio" %% "zio-streams" % zioVersion,
        "dev.zio" %% "zio-prelude" % zioPreludeVersion,
        "dev.zio" %% "zio-json" % zioJsonVersion,
        "dev.zio" %% "zio-test"          % zioVersion % Test,
        "dev.zio" %% "zio-test-sbt"      % zioVersion % Test,
        "dev.zio" %% "zio-direct" % zioDirectVersion,
    )

    val logging = Seq(
        "dev.zio" %% "zio-logging" % zioLoggingVersion,
        "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
    )
}