package lib


import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, AuthenticationStatus, User}
import com.gu.pandomainauth.{PanDomain, PublicKey, PublicSettings}
import play.api.Logging
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

trait PandaController extends BaseControllerHelpers with Logging {
  def publicSettings: PublicSettings

  def unauthorisedResponse[A](request: Request[A]) = {
    Future.successful(Unauthorized(views.html.login(Config.loginUri)(request)))
  }

  def authStatus(cookie: Cookie, publicKey: PublicKey): AuthenticationStatus = {
    PanDomain.authStatus(
      cookie.value,
      publicKey,
      PanDomain.guardianValidation,
      apiGracePeriod = 0,
      system = "s3-upload",
      cacheValidation = false
    )
  }

  object AuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = PandaController.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = PandaController.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {

      publicSettings.publicKey match {
        case Some(pk) =>

          request.cookies.get("gutoolsAuth-assym") match {
            case Some(cookie) =>
              authStatus(cookie, pk) match {
                case Authenticated(AuthenticatedUser(user, _, _, _, _)) =>
                  block(new UserRequest(user, request))

                case other =>
                  logger.info(s"Login response $other")
                  unauthorisedResponse(request)
              }

            case None =>
              logger.warn("Panda cookie missing")
              unauthorisedResponse(request)
          }

        case None =>
          logger.error("Panda public key unavailable")
          unauthorisedResponse(request)
      }
    }
  }
}