package chat.model

import zio.prelude.{Assertion, Subtype}
import zio.json.*
import java.util.UUID
import chat.model.UserTypes.UserHandle

import zio.*
import zio.prelude.NonEmptySet
import zio.prelude.Newtype

object MessageTypes:
    import DomainAssertions.* 
    import DomainTypes.*

    type MessageId = (UserHandle, NonEmptySet[UserHandle], TimeStamp)
    object MessageId:
        import DomainTypes.given
        given JsonCodec[MessageId] = DeriveJsonCodec.gen

    type MessageTag = MessageTag.Type
    object MessageTag extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            Assertion.matches("""[a-zA-Z0-9-_]+""")
                && Assertion.hasLength(Assertion.lessThanOrEqualTo(30))
            
    type TextSpan = TextSpan.Type
    object TextSpan extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            Assertion.hasLength(Assertion.greaterThan(0))

import MessageTypes.*
import DomainTypes.*

final case class Message(
    body: MessageBody,
    sender: UserHandle,
    recipients: NonEmptySet[UserHandle],
):
    lazy val threadParticipants: Set[UserHandle] =
        recipients + sender

    def submitted(timestamp: TimeStamp): SubmittedMessage =
        SubmittedMessage(body, sender, recipients, timestamp)

    def submittedNow: ZIO[Any, List[String], SubmittedMessage] =
        Clock.currentDateTime
            .map(_.toLocalDateTime())
            .flatMap(ldt => ZIO.fromEither(TimeStamp.make(ldt).toEither.left.map(_.toList)))
            .map(ts => submitted(ts))

object Message:
    import DomainTypes.nonEmptySetCodec
    given JsonCodec[Message] = DeriveJsonCodec.gen

final case class SubmittedMessage(
    body: MessageBody,
    sender: UserHandle,
    recipients: NonEmptySet[UserHandle],
    timestamp: TimeStamp,
):
    lazy val id: MessageId = (sender, recipients, timestamp)

    lazy val message: Message = Message(body, sender, recipients)

    lazy val threadParticipants: Set[UserHandle] =
        message.threadParticipants

object SubmittedMessage:
    import DomainTypes.nonEmptySetCodec
    given JsonCodec[SubmittedMessage] = DeriveJsonCodec.gen

final case class MessageBody(
    body: List[MessageComponent],
):
    lazy val tags: Set[MessageTag] =
        (body collect {
            case MessageComponent.Tag(tag) => tag
        }).toSet

    lazy val length: Int =
        body.foldLeft(0)((currentLength, nextComponent) => nextComponent match
            case MessageComponent.Tag(tag) => currentLength + tag.length
            case MessageComponent.Text(span) => currentLength + span.length
        )

object MessageBody:
    given JsonCodec[MessageBody] = JsonCodec[List[MessageComponent]]
        .transform(MessageBody.apply, _.body)

enum MessageComponent:
    case Tag(tag: MessageTag)
    case Text(span: TextSpan)

object MessageComponent:
    given JsonCodec[MessageComponent] = DeriveJsonCodec.gen
