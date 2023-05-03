package chat.model

final case class DataError(parameter: String, reason: String)

enum DomainError:
    case InvalidData(domainType: String, dataErrors: List[DataError])
    case IOError(message: String, cause: Option[Throwable])
