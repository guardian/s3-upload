package controllers

import org.apache.pekko.stream.Materializer
import com.gu.pandomainauth.PublicSettings
import lib._
import play.api.mvc.{ControllerComponents, MaxSizeExceeded}
import play.api.libs.json._
import com.gu.permissions.PermissionsProvider

class Application(
  s3Actions: S3Actions,
  override val publicSettings: PublicSettings,
  override val permissions: PermissionsProvider,
  override val controllerComponents: ControllerComponents
)(implicit mat: Materializer) extends PandaController {

  def index = AuthAction { request =>
    {
      Ok(views.html.index(request.user)(request))
    }
  }

  def upload = AuthAction { request =>
    {
      Redirect(routes.Application.index)
    }
  }

  def uploadChart = AuthAction(parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>
    request.body match {
      case Left(MaxSizeExceeded(limit)) => {
        EntityTooLarge(views.html.tooLarge(request.user, bytesToMb(limit)))
      }

      case Right(multipartForm) => {
        val uploads = s3Actions.uploadFiles(
          multipartForm.files,
          request.user,
          ChartsToolConfig,
          setPublicAcl = true
        )

        uploads.head match {
          case failure: S3UploadFailure => InternalServerError(Json.toJson(failure))
          case success: S3UploadSuccess => Ok(Json.toJson(success))
        }
      }
    }
  }

  private def bytesToMb(bytes: Long): Long = bytes / 1024 / 1024

  def uploadFile = AuthAction(parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>
    request.body match {
      case Left(MaxSizeExceeded(limit)) => {
        EntityTooLarge(views.html.tooLarge(request.user, bytesToMb(limit)))
      }

      case Right(multipartForm) => {
        val uploads = s3Actions.uploadFiles(
          multipartForm.files,
          request.user,
          S3UploadAppConfig,
          setPublicAcl = false
        )

        Ok(views.html.uploaded(request.user, uploads)(request))
      }
    }
  }
}
