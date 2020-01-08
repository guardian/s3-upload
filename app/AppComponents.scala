import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.pandomainauth.PublicSettings
import controllers.{Application, AssetsComponents, Management}
import lib.{Config, S3Actions}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSConfig.Origins
import play.filters.cors.{CORSComponents, CORSConfig, CORSFilter}
import router.Routes


class AppComponents(context: Context) extends BuiltInComponentsFromContext(context: Context) with HttpFiltersComponents with AssetsComponents with CORSComponents {


  val s3Actions = new S3Actions()

  val publicSettings = new PublicSettings(s"${Config.domain}.settings.public", "pan-domain-auth-settings", Config.s3Client)

  publicSettings.start()

  val api = new Application(s3Actions, publicSettings, controllerComponents)
  val management = new Management(controllerComponents)

  override lazy val corsConfig: CORSConfig = CORSConfig.fromConfiguration(configuration).copy(allowedOrigins = Origins.Matching(_ == Config.chartsToolOrigin))

  val disabledFilters: Set[EssentialFilter] = Set(allowedHostsFilter)
  final override def httpFilters: Seq[EssentialFilter] = corsFilter +: super.httpFilters.filterNot(disabledFilters.contains)

  override def router: Router = new Routes(httpErrorHandler, api, assets, management)

}
