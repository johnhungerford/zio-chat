package chat.service

object InMemoryUserServiceSpec extends UserServiceSpec[Nothing]("InMemoryUserService", InMemoryUserService.live)
