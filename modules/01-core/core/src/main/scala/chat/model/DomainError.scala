package chat.model

final case class DataError(parameter: Option[String], reason: String)

object DataError:
    def apply(param: String, reason: String): DataError = DataError(Some(param), reason)
    def apply(reason: String): DataError = DataError(None, reason)

enum DomainError:
    case InvalidData(domainType: String, dataErrors: List[DataError])
    case InvalidRequest(requestErrors: List[DataError])
    case IOError(message: String, cause: Option[Throwable])
