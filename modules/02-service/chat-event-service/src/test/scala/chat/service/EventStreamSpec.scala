package chat.service

import chat.model.*
import UserTypes.*
import DomainGenerators.*

import zio.* 
import zio.stream.*
import zio.test.* 
import zio.direct.*

abstract class EventStreamSpec[E](impl: String, layer: ZLayer[Any, E, EventStream]) extends ZIOSpecDefault:
    override def spec: Spec[TestEnvironment & Scope, Any] = suite(s"EventStreamSpec: $impl")(
        test("an event published to an event stream should be taken by a subscriber")(
            check(eventGen)(event => defer {
                val stream = ZIO.service[EventStream].provide(layer).run
                val input = stream.input.run
                val output = stream.subscribe(SubscriptionId("test-sub-id")).run
                val evtOutFiber = output.takeEvent.fork.run
                input.newEvent(event).run
                val evtOut = evtOutFiber.join.run
                assertTrue(evtOut == event)
            })
        ),
        test("a stream published to an event stream via eventSink should be streamed by a subscriber via streamEvents")(
            check(Gen.chunkOfBounded(10, 90)(eventGen))(eventChunk => defer {
                val stream = ZIO.service[EventStream].provide(layer).run
                val input = stream.input.run
                val output = stream.subscribe(SubscriptionId("test-sub-id")).run
                val evtStream = ZStream.fromChunk(eventChunk)
                val evtOutFiber = output.streamEvents.take(eventChunk.length).runCollect.fork.run
                evtStream.run(input.eventSink).run
                val evtOut: Chunk[Event] = evtOutFiber.join.run
                assertTrue(evtOut == eventChunk)
            })
        ) @@ TestAspect.samples(5)
    )
