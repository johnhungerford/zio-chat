package chat.model

import chat.model.UserTypes.*
import chat.model.MessageTypes.*
import zio.json.*

enum Event:
    case MessageSubmission(submittedMessage: SubmittedMessage)
    case MessageReceived(messageId: MessageId, userHandle: UserHandle)
    case MessageRead(messageId: MessageId, userHandle: UserHandle)
    case UserAdded(user: User)
    case UserUpdated(updates: User)
    case UserRemoved(userHandle: UserHandle)
    case UserConnected(userHandle: UserHandle)
    case UserDisconnected(userHandle: UserHandle)
    case UserInactive(userHandle: UserHandle)

object Event:
    import MessageId.given
    given JsonCodec[Event] = DeriveJsonCodec.gen
