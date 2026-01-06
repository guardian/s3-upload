import PlayKeys._

name := "s3-uploader"
version := "1.0"
scalaVersion := "2.13.18"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  ws, filters,
  "software.amazon.awssdk" % "s3" % "2.35.6",
  "com.gu" %% "pan-domain-auth-verification" % "13.0.0",
  "com.gu" %% "editorial-permissions-client" % "5.0.0",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.15.4"
)

resolvers ++= Resolver.sonatypeOssRepos("releases")

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


