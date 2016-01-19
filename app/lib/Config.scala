package lib

import java.net.URI

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain

object Config {
  val properties = Properties.fromPath("/etc/gu/s3-uploader.properties")

  val awsCredentials = new DefaultAWSCredentialsProviderChain()

  val bucketName = properties("s3.bucket")

  val domain = properties("panda.domain")

  val baseLoginUri = new URI(s"https://login.$domain/login?returnUrl=")

  val loginUri = new URI(s"https://login.$domain/login?returnUrl=https://s3-uploader.$domain")
}
