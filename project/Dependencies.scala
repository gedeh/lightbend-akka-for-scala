import com.lightbend.cinnamon.sbt.Cinnamon
import sbt._

object Version {
  val akkaVer           = "2.6.5"
  val logbackVer        = "1.2.3"
  val logbackContribVer = "0.1.5"
  val scalaVer          = "2.13.2"
  val scalaParsersVer   = "1.1.2"
  val scalaTestVer      = "3.1.0"

}

object Dependencies {
  val dependencies = Seq(
    "com.typesafe.akka"         %% "akka-actor"                 % Version.akkaVer,
    "com.typesafe.akka"         %% "akka-slf4j"                 % Version.akkaVer,
    "ch.qos.logback"             % "logback-classic"            % Version.logbackVer,
    "ch.qos.logback.contrib"     % "logback-jackson"            % Version.logbackContribVer,
    "ch.qos.logback.contrib"     % "logback-json-classic"       % Version.logbackContribVer,
    "org.scala-lang.modules"    %% "scala-parser-combinators"   % Version.scalaParsersVer,
    "com.lightbend.akka"        %% "akka-diagnostics"           % "1.1.14",
    Cinnamon.library.cinnamonAkka,
    Cinnamon.library.cinnamonJvmMetricsProducer,
    Cinnamon.library.cinnamonPrometheus,
    Cinnamon.library.cinnamonPrometheusHttpServer,
    "com.typesafe.akka"         %% "akka-testkit"               % Version.akkaVer % "test",
    "org.scalatest"             %% "scalatest"                  % Version.scalaTestVer % "test",
    "com.fasterxml.jackson.core" % "jackson-databind"           % "2.10.3"
  )
}
