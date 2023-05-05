package chat.service

import chat.model.*
import DomainError.*
import scala.collection.immutable.ListSet

import zio.*
import zio.direct.*

import chat.model.DomainTypes.TimeStamp
import chat.model.UserTypes.UserHandle
import MessageTypes.*
import UserTypes.*

trait MessageService:
    def submitMessage(message: Message): ZIO[Any, IOError, TimeStamp]
    protected def getMessagesImpl(
        limit: Int,
        offset: Int,
        fromTime: Option[TimeStamp],
        channel: Option[Channel],
        sender: Option[UserHandle],
    ): ZIO[Any, IOError, List[SubmittedMessage]]
    def userChannels(user: UserHandle): ZIO[Any, IOError, List[Channel]]

    private def getMessageRequestErrors(
        limit: Option[Int],
        offset: Option[Int],
        fromTime: Option[TimeStamp],
        channel: Option[Channel],
        sender: Option[UserHandle],
    ): List[DataError] =
        val intReason = "value less than zero"
        val limitErrors = if (limit.exists(_ < 0)) List(DataError("limit", intReason)) else Nil
        val offsetErrors = if (offset.exists(_ < 0)) List(DataError("offset", intReason)) else Nil
        val senderErrors = if (sender.exists(snd => channel.exists(!_.contains(snd)))) List(DataError("sender", "sender is not a member of channel")) else Nil
        limitErrors ++ offsetErrors ++ senderErrors

    final def getMessages(
        limit: Option[Int],
        offset: Option[Int],
        fromTime: Option[TimeStamp],
        channel: Option[Channel],
        sender: Option[UserHandle],
    ): ZIO[Any, IOError | InvalidRequest, List[SubmittedMessage]] =
        getMessageRequestErrors(limit, offset, fromTime, channel, sender) match
            case Nil => getMessagesImpl(
                limit = limit.getOrElse(20),
                offset = offset.getOrElse(0),
                fromTime = fromTime,
                channel = channel,
                sender = sender,
            )
            case nonEmptyErrors => ZIO.fail(InvalidRequest(nonEmptyErrors))

object MessageService:
    lazy val inMemory = InMemoryMessageService.live

final case class InMemoryMessageService(
    dataRef: Ref[InMemoryMessageService.Data],
) extends MessageService:
    override def submitMessage(message: Message): ZIO[Any, IOError, TimeStamp] = defer {
        val submittedMessage = message.submittedNow.run
        val id = submittedMessage.id
        val ts = submittedMessage.timestamp
        dataRef.update(data => {
            val newIndex = data.index.updatedWith(None) {
                case None =>
                    Some(Map(None -> List(id -> ts), Some(submittedMessage.sender) -> List(id -> ts)))
                case Some(senderIndex) => Some(senderIndex.updatedWith(None) {
                    case None => Some(List(id -> ts))
                    case Some(list) => Some((id -> ts) :: list)
                }.updatedWith(Some(submittedMessage.sender)) {
                    case None => Some(List(id -> ts))
                    case Some(list) => Some((id -> ts) :: list)
                })
            }.updatedWith(Some(submittedMessage.channel)) {
                case None =>
                    Some(Map(None -> List(id -> ts), Some(submittedMessage.sender) -> List(id -> ts)))
                case Some(senderIndex) => Some(senderIndex.updatedWith(None) {
                    case None => Some(List(id -> ts))
                    case Some(list) => Some((id -> ts) :: list)
                }.updatedWith(Some(submittedMessage.sender)) {
                    case None => Some(List(id -> ts))
                    case Some(list) => Some((id -> ts) :: list)
                })
            }
            val newRepo = data.repository.updated(id, submittedMessage)
            val newChannels = message.channel.foldLeft(data.userChannels) { (currentUserChannels, nextUser) =>
                currentUserChannels.updatedWith(nextUser) {
                    case Some(userChannelSet) => Some(userChannelSet + message.channel)
                    case None => Some(ListSet(message.channel))
                }
            }
            data.copy(newIndex, newRepo, newChannels)
        }).run
        ts
    }

    override protected def getMessagesImpl(
        limit: Int,
        offset: Int,
        fromTime: Option[TimeStamp],
        channel: Option[Channel],
        sender: Option[UserHandle]
    ): ZIO[Any, IOError, List[SubmittedMessage]] = defer {
        import math.Ordered.orderingToOrdered
        val data = dataRef.get.run
        data.index
            .get(channel)
            .flatMap(senderIndex => senderIndex.get(sender))
            .toList
            .flatten
            .dropWhile {
                case (_, ts) => fromTime match
                    case None => false
                    case Some(fromTs) => ts > fromTs
            }
            .drop(offset)
            .take(limit)
            .flatMap {
                case (id, _) => data.repository.get(id).toList
            }
    }

    override def userChannels(user: UserTypes.UserHandle): ZIO[Any, IOError, List[Channel]] = defer {
        val data = dataRef.get.run
        data.userChannels.get(user).toList.flatMap(_.toList)
    }

object InMemoryMessageService:
    final case class Data(
        index: Map[Option[Channel], Map[Option[UserHandle], List[(MessageId, TimeStamp)]]],
        repository: Map[MessageId, SubmittedMessage],
        userChannels: Map[UserHandle, ListSet[Channel]],
    )

    object Data:
        val empty = Data(Map.empty, Map.empty, Map.empty)

    lazy val live = ZLayer.fromZIO(Ref.make(Data.empty).map(InMemoryMessageService.apply))