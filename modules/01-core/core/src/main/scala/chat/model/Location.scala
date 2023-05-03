package chat.model

import zio.prelude.{Assertion, Subtype}
import zio.json.*

object LocationTypes:
    import DomainAssertions.* 
    import DomainTypes.* 

    type Country = Country.Type
    object Country extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(66))

    type StateProvince = StateProvince.Type
    object StateProvince extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(96))

    type City = City.Type
    object City extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(96))

    type StreetName = StreetName.Type
    object StreetName extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(57))

    type StreetNumber = StreetNumber.Type
    object StreetNumber extends Subtype[Int] with SubtypeCodec[Int]:
        override inline def assertion: Assertion[Int] =
            Assertion.greaterThanOrEqualTo(0)
                && Assertion.lessThanOrEqualTo(10000000)

    type PostalCode = PostalCode.Type
    object PostalCode extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(15))

    type Apartment = Apartment.Type
    object Apartment extends Subtype[String] with SubtypeCodec[String]:
        override inline def assertion: Assertion[String] =
            notEmptyAndTrimmed
                && Assertion.hasLength(Assertion.lessThan(15))

import LocationTypes.*

final case class Location(
    country: Country,
    stateOrProvince: Option[StateProvince],
    city: Option[City],
    address: Option[Address],
)

object Location:
    given JsonCodec[Location] = DeriveJsonCodec.gen


final case class Address(
    street: StreetName,
    streetNumber: StreetNumber,
    apartment: Apartment,
    postalCode: PostalCode,
)

object Address:
    given JsonCodec[Address] = DeriveJsonCodec.gen
