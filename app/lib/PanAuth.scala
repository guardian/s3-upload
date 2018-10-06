package lib


import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser

trait PanAuth extends AuthActions {
  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    (authedUser.user.email endsWith ("@guardian.co.uk")) && authedUser.multiFactor
  }

  override def authCallbackUrl: String = Config.loginUri.toString

  override def domain: String = Config.domain

  override def system: String = "s3-upload"
}
