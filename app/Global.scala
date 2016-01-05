import lib.PanAuthenticationSettings
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    PanAuthenticationSettings.publicSettings.start()
  }
}
