import com.gu.pandomainauth.PublicSettings
import controllers.{Api, AssetsComponents, Management}
import lib.Config
import okhttp3.OkHttpClient
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import router.Routes

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context: Context) with HttpFiltersComponents with AssetsComponents {
  val httpClient = new OkHttpClient()
  val publicSettings = new PublicSettings(Config.domain)(httpClient, executionContext)

  publicSettings.start()

  val api = new Api(publicSettings, controllerComponents)
  val management = new Management(controllerComponents)

  val disabledFilters: Set[EssentialFilter] = Set(allowedHostsFilter)
  final override def httpFilters: Seq[EssentialFilter] = super.httpFilters.filterNot(disabledFilters.contains)

  override def router: Router = new Routes(httpErrorHandler, api, assets, management)
}
