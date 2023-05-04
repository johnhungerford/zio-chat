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

    type Channel = Channel.Type
    object Channel
        extends Subtype[NonEmptySet[UserHandle]]
        with SubtypeCodec[NonEmptySet[UserHandle]](using DomainTypes.nonEmptySetCodec[UserHandle]):
            def zio(value: NonEmptySet[UserHandle]): ZIO[Any, List[String], Channel] =
                ZIO.fromEither(make(value).toEither.left.map(_.toList))

import MessageTypes.*
import DomainTypes.*

final case class Message(
    body: MessageBody,
    sender: UserHandle,
    recipients: NonEmptySet[UserHandle],
):
    lazy val channel: Channel = Channel.wrap(recipients + sender)

    lazy val channelParticipants: NonEmptySet[UserHandle] = channel

    def submitted(timestamp: TimeStamp): SubmittedMessage =
        SubmittedMessage(body, sender, recipients, timestamp)

    def submittedNow: ZIO[Any, Nothing, SubmittedMessage] =
        TimeStamp.now.map(ts => submitted(ts))


object Message:
    import DomainTypes.nonEmptySetCodec
    given JsonCodec[Message] = DeriveJsonCodec.gen

final case class SubmittedMessage(
    body: MessageBody,
    sender: UserHandle,
    recipients: NonEmptySet[UserHandle],
    timestamp: TimeStamp,
):
    lazy val id: MessageId = (sender, channel, timestamp)

    lazy val channel: Channel =
        message.channel

    lazy val channelParticipants: NonEmptySet[UserHandle] = channel

    lazy val message: Message = Message(body, sender, recipients)

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
