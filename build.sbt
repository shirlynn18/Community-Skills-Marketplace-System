ThisBuild / scalaVersion := "3.3.5"

val javafxVersion = "21"
val scalafxVersion = "21.0.0-R32"
val scalikejdbcVersion = "4.3.5"
val derbyVersion = "10.16.1.1"
val logbackVersion = "1.5.6"

// Determine OS name for JavaFX classifier
val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _                            => throw new Exception("Unknown platform!")
}

lazy val root = (project in file("."))
  .settings(
    name := "Project_23060486",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % scalafxVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
      "org.apache.derby" % "derby" % derbyVersion,
      "org.apache.derby" % "derbyshared" % derbyVersion,
      "org.apache.derby" % "derbytools" % derbyVersion,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    // JavaFX modules with OS classifier
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media", "web").map(m =>
      "org.openjfx" % s"javafx-$m" % javafxVersion classifier osName
    ),
    scalacOptions ++= Seq("-Wunused:all"),
    fork := true
  )

