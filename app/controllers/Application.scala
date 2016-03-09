package controllers

import java.io.File
import lib._
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
        val uploads : Seq[S3UploadResponse] = multipartForm.files.map { f =>
          val file = new File(s"/tmp/${f.filename}")
          f.ref.moveTo(file)
          val res = S3Actions.upload(file, request.user)
          file.delete()
          res
        }

        Ok(views.html.uploaded(request.user, uploads)(request))
      }
    }
  }
}

