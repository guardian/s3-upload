package lib

import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

case class CommissioningDesk(id: String, name: String)

object CommissioningDesk {
  implicit val format: OFormat[CommissioningDesk] = Json.format[CommissioningDesk]

  // The id is the tag path, which is what interactive-atom-maker expects in an atom's `commissioningDesks`
  val PATH_PREFIX = "tracking/commissioningdesk/"

  def isValidId(id: String): Boolean = id.startsWith(PATH_PREFIX) && id.matches("[a-z0-9/_-]+")
}

class TagManager(ws: WSClient, baseUrl: String)(implicit ec: ExecutionContext) {
  // There are a few hundred desks, so they are all fetched at once rather than searched for
  def listCommissioningDesks(): Future[List[CommissioningDesk]] =
    ws.url(s"$baseUrl/hyper/tags")
      .withQueryStringParameters("query" -> "*", "limit" -> "500", "type" -> "tracking", "subType" -> "commissioningdesk")
      .get()
      .map { response =>
        if (response.status != 200) throw new RuntimeException(s"Tag Manager returned ${response.status}")
        TagManager.parseDesks(response.json)
      }
}

object TagManager {
  def parseDesks(json: JsValue): List[CommissioningDesk] =
    (json \ "data").as[List[JsValue]].map { item =>
      CommissioningDesk((item \ "data" \ "path").as[String], (item \ "data" \ "externalName").as[String])
    }
}
