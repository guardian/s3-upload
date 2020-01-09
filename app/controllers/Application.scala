package controllers

import java.io.File
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Calendar

import akka.stream.Materializer
import com.gu.pandomainauth.PublicSettings
import lib._
import play.api.mvc.{ControllerComponents, MaxSizeExceeded}
import play.api.libs.json._

class Application(s3Actions: S3Actions, override val publicSettings: PublicSettings,
                  override val controllerComponents: ControllerComponents)(implicit mat: Materializer) extends PandaController {
  def index = AuthAction { request => {
      Ok(views.html.index(request.user)(request))
    }
  }

  def upload = AuthAction { request => {
      Redirect(routes.Application.index())
    }
  }

  def uploadChart = AuthAction (parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>

    request.body match {
      case Left(MaxSizeExceeded(limit)) => {
        EntityTooLarge(views.html.tooLarge(request.user, bytesToMb(limit)))
      }

      case Right(multipartForm) => {
        val uploads : Seq[S3UploadResponse] = multipartForm.files.map { f =>
          val temporaryFilePath = Paths.get(s"/tmp/${f.filename}")
          f.ref.moveFileTo(temporaryFilePath, replace = true)

          val res = s3Actions.upload(temporaryFilePath.toFile, request.user, Config.interactivesBucketName, Config.s3ClientUS, getChartKey(), setPublicAcl = true)
          Files.delete(temporaryFilePath)
          res
        }

        uploads.head match {
          case failure: S3UploadFailure => InternalServerError(Json.toJson(failure))
          case success: S3UploadSuccess => Ok(Json.toJson(success))
        }
      }
    }
  }

  private def getS3Key(file: File) = {
    s"$getCurrentDate/${file.getName.replace(' ', '_')}"
  }

  private def getChartKey() = {
    val now = Calendar.getInstance().getTime
    val dateFormat = new SimpleDateFormat("MMM/yyyy-MM-dd-hh:mm:ss")
    s"${dateFormat.format(now).toLowerCase}/embed.html"
  }

  private def getCurrentDate = {
    val now = Calendar.getInstance().getTime
    val dateFormat = new SimpleDateFormat("yyyy/MM/dd")
    dateFormat.format(now)
  }

  private def bytesToMb (bytes: Long): Long = bytes / 1024 / 1024

  def uploadFile = AuthAction (parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>

    request.body match {
      case Left(MaxSizeExceeded(limit)) => {
        EntityTooLarge(views.html.tooLarge(request.user, bytesToMb(limit)))
      }

      case Right(multipartForm) => {
        val uploads : Seq[S3UploadResponse] = multipartForm.files.map { f =>
          val temporaryFilePath = Paths.get(s"/tmp/${f.filename}")
          f.ref.moveFileTo(temporaryFilePath, replace = true)

          val res = s3Actions.upload(temporaryFilePath.toFile, request.user, Config.bucketName, Config.s3Client, getS3Key(temporaryFilePath.toFile), setPublicAcl = false)
          Files.delete(temporaryFilePath)
          res
        }

        Ok(views.html.uploaded(request.user, uploads)(request))
      }
    }
  }
}

