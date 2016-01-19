name := "s3-uploader"

version := "1.0"

lazy val `s3-uploader` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test )

libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.10.44"

libraryDependencies += "com.gu" %% "pan-domain-auth-verification" % "0.2.10"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

enablePlugins(JavaAppPackaging, RiffRaffArtifact, UniversalPlugin)

riffRaffPackageType := (packageZipTarball in Universal).value

def env(key: String): Option[String] = Option(System.getenv(key))
riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV")
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
