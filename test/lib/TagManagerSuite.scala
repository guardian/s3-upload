package lib

import play.api.libs.json.Json

class TagManagerSuite extends munit.FunSuite {
  test("reads desks from a Tag Manager search response") {
    val response = Json.parse(
      """{"data": [{"data": {"path": "tracking/commissioningdesk/uk-news", "externalName": "UK News", "id": 1}}]}"""
    )
    assertEquals(TagManager.parseDesks(response), List(CommissioningDesk("tracking/commissioningdesk/uk-news", "UK News")))
  }

  test("only accepts commissioning desk tag paths") {
    assert(CommissioningDesk.isValidId("tracking/commissioningdesk/uk-news"))
    assert(!CommissioningDesk.isValidId("tracking/commissioningdesk/../../x"))
    assert(!CommissioningDesk.isValidId("tracking/commissioningdesk/UK News"))
    assert(!CommissioningDesk.isValidId("tone/news"))
  }
}
