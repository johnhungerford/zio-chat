package chat.service

import zio.*
import chat.model.UserTypes.* 
import chat.model.User
import chat.model.DomainError.IOError
import zio.direct.*

enum UserServiceError:
    case UserNotFound(handle: UserHandle)
    case UserAlreadyExists(handle: UserHandle)

import UserServiceError.*

trait UserService:
    def getUser(handle: UserHandle): ZIO[Any, UserNotFound | IOError, User]
    def addUser(user: User): ZIO[Any, UserAlreadyExists | IOError, Unit]
    def updateUser(user: User): ZIO[Any, UserNotFound | IOError, Unit]
    def removeUser(handle: UserHandle): ZIO[Any, UserNotFound | IOError, Unit]

case class InMemoryUserService(
    repositoryRef: Ref[Map[UserHandle, User]]
) extends UserService:
    override def getUser(handle: UserHandle): ZIO[Any, UserNotFound, User] = defer {
        val repo = repositoryRef.get.run
        repo.get(handle) match
            case None => ZIO.fail[UserNotFound](UserNotFound(handle)).run
            case Some(value) => ZIO.succeed(value).run
        
    }

    override def addUser(user: User): ZIO[Any, UserAlreadyExists, Unit] = defer {
        val userExists = repositoryRef.modify { repo =>
            val userExists = repo.contains(user.handle)
            if (userExists) (userExists, repo)
            else (userExists, repo + (user.handle -> user))
        }.run
        if (userExists) ZIO.fail[UserAlreadyExists](UserAlreadyExists(user.handle)).run
        else ZIO.unit.run
    }

    override def updateUser(user: User): ZIO[Any, UserNotFound, Unit] = defer {
        val userExists = repositoryRef.modify { repo =>
            val userExists = repo.contains(user.handle)
            if (userExists) (userExists, repo + (user.handle -> user))
            else (userExists, repo)
        }.run
        if (!userExists) ZIO.fail[UserNotFound](UserNotFound(user.handle)).run
        else ZIO.unit.run
    }

    override def removeUser(handle: UserHandle): ZIO[Any, UserNotFound, Unit] = defer {
        val userExists = repositoryRef.modify { repo =>
            val userExists = repo.contains(handle)
            if (userExists) (userExists, repo - handle)
            else (userExists, repo)
        }.run
        if (!userExists) ZIO.fail[UserNotFound](UserNotFound(handle)).run
        else ZIO.unit.run
    }

object InMemoryUserService:
    lazy val live = ZLayer.fromZIO(
        Ref.make[Map[UserHandle, User]](Map.empty).map(InMemoryUserService.apply)
    )
