package controllers

import java.io.File
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

import org.apache.pekko.stream.Materializer
import com.gu.pandomainauth.PublicSettings
import lib._
import play.api.mvc.{ControllerComponents, MaxSizeExceeded}
import play.api.libs.json._
import com.gu.permissions.PermissionsProvider

class Application(
  s3Actions: S3Actions,
  interactivePublisher: InteractivePublisher,
  interactiveExecutionContext: ExecutionContext,
  override val publicSettings: PublicSettings,
  override val permissions: PermissionsProvider,
  override val controllerComponents: ControllerComponents
)(implicit mat: Materializer) extends PandaController with Logging {

  def interactive = AuthAction { request =>
    Ok(views.html.interactive(request.user, None)(request))
  }

  def uploadInteractive = AuthAction.async(parse.maxLength(parse.DefaultMaxDiskLength, parse.multipartFormData)) { request =>
    Future {
      def invalid(message: String) = BadRequest(views.html.interactive(request.user, Some(message))(request))
      request.body match {
        case Left(MaxSizeExceeded(limit)) =>
          EntityTooLarge(views.html.interactive(request.user, Some(s"ZIP must be smaller than ${bytesToMb(limit)} MB."))(request))
        case Right(form) =>
          form.files match {
            case Seq(file) if file.key == "zipfile" && file.filename.toLowerCase(java.util.Locale.ROOT).endsWith(".zip") =>
              val project = form.dataParts.get("projectName").flatMap(_.headOption).filter(_.trim.nonEmpty).getOrElse(file.filename)
              try {
                val published = interactivePublisher.publish(file.ref.path, project, request.user.email)
                Ok(views.html.interactiveUploaded(request.user, published)(request))
              } catch {
                case e: InvalidInteractive => invalid(e.getMessage)
                case scala.util.control.NonFatal(e) =>
                  logger.error("Unable to publish interactive", e)
                  InternalServerError(views.html.interactive(request.user, Some("Upload failed. Please try again; if it persists, contact the tools team."))(request))
              } finally file.ref.delete()
            case _ => invalid("Choose one ZIP file containing your interactive.")
          }
      }
    }(interactiveExecutionContext)
  }


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
          val uploadFileName = Paths.get(s"/tmp/${f.filename}").getFileName.toString
          val temporaryFilePath = Files.createTempFile("s3-upload-", ".tmp")
          try {
            f.ref.moveTo(temporaryFilePath, replace = true)
            s3Actions.upload(temporaryFilePath.toFile, uploadFileName, request.user, ChartsToolConfig, setPublicAcl = true)
          } finally {
            Files.deleteIfExists(temporaryFilePath)
          }
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
          val uploadFileName = Paths.get(s"/tmp/${f.filename}").getFileName.toString
          val temporaryFilePath = Files.createTempFile("s3-upload-", ".tmp")
          try {
            f.ref.moveTo(temporaryFilePath, replace = true)
            s3Actions.upload(temporaryFilePath.toFile, uploadFileName, request.user, S3UploadAppConfig, setPublicAcl = false)
          } finally {
            Files.deleteIfExists(temporaryFilePath)
          }
        }

        Ok(views.html.uploaded(request.user, uploads)(request))
      }
    }
  }
}
