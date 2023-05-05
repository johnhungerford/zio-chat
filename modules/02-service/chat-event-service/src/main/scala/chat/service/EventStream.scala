package chat.service

import chat.model.{Event, SubscriptionId}
import chat.model.DomainError.IOError

import zio.* 
import zio.stream.*
import zio.direct.*

trait EventStream:
    def input: ZIO[Scope, IOError, EventStreamInput]
    def subscribe(identifier: SubscriptionId): ZIO[Scope, IOError, EventStreamOutput]

object EventStream:
    lazy val inMemory = HubEventStream.live

trait EventStreamInput:
    def newEvent(event: Event): ZIO[Any, IOError, Unit]
    def eventSink: ZSink[Any, IOError, Event, Nothing, Unit]

case class EnqueueEventStreamInput(enqueue: Enqueue[Event]) extends EventStreamInput:
    override def newEvent(event: Event): ZIO[Any, IOError, Unit] = enqueue.offer(event).unit
    override def eventSink: ZSink[Any, IOError, Event, Nothing, Unit] = ZSink.fromQueue(enqueue)

trait EventStreamOutput:
    def takeEvent: ZIO[Any, IOError, Event]
    def takeEvents(n: Int): ZIO[Any, IOError, List[Event]]
    def streamEvents: ZStream[Any, IOError, Event]

case class DequeueEventStreamOutput(dequeue: Dequeue[Event]) extends EventStreamOutput:
    override def takeEvent: ZIO[Any, IOError, Event] = dequeue.take
    override def takeEvents(n: Int): ZIO[Any, IOError, List[Event]] = dequeue.takeN(n).map(_.toList)
    override def streamEvents: ZStream[Any, IOError, Event] = ZStream.fromQueue(dequeue)

case class HubEventStream(hub: Hub[Event]) extends EventStream:
    override def input: ZIO[Any, IOError, EventStreamInput] = ZIO.succeed(EnqueueEventStreamInput(hub))
    override def subscribe(identifier: SubscriptionId): ZIO[Scope, IOError, EventStreamOutput] = 
        hub.subscribe.map(DequeueEventStreamOutput.apply)

object HubEventStream:
    lazy val live = ZLayer.fromZIO(Hub.unbounded[Event].map(HubEventStream.apply))

