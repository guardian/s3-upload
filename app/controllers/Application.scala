package controllers

import java.io.File

import lib.{PanAuthentication, S3Actions}
import play.api.mvc._

object Application extends Controller {
  def index = PanAuthentication { request => {
      Ok(views.html.index(request.user)(request))
    }
  }

  def upload = PanAuthentication { request => {
      Redirect(routes.Application.index())
    }
  }

  private def bytesToMb (bytes: Long): Long = bytes / 1024 / 1024

  def uploadFile = PanAuthentication (parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>
    request.body match {
      case Left(MaxSizeExceeded(limit)) => {
        EntityTooLarge(views.html.tooLarge(request.user, bytesToMb(limit)))
      }

      case Right(multipartForm) => {
        multipartForm.file("files").map { f =>
          val file = new File(s"/tmp/${f.filename}")
          f.ref.moveTo(file)

          S3Actions.upload(file, request.user).map(s3Upload => {
            file.delete()
            Ok(views.html.uploaded(request.user, s3Upload)(request))
          }).getOrElse({
            file.delete()
            Ok(views.html.duplicate(request.user, file)(request))
          })
        }.getOrElse {
          BadRequest(views.html.index(request.user)(request))
        }
      }
    }
  }
}

