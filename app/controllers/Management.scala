package controllers

import play.api.mvc._

object Management extends Controller {
  def healthcheck = Action {
    Ok("OK")
  }
}
