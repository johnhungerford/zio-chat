package chat.model

import chat.model.UserTypes.*
import chat.model.MessageTypes.*

import zio.json.*
import zio.prelude.* 

enum Event:
    case MessageSubmission(submittedMessage: SubmittedMessage)
    case MessageReceived(messageId: MessageId, userHandle: UserHandle, channel: Channel)
    case MessageRead(messageId: MessageId, userHandle: UserHandle, channel: Channel)
    case UserAdded(user: User)
    case UserUpdated(updates: User)
    case UserRemoved(userHandle: UserHandle)
    case UserConnected(userHandle: UserHandle)
    case UserDisconnected(userHandle: UserHandle)
    case UserInactive(userHandle: UserHandle)

object Event:
    import MessageId.given
    given JsonCodec[Event] = DeriveJsonCodec.gen

type SubscriptionId = SubscriptionId.Type
object SubscriptionId extends Subtype[String] with SubtypeCodec[String]:
    override inline def assertion: Assertion[String] =
            Assertion.matches("""[a-z0-9-]+""")
                && Assertion.hasLength(Assertion.lessThanOrEqualTo(30))
