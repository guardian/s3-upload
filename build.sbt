import PlayKeys._

name := "s3-uploader"
version := "1.0"
scalaVersion := "2.13.10"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  ws, filters,
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.351",
  "com.gu" %% "pan-domain-auth-verification" % "1.2.0"
)

dependencyOverrides ++= Seq (
  "org.bouncycastle" % "bcprov-jdk15on" % "1.67",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4",
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)
  .settings(
    playDefaultPort := 9050,

    pipelineStages := Seq(digest, gzip),
    debianPackageDependencies := Seq("openjdk-8-jre-headless"),
    maintainer := "Editorial Tools <digitalcms.dev@guardian.co.uk>",
    packageSummary := "S3 Uploader",
    packageDescription := """Allow uploading images to S3""",

    Universal / javaOptions ++= Seq(
      "-Dpidfile.path=/dev/null"
    )
  )


