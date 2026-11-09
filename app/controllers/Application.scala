package controllers

import java.io.File
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.concurrent.Future
import org.apache.pekko.stream.Materializer
import com.gu.pandomainauth.PublicSettings
import lib._
import play.api.mvc.{Action, AnyContent, ControllerComponents, MaxSizeExceeded, MultipartFormData}
import play.api.libs.json._
import com.gu.permissions.PermissionsProvider

import java.util.zip.ZipException
import scala.util.control.NonFatal

class Application(
  s3Actions: S3Actions,
  override val publicSettings: PublicSettings,
  override val permissions: PermissionsProvider,
  override val controllerComponents: ControllerComponents
)(implicit mat: Materializer) extends PandaController {

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

          val res = s3Actions.upload(temporaryFilePath.toFile, request.user, ChartsToolConfig, ChartsToolConfig.setPublicAcl)
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

  def interactive: Action[AnyContent] = AuthAction { request =>
    Ok(views.html.interactive(request.user, None)(request))
  }

  val INTERACTIVE_ZIP_INPUT_NAME = "zipfile" // must match what's used in interactive.scala.html
  def uploadInteractive = AuthAction.async(parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>

    def badRequestError(message: String) = BadRequest(views.html.interactive(request.user, Some(message))(request))

    Future {
      request.body match {
        case Left(MaxSizeExceeded(limit)) =>
          EntityTooLarge(views.html.interactive(request.user, Some(s"ZIP must be smaller than ${bytesToMb(limit)} MB."))(request))
        case Right(form) =>
          form.files match {
            case Seq(file) if file.key == INTERACTIVE_ZIP_INPUT_NAME && file.filename.toLowerCase().endsWith(".zip") =>
              val projectName = form.dataParts.get("projectName").flatMap(_.headOption).filter(_.trim.nonEmpty).getOrElse(file.filename)
              try {
                val published = s3Actions.uploadInteractiveZip(file.ref.path, projectName, request.user.email, InteractiveUploaderConfig, controllerComponents.fileMimeTypes)
                Ok(views.html.uploaded(request.user, Seq(published))(request))
              } catch {
                case e: ZipException => badRequestError(e.getMessage)
                case NonFatal(e) =>
                  logger.error("Unable to publish interactive", e)
                  InternalServerError(views.html.interactive(request.user, Some("Upload failed. Please try again; if it persists, contact the tools team."))(request))
              } finally {
                file.ref.delete()
              }
            case _ => badRequestError("Choose one ZIP file containing your interactive.")
          }
      }
    }(controllerComponents.executionContext)
  }
}

