package chat.service

object HubEventStreamSpec extends EventStreamSpec[Nothing]("HubEventStreamSpec", HubEventStream.live)
