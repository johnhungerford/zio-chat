package chat.service

import chat.model.*
import chat.model.UserTypes.*
import chat.model.DomainError.*
import chat.model.MessageTypes.*
import Event.*

import zio.* 
import zio.stream.*
import zio.direct.*

trait UserSubscriptionService:
    def userSubscription(user: UserHandle): ZIO[Scope, IOError, EventStreamOutput]

object UserSubscriptionService:
    lazy val live = ZLayer.fromZIO(defer {
        val eventStream = ZIO.service[EventStream].run
        val messageService = ZIO.service[MessageService].run
        val eventStreamOutput =
            eventStream.subscribe(SubscriptionId("user-subscription-service")).run
        val hub = Hub.bounded[Event](2048).run
        val hubSink = ZSink.fromQueue(hub)
        eventStreamOutput.streamEvents.run(hubSink).forkScoped.run
        HubUserSubscriptionService(hub, messageService)
    })

case class HubUserSubscriptionService(hub: Hub[Event], messageService: MessageService)
    extends UserSubscriptionService:
        private def userEventPredicate(user: UserHandle, channels: Set[Channel], selfAndContacts: Set[UserHandle])(event: Event): Boolean = event match
            case MessageSubmission(msg: SubmittedMessage) if (msg.channel.contains(user)) => true 
            case MessageReceived(_, _, channel: Channel) if (channels.contains(channel)) => true 
            case MessageRead(_, _, channel: Channel) if (channels.contains(channel)) => true 
            case UserUpdated(updates: User) if (selfAndContacts.contains(updates.handle)) => true
            case UserRemoved(userHandle: UserHandle) if (selfAndContacts.contains(userHandle)) => true 
            case UserConnected(userHandle: UserHandle) if (selfAndContacts.contains(userHandle)) => true 
            case UserDisconnected(userHandle: UserHandle) if (selfAndContacts.contains(userHandle)) => true 
            case UserInactive(userHandle: UserHandle) if (selfAndContacts.contains(userHandle)) => true
            case _ => false

        private def constructEventStreamOutput(user: UserHandle, dequeue: Dequeue[Event], predicate: Event => Boolean): EventStreamOutput =
            new EventStreamOutput:
                override def takeEvent: ZIO[Any, IOError, Event] =
                    dequeue.take.repeatUntil(predicate)

                override def takeEvents(n: Int): ZIO[Any, IOError, List[Event]] = defer {
                    val collectedEventsRef = Ref.make[List[Event]](Nil).run
                    while(collectedEventsRef.get.run.length < n) {
                        val nextEvent = takeEvent.run
                        collectedEventsRef.update(nextEvent :: _).run
                    }
                    collectedEventsRef.get.run
                }

                override def streamEvents: ZStream[Any, IOError, Event] =
                    ZStream.fromQueue(dequeue).filter(predicate)

        override def userSubscription(user: UserHandle): ZIO[Scope, IOError, EventStreamOutput] = defer {
            val dequeue = hub.subscribe.run
            val channels = messageService.userChannels(user).run.toSet
            val selfAndContacts = channels.map(_.toSet).reduceOption(_ ++ _).getOrElse(Set.empty)
            val predicate = userEventPredicate(user, channels, selfAndContacts)
            constructEventStreamOutput(user, dequeue, predicate)
        }