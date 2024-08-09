import PlayKeys._

name := "s3-uploader"
version := "1.0"
scalaVersion := "2.13.14"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  ws, filters,
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.761",
  "com.gu" %% "pan-domain-auth-verification" % "6.0.0-PREVIEW.update-settings-loading-and-parsing-code.2024-08-09T1450.4c87946a"
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)
  .settings(
    playDefaultPort := 9050,

    pipelineStages := Seq(digest, gzip),
    debianPackageDependencies := Seq("java11-runtime-headless"),
    maintainer := "Editorial Tools <digitalcms.dev@guardian.co.uk>",
    packageSummary := "S3 Uploader",
    packageDescription := """Allow uploading images to S3""",

    Universal / javaOptions ++= Seq(
      "-Dpidfile.path=/dev/null"
    )
  )


