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

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JavaAppPackaging, RiffRaffArtifact, UniversalPlugin, JDebPackaging)
  .settings(
    riffRaffPackageName := s"media-service::${name.value}",
    riffRaffManifestProjectName := s"media-service::teamcity::${riffRaffPackageName.value}",
    riffRaffBuildIdentifier :=  env("BUILD_NUMBER").getOrElse("DEV"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestVcsUrl := "git@github.com:guardian/s3-upload.git",
    riffRaffManifestBranch := env("GIT_BRANCH").getOrElse("DEV"),
    riffRaffPackageType := (packageBin in Debian).value
  )

