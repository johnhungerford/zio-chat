package chat.service

object InMemoryMessageServiceSpec
    extends MessageServiceSpec("InMemoryMessageService", InMemoryMessageService.live)
