package chat.service

import chat.model.*
import UserTypes.*
import DomainGenerators.*

import zio.* 
import zio.stream.*
import zio.test.* 
import zio.direct.*

// object UserSubscriptionSpec extends ZIOSpecDefault:
//     override def spec: Spec[TestEnvironment & Scope, Any] = suite("UserSubscriptionSpec")(
//         test("should only expose messages revelant to the subscriber") {
//             check(Gen.chunkOfBounded(10, 90)(eventGen))(eventChunk => defer {
//                 val stream = ZIO.service[EventStream].provide(layer).run
//                 val input = stream.input.run
//                 val output = stream.subscribe(SubscriptionId("test-sub-id")).run
//                 val evtStream = ZStream.fromChunk(eventChunk)
//                 val evtOutFiber = output.streamEvents.take(eventChunk.length).runCollect.fork.run
//                 evtStream.run(input.eventSink).run
//                 val evtOut: Chunk[Event] = evtOutFiber.join.run
//                 assertTrue(evtOut == eventChunk)
//             })
//         } @@ TestAspect.samples(5)
//     )
