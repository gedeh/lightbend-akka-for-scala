
import sbt._

lazy val `lightbend-akka-for-scala` = (project in file("."))
  .aggregate(
    common,
    exercises
  )
  .settings(CommonSettings.commonSettings: _*)
lazy val common = project.settings(CommonSettings.commonSettings: _*)

lazy val exercises = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")
  // .enablePlugins(Cinnamon)
