package controllers

import anorm.{Id, NotAssigned, Pk}
import jp.t2v.lab.play2.auth.{AuthElement, OptionalAuthElement}
import models.security.{Administrator, NormalUser}
import models.{Membership, Participation, User}
import play.api.data.Form
import play.api.data.Forms.{boolean, ignored, longNumber, mapping, nonEmptyText, optional, text}
import play.api.mvc._
import util.AuthHelper._
import util.GravatarHelper

object Users extends Controller with OptionalAuthElement with AuthConfigImpl {

  def list = StackAction { implicit request =>
    val usersToList = User.findAll()
    Ok(views.html.users.list(usersToList))
  }

  def show(id: Long) = StackAction { implicit request =>
    val userToShow = User.find(id)
    Ok(views.html.users.show(userToShow))
  }
}

object UsersSecured extends Controller with AuthElement with AuthConfigImpl {

  def createForm = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
    Ok(views.html.users.edit(userCreateForm))
  }

  def createMemberForm(groupId: Long) = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
    Ok(views.html.users.edit(userCreateForm, groupId = Option(groupId)))
  }

  def createGuestForm(eventId: Long) = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
    Ok(views.html.users.edit(userCreateForm, eventId = Option(eventId)))
  }


  def updateForm(id: Long) = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
    val userToUpdate = User.find(id)
    Ok(views.html.users.edit(userUpdateForm.fill(userToUpdate), idToUpdate = Option(id)))
  }

  def create = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
      userCreateForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.users.edit(formWithErrors)),
        user => {
          User.create(user)
          Redirect(routes.Users.list())
        }
      )
  }

  def createMember(groupId: Long) = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
      userCreateForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.users.edit(formWithErrors)),
        user => {
          Membership.create(groupId = groupId, userId = User.create(user))
          Redirect(routes.Groups.show(groupId))
        }
      )
  }

  def createGuest(eventId: Long) = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
      userCreateForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.users.edit(formWithErrors)),
        user => {
          Participation.createGuest(eventId = eventId, userId = User.create(user))
          Redirect(routes.Events.show(eventId))
        }
      )
  }

  def update(id: Long) = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    implicit val loggedInUser = Option(loggedIn)
    val whatever = userUpdateForm.bindFromRequest
    whatever.fold(
        formWithErrors => BadRequest(views.html.users.edit(formWithErrors, Option(id))),
        user => {
          User.update(id, user)
          Redirect(routes.Users.show(id))
        }
      )
  }

  def delete(id: Long) = StackAction(AuthorityKey -> hasPermission(Administrator)_) { implicit request =>
    User.delete(id)
    Redirect(routes.Users.list())
  }

  val userCreateForm: Form[User] = Form(
    mapping(
      "id" -> ignored(NotAssigned:Pk[Long]),
      "firstName" -> nonEmptyText(maxLength = 127),
      "lastName" -> nonEmptyText(maxLength = 127),
      "email" -> play.api.data.Forms.email.verifying("Epostadressen används av någon annan", User.findByEmail(_).isEmpty),
      "phone" -> text(maxLength = 127),
      "comment" -> text(maxLength = 127),
      "administrator" -> boolean,
      "password" -> optional(text(maxLength = 127))
    )(toUser)(fromUser)
      .verifying("Administratörer måste ha ett lösenord på minst 8 tecken", user => user.password.length >= 8)
  )

  val primaryKey = optional(longNumber).transform(
    (optionLong: Option[Long]) =>
      if (optionLong.isDefined) {
        Id(optionLong.get)
      } else {
        NotAssigned:Pk[Long]
      },
    (pkLong: Pk[Long]) =>
      pkLong.toOption)


  val userUpdateForm: Form[User] = Form(
    mapping(
      "id" -> primaryKey,
      "firstName" -> nonEmptyText(maxLength = 127),
      "lastName" -> nonEmptyText(maxLength = 127),
      "email" -> play.api.data.Forms.email,
      "phone" -> text(maxLength = 127),
      "comment" -> text(maxLength = 127),
      "administrator" -> boolean,
      "password" -> optional(text(maxLength = 127))
    )(toUser)(fromUser)
      .verifying("Epostadressen används av någon annan", user => User.verifyUniqueEmail(user))
      .verifying("Administratörer måste ha ett lösenord på minst 8 tecken", user => user.password.length >= 8)
  )

  def toUser(id: Pk[Long], firstName: String, lastName: String, email: String, phone: String, comment: String, isAdministrator: Boolean, password: Option[String]): User = {
    val permission = isAdministrator match {
      case true => Administrator
      case _ => NormalUser
    }
    val passwordToSet = permission match {
      case Administrator => password.getOrElse("").trim
      case _ => User.NOT_CHANGED_PASSWORD
    }
    User(id=id, firstName=firstName, lastName=lastName, email=email, phone=phone, comment=comment, permission=permission, password=passwordToSet, imageUrl = GravatarHelper.gravatarParametrizedUrl(email))
  }

  def fromUser(user: models.User) = {
    val passwordToShow: Option[String]  = user.permission match {
      case Administrator => Option(User.NOT_CHANGED_PASSWORD)
      case _ => None
    }
    Option(user.id, user.firstName, user.lastName, user.email, user.phone, user.comment, user.permission==Administrator, passwordToShow)
  }
}

