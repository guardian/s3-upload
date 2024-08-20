import com.gu.pandomainauth.PublicSettings
import controllers.{Application, AssetsComponents, Management}
import lib.{S3Actions, S3UploadAppConfig}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import play.filters.cors.{CORSComponents}
import router.Routes
import com.gu.permissions.PermissionsProvider
import com.gu.permissions.PermissionsConfig


class AppComponents(context: Context) extends BuiltInComponentsFromContext(context: Context) with HttpFiltersComponents with AssetsComponents with CORSComponents {

  val s3Actions = new S3Actions()

  val publicSettings = new PublicSettings(s"${S3UploadAppConfig.domain}.settings.public", "pan-domain-auth-settings", S3UploadAppConfig.s3Client)

  publicSettings.start()

  val permissions = {
    val permissionsStage = S3UploadAppConfig.stage match {
      case "PROD" => "PROD"
      case _ => "CODE"
    }
    PermissionsProvider(PermissionsConfig(
      stage = permissionsStage,
      region = S3UploadAppConfig.region,
      awsCredentials = S3UploadAppConfig.awsCredentials)
    )
  }

  val api = new Application(s3Actions, publicSettings, permissions, controllerComponents)
  val management = new Management(controllerComponents)

  val disabledFilters: Set[EssentialFilter] = Set(allowedHostsFilter)
  final override def httpFilters: Seq[EssentialFilter] = corsFilter +: super.httpFilters.filterNot(disabledFilters.contains)

  override def router: Router = new Routes(httpErrorHandler, api, assets, management)
}
