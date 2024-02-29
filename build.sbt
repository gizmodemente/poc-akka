import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven")

Compile / scalacOptions ++= Seq(
  "-release:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

run / fork := true
// pass along config selection to forked jvm
run / javaOptions ++= sys.props
  .get("config.resource")
  .fold(Seq.empty[String])(res => Seq(s"-Dconfig.resource=$res"))
Global / cancelable := false // ctrl-c

val AkkaVersion = "2.9.1"
val AkkaHttpVersion = "10.6.0"
val AkkaManagementVersion = "1.5.0"
val AkkaPersistenceR2dbcVersion = "1.2.1"
val AkkaProjectionVersion =
  sys.props.getOrElse("akka-projection.version", "1.5.2")
val AkkaDiagnosticsVersion = "2.1.0"

ThisBuild / dynverSeparator := "-"

lazy val `poc-akka` = (project in file("."))
  .aggregate(`poc-akka-api`, `poc-akka-impl`)

lazy val `poc-akka-api` = (project in file("poc-akka-api"))
  .enablePlugins(AkkaGrpcPlugin)

lazy val `poc-akka-impl` = (project in file("poc-akka-impl"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      // 1. Basic dependencies for a clustered application
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
      // Akka Management powers Health Checks and Akka Cluster Bootstrapping
      "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
      "com.lightbend.akka" %% "akka-diagnostics" % AkkaDiagnosticsVersion,
      // Common dependencies for logging and testing
      "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      // 2. Using Akka Persistence
      "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "com.lightbend.akka" %% "akka-persistence-r2dbc" % AkkaPersistenceR2dbcVersion,
      "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
      // 3. Querying and publishing data from Akka Persistence
      "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
      "com.lightbend.akka" %% "akka-projection-r2dbc" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-grpc" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-testkit" % AkkaProjectionVersion % Test)
  ).settings(
    dockerBaseImage := "docker.io/library/eclipse-temurin:17.0.8.1_1-jre",
    dockerUsername := sys.props.get("docker.username"),
    dockerRepository := sys.props.get("docker.registry"),
    dockerBuildxPlatforms := Seq("linux/amd64"),
    dockerUpdateLatest := true
  )
  .dependsOn(`poc-akka-api`)
