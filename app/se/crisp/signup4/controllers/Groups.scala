package se.crisp.signup4.controllers

import javax.inject.Inject

import jp.t2v.lab.play2.auth.{AuthElement, OptionalAuthElement}
import se.crisp.signup4.models.security.Administrator
import se.crisp.signup4.models.{Event, Group, Membership}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import se.crisp.signup4.util.AuthHelper._

class Groups @Inject() (val messagesApi: MessagesApi) extends Controller with OptionalAuthElement with AuthConfigImpl with I18nSupport{

  def list: Action[AnyContent] = StackAction { implicit request =>
    val groups = Group.findAll()
    Ok(se.crisp.signup4.views.html.groups.list(groups))
  }

  def show(id: Long, showAll: Boolean): Action[AnyContent] = StackAction { implicit request =>
    val group = Group.find(id)
    val members = Membership.findMembers(group)
    if(showAll) {
      Ok(se.crisp.signup4.views.html.groups.show(group, Event.findAllEventsByGroup(group) , members, showingAll = true))
    } else {
      Ok(se.crisp.signup4.views.html.groups.show(group, Event.findFutureEventsByGroup(group), members))
    }
  }
}


class GroupsSecured @Inject() (val messagesApi: MessagesApi) extends Controller with AuthElement with AuthConfigImpl with I18nSupport{


  def createForm: Action[AnyContent] = StackAction(AuthorityKey -> hasPermission(Administrator)) { implicit request =>
    implicit val loggedInUser: Option[User] = Option(loggedIn)
    Ok(se.crisp.signup4.views.html.groups.edit(groupForm))
  }

  def updateForm(id: Long): Action[AnyContent] = StackAction(AuthorityKey -> hasPermission(Administrator)) { implicit request =>
    implicit val loggedInUser: Option[User] = Option(loggedIn)
    val group = Group.find(id)
    Ok(se.crisp.signup4.views.html.groups.edit(groupForm.fill(group), Option(id)))
  }

  def create: Action[AnyContent] = StackAction(AuthorityKey -> hasPermission(Administrator)) { implicit request =>
    implicit val loggedInUser: Option[User] = Option(loggedIn)
      groupForm.bindFromRequest.fold(
        formWithErrors => BadRequest(se.crisp.signup4.views.html.groups.edit(formWithErrors)),
        group => {
          val groupId = Group.create(group)
          Redirect(routes.Groups.show(groupId))
        }
      )
  }

  def update(id: Long): Action[AnyContent] = StackAction(AuthorityKey -> hasPermission(Administrator)) { implicit request =>
    implicit val loggedInUser: Option[User] = Option(loggedIn)
      groupForm.bindFromRequest.fold(
        formWithErrors => BadRequest(se.crisp.signup4.views.html.groups.edit(formWithErrors, Option(id))),
        group => {
          Group.update(id, group)
          Redirect(routes.Groups.show(id))
        }
      )
  }

  def delete(id: Long): Action[AnyContent] = StackAction(AuthorityKey -> hasPermission(Administrator)) { implicit request =>
    implicit val loggedInUser: Option[User] = Option(loggedIn)
    Group.delete(id)
    Redirect(routes.Groups.list())

  }

  val groupForm: Form[Group] = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "name" -> nonEmptyText(maxLength = 127),
      "description" -> text(maxLength = 127),
      "mail_from" -> email,
      "mail_subject_prefix" -> text(maxLength = 127)
    )(Group.apply)(Group.unapply)
  )
}
