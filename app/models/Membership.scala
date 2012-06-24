package models

import anorm.SqlParser._
import play.api.db.DB
import anorm._
import play.api.Play.current

case class Membership(id: Pk[Long] = NotAssigned,
                      group: Group,
                      user: User)

object Membership {

  def create(membership: Membership) {
    DB.withConnection {
      implicit connection =>
        SQL(insertQueryString).on(
          'group -> membership.group.id,
          'user -> membership.user.id
        ).executeUpdate()
    }
  }

  val insertQueryString =
    """
INSERT INTO memberships (
  groupx,
  userx
) VALUES (
  {group},
  {user}
)
    """

  val parser = {
    get[Pk[Long]]("id") ~
    get[Long]("groupx") ~
    get[Long]("userx") map {
    case id ~ groupx ~ userx  =>
      Membership(
        id = id,
        group = Group.find(groupx),
        user = User.find(userx)
      )
    }
  }

  def find(id: Long): Membership = {
    DB.withConnection {
      implicit connection =>
        SQL("SELECT * FROM memberships WHERE id={id}").on('id -> id).as(Membership.parser single)
    }
  }

  def findAll(): Seq[Membership] = {
    DB.withConnection {
      implicit connection =>
        SQL("SELECT * FROM memberships").as(parser *)
    }
  }

  def findMembers(group: Group): Seq[Membership] = {
    DB.withConnection {
      implicit connection =>
        SQL("SELECT m.* FROM memberships m, users u WHERE m.userx=u.id AND m.groupx={groupId} ORDER BY u.first_name, u.last_name").on('groupId -> group.id.get).as(parser *)
    }
  }

  def delete(id: Long) {
    DB.withConnection {
      implicit connection => {
        SQL("DELETE FROM memberships m WHERE m.id={id}").on('id -> id).executeUpdate()
      }
    }

  }


}
