import PlayKeys._

name := "s3-uploader"
version := "1.0"
scalaVersion := "2.12.8"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Ypartial-unification"
)

libraryDependencies ++= Seq(
  ws, filters,
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.539",
  "com.gu" %% "pan-domain-auth-verification" % "0.8.2"
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin)
  .settings(
    playDefaultPort := 9050,

    riffRaffPackageName := s"media-service::teamcity::${name.value}",
    riffRaffManifestProjectName := s"${riffRaffPackageName.value}",
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffPackageType := (packageBin in Debian).value,
    riffRaffArtifactResources := Seq(
      (packageBin in Debian ).value -> s"${name.value}/${name.value}.deb",
      baseDirectory.value / "conf/riff-raff.yaml" -> "riff-raff.yaml"
    ),
    debianPackageDependencies := Seq("openjdk-8-jre-headless"),
    maintainer := "Editorial Tools <digitalcms.dev@guardian.co.uk>",
    packageSummary := "S3 Uploader",
    packageDescription := """Allow uploading images to S3""",

    javaOptions in Universal ++= Seq(
      "-Dpidfile.path=/dev/null"
    )
  )


