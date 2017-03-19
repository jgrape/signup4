package controllers

import jp.t2v.lab.play2.auth.{LoginLogout, OptionalAuthElement}
import models.User
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.ws.WS
import play.api.mvc.{Action, AnyContent, Controller}
import util.ThemeHelper._
import util.WsHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


object FacebookAuth extends Controller with LoginLogout with OptionalAuthElement with AuthConfigImpl {

  private val FACEBOOK_AUTHENTICATION_URL = "https://graph.facebook.com/oauth/authorize"
  private val FACEBOOK_TOKEN_URL = "https://graph.facebook.com/v2.8/oauth/access_token"
  private val FACEBOOK_GET_EMAIL_URL = "https://graph.facebook.com/me"

  import play.api.Play.current

  private lazy val FACEBOOK_CLIENT_ID = play.api.Play.configuration.getString("facebook.client.id")
  private lazy val FACEBOOK_CLIENT_SECRET = play.api.Play.configuration.getString("facebook.client.secret")

  def isConfigured: Boolean = FACEBOOK_CLIENT_ID.isDefined && FACEBOOK_CLIENT_SECRET.isDefined

  def authenticate = Action { implicit request =>
    val randomString = DigestUtils.md5Hex(Math.random().toString)

    val callbackUrl = routes.FacebookAuth.callback().absoluteURL()
    import com.netaporter.uri.dsl._
    val requestAuthenticationTokenUrl = FACEBOOK_AUTHENTICATION_URL.addParams(
      "response_type" -> "code" ::
        "client_id" -> FACEBOOK_CLIENT_ID.get ::
        "redirect_uri" -> callbackUrl ::
        "state" -> randomString ::
        "scope" -> "email" :: Nil
    )

    Redirect(requestAuthenticationTokenUrl)
  }

  def callback(error: Option[String] = None, state: Option[String] = None, code: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    if (code.isDefined) {
      val callbackUrl = routes.FacebookAuth.callback().absoluteURL()
      val access_token = requestAccessToken(code.get, callbackUrl)

      val email = getEmailAddress(access_token)

      val user = User.findByEmail(email)
      if (user.isDefined) {
        gotoLoginSucceeded(user.get.id.get)
      } else {
        val errorMessage = "Det finns ingen användare med epostadressen " + email + " i " + APPLICATION_NAME + "."
        Future.successful(
          Redirect(routes.Application.loginForm()).flashing(("error", errorMessage))
        )
      }
    } else {
      val errorMessage = "Det gick inte att logga in via Facebook: " + error.getOrElse("okänt fel")
      Future.successful(
        Redirect(routes.Application.loginForm()).flashing(("error", errorMessage))
      )
    }
  }

  private def requestAccessToken(code: String, callbackUrl: String): String = {
    val callToFacebook = WS.url(FACEBOOK_TOKEN_URL).withHeaders("Accept" -> "application/json").post(Map(
      "code" -> Seq(code),
      "client_id" -> Seq(FACEBOOK_CLIENT_ID.get),
      "client_secret" -> Seq(FACEBOOK_CLIENT_SECRET.get),
      "redirect_uri" -> Seq(callbackUrl),
      "grant_type" -> Seq("authorization_code"),
      "scope" -> Seq("email")
    )).map { response =>
      onOkResponse(response) {
        (response.json \ "access_token").as[String]
      }
    }

    Await.result(callToFacebook, 60 seconds)
  }

  private def getEmailAddress(access_token: String): String = {
    val callToFacebook = WS.url(FACEBOOK_GET_EMAIL_URL).withHeaders("Accept" -> "application/json").withQueryString(
      "fields" -> "email",
      "return_ssl_resources" -> "",
      "access_token" -> access_token
    ).get().map { response =>
      onOkResponse(response) {
        (response.json \ "email").as[String]
      }
    }

    Await.result(callToFacebook, 60 seconds)
  }
}
