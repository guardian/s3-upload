import play.api.GlobalSettings
import play.api.mvc.WithFilters
import play.filters.csrf.CSRFFilter

object Global extends WithFilters(CSRFFilter()) with GlobalSettings
