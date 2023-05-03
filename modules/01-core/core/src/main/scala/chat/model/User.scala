package chat.model

import java.time.LocalDateTime
import java.time.LocalDate
import zio.prelude.Subtype
import zio.prelude.Assertion
import zio.json.*
import java.util.UUID

object UserTypes:
    import DomainAssertions.* 
    import DomainTypes.*

    type UserHandle = UserHandle.Type
    object UserHandle extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            Assertion.matches("""[a-zA-Z0-9-.]+""")
                && Assertion.hasLength(Assertion.lessThanOrEqualTo(32))

    type BirthDate = BirthDate.Type
    object BirthDate extends Subtype[LocalDate] with SubtypeCodec[LocalDate]:
        inline def minDate: LocalDate = LocalDate.of(1900, 1, 1)
        val minVal: BirthDate = BirthDate.wrap(minDate)
        override inline def assertion: Assertion[LocalDate] =
            Assertion.greaterThanOrEqualTo(minDate)

    type FirstName = FirstName.Type
    object FirstName extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(50))

    type LastName = LastName.Type
    object LastName extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(100))

    type AboutUser = AboutUser.Type
    object AboutUser extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(500))

import UserTypes.*
import DomainTypes.*

case class User(
    handle: UserHandle,
    location: Location,
    birthDate: BirthDate,
    firstName: Option[FirstName],
    lastName: Option[LastName],
    about: Option[AboutUser],
    creationTimestamp: TimeStamp,
)

object User:
    given JsonCodec[User] = DeriveJsonCodec.gen

object UserTst:
    val somat: String = ""

    import zio.json.*

    def main(args: Array[String]): Unit =
        val ts = TimeStamp.make(LocalDateTime.now()).getOrElse(throw new Exception("sdf"))
        println(ts.toJson)
        val badTs = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
        println(badTs.toJson.fromJson[TimeStamp])
