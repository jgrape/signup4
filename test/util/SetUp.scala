package util

import java.util
import models.{Membership, Event, Group, User}
import org.joda.time.DateTime
import collection.JavaConversions._
import TestHelper._

object SetUp {
  def createUsers(userNames: util.List[String]): util.List[User] = {
    userNames map (name => createUser(name))
  }

  def createUser(userName: String): User = {
    val user = User(firstName = userName, lastName = userName, email = userName + "@mailinator.com")
    val id = User.create(user = user)
    User.find(id)
  }

  def createGroup(groupName: String): Group = {
    val group = Group(name = groupName)
    val id = Group.create(group = group)
    Group.find(id)
  }

  def addMembers(group: Group, members: util.List[User]) = {
    members foreach { member => Membership.create(group.id.get, member.id.get)}
  }

  def createMorningEvent(group: Group, eventName: String): Event = {
    val event = Event(group = group, name = eventName, startTime = morningStart, endTime = morningEnd, lastSignUpDate = morningStart)
    val id = Event.create(event = event)
    Event.find(id)
  }
}
