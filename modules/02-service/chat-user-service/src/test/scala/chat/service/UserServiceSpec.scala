package chat.service

import zio.test.ZIOSpecDefault

import zio.* 
import zio.test.*
import zio.direct.*

import chat.model.DomainGenerators.*

abstract class UserServiceSpec[E](
    impl: String,
    layer: ZLayer[Any, E, UserService],
) extends ZIOSpecDefault:
    override def spec: Spec[TestEnvironment & Scope, Any] = suite(s"UserServiceSpec: $impl")(
        test("any user added should be retrievable") {
            check(userGen)(user => defer {
                val service = ZIO.service[UserService].provide(layer).run
                service.addUser(user).run
                val retrievedUser = service.getUser(user.handle).run
                assertTrue(retrievedUser == user)
            })
        },
        test("any user added should be removeable") {
            check(userGen)(user => defer {
                val service = ZIO.service[UserService].provide(layer).run
                service.addUser(user).run
                val userExistsAfterAdd = service.getUser(user.handle)
                    .as(true)
                    .catchSome {
                        case UserServiceError.UserNotFound(_) => ZIO.succeed(false)
                    }
                    .run
                service.removeUser(user.handle).run
                val userExistsAfterRemoval = service.getUser(user.handle)
                    .as(true)
                    .catchSome {
                        case UserServiceError.UserNotFound(_) => ZIO.succeed(false)
                    }
                    .run

                assertTrue(userExistsAfterAdd, !userExistsAfterRemoval)
            })
        },
        test("any user added should be updateable") {
            check(userGen, birthDateGen, Gen.option(lastNameGen))((user, newBirthday, newLastName) => defer {
                val updatedUser = user.copy(birthDate = newBirthday, lastName = newLastName)
                val service = ZIO.service[UserService].provide(layer).run
                service.addUser(user).run
                val userAfterAdd = service.getUser(user.handle).run
                service.updateUser(updatedUser).run
                val userAfterUpdate = service.getUser(user.handle).run
                assertTrue(userAfterAdd == user, userAfterUpdate == updatedUser)
            })
        },
        test("calling addUser with an existing user should fail with UserAlreadyExists with that user's handle") {
            check(userGen)(user => defer {
                val service = ZIO.service[UserService].provide(layer).run
                service.addUser(user).run
                val failedAdd = service.addUser(user).flip.run
                assertTrue(failedAdd == UserServiceError.UserAlreadyExists(user.handle))
            })
        },
        test("calling getUser with a non-existent user handle should fail with UserNotFound with that handle") {
            check(userHandleGen)(userHandle => defer {
                val service = ZIO.service[UserService].provide(layer).run
                val failedRetrieval = service.getUser(userHandle).flip.run
                assertTrue(failedRetrieval == UserServiceError.UserNotFound(userHandle))
            })
        },
        test("calling updateUser with a non-existent user should fail with UserNotFound with that user's handle") {
            check(userGen)(user => defer {
                val service = ZIO.service[UserService].provide(layer).run
                val failedUpdate = service.getUser(user.handle).flip.run
                assertTrue(failedUpdate == UserServiceError.UserNotFound(user.handle))
            })
        },
        test("calling removeUser with a non-existent user should fail with UserNotFound with that user's handle") {
            check(userGen)(user => defer {
                val service = ZIO.service[UserService].provide(layer).run
                val failedRemoval = service.removeUser(user.handle).flip.run
                assertTrue(failedRemoval == UserServiceError.UserNotFound(user.handle))
            })
        },
    )

