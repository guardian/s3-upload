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
      Redirect(routes.Application.index)
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
          f.ref.moveTo(temporaryFilePath, replace = true)

          val res = s3Actions.upload(temporaryFilePath.toFile, request.user, ChartsToolConfig, setPublicAcl = true)
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


  private def bytesToMb (bytes: Long): Long = bytes / 1024 / 1024

  def uploadFile = AuthAction (parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>
    request.body match {
      case Left(MaxSizeExceeded(limit)) => {
        EntityTooLarge(views.html.tooLarge(request.user, bytesToMb(limit)))
      }

      case Right(multipartForm) => {
        val uploads : Seq[S3UploadResponse] = multipartForm.files.map { f =>
          val temporaryFilePath = Paths.get(s"/tmp/${f.filename}")
          f.ref.moveTo(temporaryFilePath, replace = true)

          val res = s3Actions.upload(temporaryFilePath.toFile, request.user, S3UploadAppConfig, setPublicAcl = false)
          Files.delete(temporaryFilePath)
          res
        }

        Ok(views.html.uploaded(request.user, uploads)(request))
      }
    }
  }
}

