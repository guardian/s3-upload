name := "s3-uploader"
version := "1.0"
scalaVersion := "2.11.6"

def env(key: String): Option[String] = Option(System.getenv(key))

libraryDependencies ++= Seq(
  cache, ws, filters,
  "com.amazonaws" % "aws-java-sdk-s3" % "1.10.44",
  "com.gu" %% "pan-domain-auth-verification" % "0.2.10"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging)
  .settings(
    riffRaffPackageName := s"media-service::teamcity::${name.value}",
    riffRaffManifestProjectName := s"${riffRaffPackageName.value}",
    riffRaffBuildIdentifier :=  env("BUILD_NUMBER").getOrElse("DEV"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestVcsUrl := "git@github.com:guardian/s3-upload.git",
    riffRaffManifestBranch := env("GIT_BRANCH").getOrElse("DEV"),
    riffRaffPackageType := (packageBin in Debian).value,
    riffRaffArtifactResources := Seq(
      (packageBin in Debian ).value -> s"${name.value}/${name.value}.deb",
      baseDirectory.value / "conf/riff-raff.yaml" -> "riff-raff.yaml"
    ),
    debianPackageDependencies := Seq("openjdk-8-jre-headless"),
    maintainer := "Editorial Tools <digitalcms.dev@guardian.co.uk>",
    packageSummary := "S3 Uploader",
    packageDescription := """Allow uploading images to S3"""
  )

