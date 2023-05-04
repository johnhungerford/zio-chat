package chat.model

import java.time.LocalDateTime
import zio.*
import zio.prelude.Subtype
import zio.prelude.Assertion
import zio.json.JsonEncoder
import zio.json.JsonDecoder
import zio.prelude.Validation
import java.security.Timestamp
import zio.json.*
import zio.prelude.NonEmptySet

trait SubtypeCodec[A](using JsonCodec[A]):
    self: Subtype[A] =>
        import DomainTypes.* 

        given JsonCodec[Type] = JsonCodec[A]
            .transformOrFail(
                value => make(value).toJsonResult,
                identity
            )


object DomainAssertions:
    inline def notEmptyOrOnlyWhitespace: Assertion[String] =
        (!Assertion.isEmptyString)
            && Assertion.matches(""".*[\S].*""")

    inline def trimmed: Assertion[String] =
        Assertion.matches("""[\S].*[\S]""")

    inline def notEmptyAndTrimmed = (!Assertion.isEmptyString) && trimmed


object DomainTypes:
    extension[A](validationResult: Validation[String, A])
        def toJsonResult: Either[String, A] =
            validationResult.toEither.left.map(_.mkString("; "))

    type TimeStamp = TimeStamp.Type
    object TimeStamp extends Subtype[LocalDateTime] with SubtypeCodec[LocalDateTime]:
        inline def minTime: LocalDateTime = LocalDateTime.of(2023, 5, 2, 0, 0, 0)
        val minVal: TimeStamp = TimeStamp.wrap(minTime)
        val now: ZIO[Any, Nothing, TimeStamp] = Clock.localDateTime.map(TimeStamp.wrap)
        override inline def assertion: Assertion[LocalDateTime] =
            Assertion.greaterThanOrEqualTo(minTime)

    given nonEmptySetCodec[A: Ordering](using codec: JsonCodec[List[A]]): JsonCodec[NonEmptySet[A]] =
        codec.transformOrFail(
                list => list match {
                    case Nil => Left("")
                    case head :: tail => Right(NonEmptySet(head, tail: _*))
                },
                nonEmptySet => nonEmptySet.toList.sorted,
            )

