package lib


import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, AuthenticationStatus, User}
import com.gu.pandomainauth.service.CryptoConf.Verification
import com.gu.pandomainauth.{PanDomain, PublicSettings}
import play.api.Logging
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import com.gu.permissions.PermissionsProvider
import com.gu.permissions.PermissionDefinition

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

trait PandaController extends BaseControllerHelpers with Logging {
  def publicSettings: PublicSettings

  def permissions: PermissionsProvider

  val S3UploaderAccess = PermissionDefinition("s3_uploader_access", "s3-uploader")

  def unauthenticatedResponse[A](request: Request[A]) = {
    Future.successful(Unauthorized(views.html.login(S3UploadAppConfig.loginUri)(request)))
  }

  def unauthorisedResponse[A](request: Request[A]) = {
    Future.successful(Forbidden(views.html.unauthorised()(request)))
  }

  def authStatus(cookie: Cookie, verification: Verification): AuthenticationStatus = PanDomain.authStatus(
    cookie.value,
    verification,
    PanDomain.guardianValidation,
    apiGracePeriod = 0,
    system = "s3-upload",
    cacheValidation = false,
    forceExpiry = false
  )

  object AuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = PandaController.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = PandaController.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      request.cookies.get("gutoolsAuth-assym") match {
        case Some(cookie) =>
          authStatus(cookie, publicSettings.verification) match {
            case Authenticated(AuthenticatedUser(user, _, _, _, _)) =>
              if (!permissions.hasPermission(S3UploaderAccess, user.email)) {
                logger.error(s"User ${user.email} does not have ${S3UploaderAccess.name} permission")
                unauthorisedResponse(request)
              } else {
                block(new UserRequest(user, request))
              }

            case other =>
              logger.info(s"Login response $other")
              unauthenticatedResponse(request)
          }

        case None =>
          logger.warn("Panda cookie missing")
          unauthenticatedResponse(request)
      }
    }
  }
}
