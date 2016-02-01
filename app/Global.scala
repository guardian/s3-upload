import lib.PanAuthenticationSettings
import play.api.mvc.WithFilters
import play.api.{Application, GlobalSettings}
import play.filters.csrf.CSRFFilter

object Global extends WithFilters(CSRFFilter()) with GlobalSettings {
  override def onStart(app: Application) {
    PanAuthenticationSettings.publicSettings.start()
  }
}
