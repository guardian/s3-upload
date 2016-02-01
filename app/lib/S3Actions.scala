package lib

import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult, AmazonS3Exception}

case class S3Upload(url: URI)

object S3Upload {
  def build(putObjectRequest: PutObjectRequest) = {
    val uri = Config.stage match {
      case "PROD" => s"https://uploads.guim.co.uk/${putObjectRequest.getKey}"
      case _ => s"https://${putObjectRequest.getBucketName}.s3.amazonaws.com/${putObjectRequest.getKey}"
    }

    S3Upload(new URI(uri))
  }
}

object S3Actions {
  val s3Client = new AmazonS3Client(Config.awsCredentials)

  def upload(file: File): Option[S3Upload] = {
    val key = getS3Key(file)

    getObject(key) match {
      case None => {
        val request = new PutObjectRequest(Config.bucketName, key, file)

        s3Client.putObject(request) match {
          case _: PutObjectResult   => Some(S3Upload.build(request))
          case _                    => None
        }
      }
      case _ => None
    }
  }

  def getObject (key: String) = {
    try {
      s3Client.getObject(Config.bucketName, key)
    } catch {
      case e: AmazonS3Exception if e.getStatusCode.equals(403) => {
        println("object not found")
        None
      }
    }
  }

  private def getCurrentDate = {
    val now = Calendar.getInstance().getTime
    val dateFormat = new SimpleDateFormat("yyyy/MM/dd")
    dateFormat.format(now)
  }

  private def getS3Key (file: File) = {
    s"$getCurrentDate/${file.getName.replace(' ', '_')}"
  }
}

