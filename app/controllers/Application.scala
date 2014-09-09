package controllers

import fi.pyppe.simpleauth.{UserResponse, Auth}
import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action { implicit req =>
    val userJson =
      req.session.get("user.identity").
        flatMap(Cache.getAs[String])
    Ok(views.html.index(userJson))
  }

  def authenticate(provider: String) = Action.async { implicit req =>
    Auth.initialize(provider)
  }

  def authenticateCallback(provider: String) = Action.async { implicit req =>
    Auth.callback(provider) { case UserResponse(user, userJson) =>
      /*
       HERE WE WOULD TYPICALLY SAVE THE USER DETAILS TO A DATABASE
       */
      val identity = user.identity.toString
      // A bit hacky: let's store JSON temporarily in cache so we can show it after redirect
      Cache.set(identity, Json.prettyPrint(userJson), 60*10)

      val session = Session(Seq(
        Some("user.identity" -> user.identity.toString),
        Some("user.name" -> user.name),
        user.email.map(e => "user.email" -> e),
        user.image.map(i => "user.image" -> i)
      ).flatten.toMap)

      Redirect("/").withSession(session)
    }
  }

  def logout = Action { implicit req =>
    Redirect("/").withNewSession.flashing("loggedOut" -> "true")
  }

}