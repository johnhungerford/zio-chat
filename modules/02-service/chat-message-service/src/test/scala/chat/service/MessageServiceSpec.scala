package chat.service

import zio.* 
import zio.test.*
import zio.direct.*

import chat.model.DomainGenerators.*
import chat.model.Message
import chat.model.UserTypes.*
import chat.model.MessageTypes.*
import zio.prelude.NonEmptySet

abstract class MessageServiceSpec[E](
    impl: String,
    layer: ZLayer[Any, E, MessageService],
) extends ZIOSpecDefault:
    def messageList(length: Int): ZIO[Any, Nothing, List[Message]] =
        messageGen
            .sample
            .forever
            .take(length)
            .runCollect
            .map(_.toList.map(_.value))

    override def spec: Spec[TestEnvironment & Scope, Any] = suite(s"MessageServiceSpec: $impl")(
        test("all messages submitted should be retrieved when request parameters are empty, in reverse of the order submitted") {
            defer {
                val messages = messageList(100).run

                val service = ZIO.service[MessageService].provide(layer).run

                val submittedMessages = ZIO.foreach(messages)(msg => {
                    service.submitMessage(msg).map(ts => msg.submitted(ts))
                }).run

                val retrievedMessages =
                    service.getMessages(Some(10000), None, None, None, None).run

                assertTrue(
                    submittedMessages.map(_.timestamp).distinct.length == submittedMessages.length,
                    retrievedMessages == submittedMessages.reverse,
                )
            }            
        } @@ TestAspect.withLiveClock,
        test("messages sent by a user should be retrieved in reverse order of submission when querying by that user, and should exclude messages sent by other users") {
            defer {
                val (user1, user2, user3) =
                    (UserHandle("user-1"), UserHandle("user-2"), UserHandle("user-3"))

                val user1Messages = messageList(30).run.map(_.copy(sender = user1))
                val user2Messages = messageList(30).run.map(_.copy(sender = user2))
                val user3Messages = messageList(30).run.map(_.copy(sender = user3))
                val allMessages = user3Messages ++ user1Messages.zip(user2Messages).flatMap(_.toList)

                val service = ZIO.service[MessageService].provide(layer).run

                ZIO.foreach(allMessages)(service.submitMessage).run

                val retrievedMessages =
                    service.getMessages(Some(10000), None, None, None, Some(user1)).run

                assertTrue(
                    retrievedMessages.length == user1Messages.length,
                    user1Messages == retrievedMessages.reverse.map(_.message),
                )
            }
        } @@ TestAspect.withLiveClock,
        test("messages sent in a given channel should be retrieved in reverse order of submission when querying by that channel, and should exclude messages sent in other channels") {
            defer {
                val user1 = UserHandle("user-1")
                val user2 = UserHandle("user-2")
                val user3 = UserHandle("user-3")
                val user4 = UserHandle("user-4")

                val channel1Recipients = NonEmptySet(user1, user2)
                val channel2Recipients = NonEmptySet(user2, user4)
                val channel3Recipients = NonEmptySet(user1, user2, user3, user4)

                val channel1user1Messages = messageList(15).run.map(_.copy(recipients = channel1Recipients, sender = user1))
                val channel1user2Messages = messageList(15).run.map(_.copy(recipients = channel1Recipients, sender = user2))
                val channel1Messages = channel1user1Messages.zip(channel1user2Messages).flatMap(_.toList)

                val channel2User2Messages = messageList(15).run.map(_.copy(recipients = channel2Recipients, sender = user2))
                val channel2User4Messages = messageList(15).run.map(_.copy(recipients = channel2Recipients, sender = user4))
                val channel2Messages = channel2User2Messages.zip(channel2User4Messages).flatMap(_.toList)

                val channel3User1Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user1))
                val channel3User2Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user2))
                val channel3User3Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user3))
                val channel3User4Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user4))
                val channel3Messages = channel3User1Messages.zip(channel3User2Messages).flatMap(_.toList)
                    .zip(channel3User2Messages.zip(channel3User4Messages).flatMap(_.toList))
                    .flatMap(_.toList)

                val allMessages = channel3Messages ++ channel1Messages.zip(channel2Messages).flatMap(_.toList)

                val service = ZIO.service[MessageService].provide(layer).run

                ZIO.foreach(allMessages)(service.submitMessage).run

                val retrievedMessages =
                    service.getMessages(Some(10000), None, None, Some(Channel(channel1Recipients)), None).run

                assertTrue(
                    retrievedMessages.length == channel1Messages.length,
                    channel1Messages == retrievedMessages.reverse.map(_.message),
                )
            }
        } @@ TestAspect.withLiveClock,
        test("messages sent in a given channel by a given user should be retrieved in reverse order of submission when querying by that channel and user, and should exclude messages sent in other channels or in the same channel by other users") {
            defer {
                val user1 = UserHandle("user-1")
                val user2 = UserHandle("user-2")
                val user3 = UserHandle("user-3")
                val user4 = UserHandle("user-4")

                val channel1Recipients = NonEmptySet(user1, user2)
                val channel2Recipients = NonEmptySet(user2, user4)
                val channel3Recipients = NonEmptySet(user1, user2, user3, user4)

                val channel1user1Messages = messageList(15).run.map(_.copy(recipients = channel1Recipients, sender = user1))
                val channel1user2Messages = messageList(15).run.map(_.copy(recipients = channel1Recipients, sender = user2))
                val channel1Messages = channel1user1Messages.zip(channel1user2Messages).flatMap(_.toList)

                val channel2User2Messages = messageList(15).run.map(_.copy(recipients = channel2Recipients, sender = user2))
                val channel2User4Messages = messageList(15).run.map(_.copy(recipients = channel2Recipients, sender = user4))
                val channel2Messages = channel2User2Messages.zip(channel2User4Messages).flatMap(_.toList)

                val channel3User1Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user1))
                val channel3User2Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user2))
                val channel3User3Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user3))
                val channel3User4Messages = messageList(8).run.map(_.copy(recipients = channel3Recipients, sender = user4))
                val channel3Messages = channel3User1Messages.zip(channel3User2Messages).flatMap(_.toList)
                    .zip(channel3User2Messages.zip(channel3User4Messages).flatMap(_.toList))
                    .flatMap(_.toList)

                val allMessages = channel3Messages ++ channel1Messages.zip(channel2Messages).flatMap(_.toList)

                val service = ZIO.service[MessageService].provide(layer).run

                ZIO.foreach(allMessages)(service.submitMessage).run

                val retrievedMessages =
                    service.getMessages(Some(10000), None, None, Some(Channel(channel1Recipients)), Some(user2)).run

                assertTrue(
                    retrievedMessages.length == channel1user2Messages.length,
                    channel1user2Messages == retrievedMessages.reverse.map(_.message),
                )
            }
        } @@ TestAspect.withLiveClock,
        test("offset parameter should allow paging through messages") {
            defer {
                val messages = messageList(100).run

                val service = ZIO.service[MessageService].provide(layer).run

                val submittedMessages = ZIO.foreach(messages)(msg => {
                    service.submitMessage(msg).map(ts => msg.submitted(ts))
                }).run

                val retrievedMessages1 =
                    service.getMessages(Some(25), None, None, None, None).run
                val retrievedMessages2 =
                    service.getMessages(Some(25), Some(25), None, None, None).run
                val retrievedMessages3 =
                    service.getMessages(Some(25), Some(50), None, None, None).run
                val retrievedMessages4 =
                    service.getMessages(Some(25), Some(75), None, None, None).run

                assertTrue(
                    retrievedMessages1 ++ retrievedMessages2 ++ retrievedMessages3 ++ retrievedMessages4 == submittedMessages.reverse,
                )
            }
        } @@ TestAspect.withLiveClock,
        test("timestamp parameter should specify the *most recent* timestamp of the retrieved messages") {
            defer {
                val messages1 = messageList(50).run
                val messages2 = messageList(50).run
                val messages = messages1 ++ messages2

                val middleMessage = messages2.head

                val service = ZIO.service[MessageService].provide(layer).run

                val submittedMessages = ZIO.foreach(messages)(msg => {
                    service.submitMessage(msg).map(ts => msg.submitted(ts))
                }).run

                val middleTs =
                    submittedMessages.find(_.message == middleMessage).map(_.timestamp).get

                val retrievedMessages =
                    service.getMessages(Some(10000), None, Some(middleTs), None, None).run

                Console.printLine(retrievedMessages.length).run

                import math.Ordered.orderingToOrdered
                assertTrue(
                    retrievedMessages.forall(_.timestamp <= middleTs),
                    retrievedMessages.map(_.message) == middleMessage :: messages1.reverse,
                )
            }
        } @@ TestAspect.withLiveClock,
    )
