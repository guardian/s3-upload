import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.pandomainauth.PublicSettings
import controllers.{Api, AssetsComponents, Management}
import lib.{Config, S3Actions}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import router.Routes

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context: Context) with HttpFiltersComponents with AssetsComponents {
  val s3Client = AmazonS3ClientBuilder.standard().withRegion(Config.region).withCredentials(Config.awsCredentials).build()
  val s3Actions = new S3Actions(s3Client)

  val publicSettings = new PublicSettings(s"${Config.domain}.settings.public", "pan-domain-auth-settings", s3Client)

  publicSettings.start()

  val api = new Api(s3Actions, publicSettings, controllerComponents)
  val management = new Management(controllerComponents)

  val disabledFilters: Set[EssentialFilter] = Set(allowedHostsFilter)
  final override def httpFilters: Seq[EssentialFilter] = super.httpFilters.filterNot(disabledFilters.contains)

  override def router: Router = new Routes(httpErrorHandler, api, assets, management)
}
