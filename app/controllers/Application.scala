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

  def uploadFile = PanAuthentication (parse.multipartFormData) { request =>
    request.body.file("files").map { f =>
      val file = new File(s"/tmp/${f.filename}")
      f.ref.moveTo(file)

      S3Actions.upload(file).map(s3Upload => {
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

