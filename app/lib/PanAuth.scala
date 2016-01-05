package lib


import com.gu.pandomainauth.{PublicSettings, PanDomain}
import com.gu.pandomainauth.model.{Authenticated, Expired, User}
import com.ning.http.client.AsyncHttpClient
import controllers.Application._
import dispatch.Http
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{ActionBuilder, Request, Result}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object PanAuthenticationSettings {
  implicit val httpClient = Http(new AsyncHttpClient())

  val publicSettings = new PublicSettings(Config.domain, {
    case Success(settings) =>
      println("successfully updated pan-domain public settings")
    case Failure(err) =>
      println("failed to update pan-domain public settings", err)
  })

  def publicKey: Option[String] = publicSettings.publicKey
}

object PanAuthentication extends ActionBuilder[({ type R[A] = AuthenticatedRequest[A, User] })#R] {
  override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, User]) => Future[Result]): Future[Result] = {
    PanAuthenticationSettings.publicKey match {
      case Some(key) => {
        request.cookies.get(PanAuthenticationSettings.publicSettings.assymCookieName) match {
          case Some(cookie) => {
            PanDomain.authStatus(cookie.value, key) match {
              case user: Authenticated => {
                block(new AuthenticatedRequest(user.authedUser.user, request))
              }
              case _: Expired => Future(Unauthorized(views.html.login(Config.loginUri)))

              case _ => Future(Unauthorized(views.html.login(Config.loginUri)))
            }
          }
          case _ => Future(Unauthorized(views.html.login(Config.loginUri)))
        }
      }
      case _ => Future(Unauthorized(views.html.login(Config.loginUri)))
    }
  }
}
