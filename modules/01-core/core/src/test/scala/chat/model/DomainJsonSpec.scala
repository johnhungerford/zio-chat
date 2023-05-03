package chat.model

import zio.test.*
import zio.*
import zio.json.*
import chat.model.DomainGenerators.*

object DomainJsonSpec extends ZIOSpecDefault:
    def jsonPropTest[A](tpe: String, gen: Gen[Any, A])(using JsonCodec[A]) =
        test(s"$tpe should serialize and deserialize to same value") {
            check(gen) { (value) =>
                assertTrue(value.toJson.fromJson[A].contains(value))
            }
        }

    override def spec: Spec[TestEnvironment & Scope, Any] = suite("DomainJsonSpec")(
        jsonPropTest("UserHandle", userHandleGen),
        jsonPropTest("BirthDate", birthDateGen),
        jsonPropTest("FirstName", firstNameGen),
        jsonPropTest("LastName", lastNameGen),
        jsonPropTest("AboutUser", aboutUserGen),
        jsonPropTest("Country", countryGen),
        jsonPropTest("StateProvince", stateProvinceGen),
        jsonPropTest("City", cityGen),
        jsonPropTest("StreetName", streetNameGen),
        jsonPropTest("StreetNumber", streetNumberGen),
        jsonPropTest("PostalCode", postalCodeGen),
        jsonPropTest("Apartment", apartmentGen),
        jsonPropTest("Address", addressGen),
        jsonPropTest("Location", locationGen),
        jsonPropTest("User", userGen),
        jsonPropTest("MessageTag", messageTagGen),
        jsonPropTest("MessageComponent", messageComponentGen),
        jsonPropTest("TextSpan", textSpanGen),
        jsonPropTest("MessageBody", messageBodyGen),
        jsonPropTest("Message", messageGen),
        jsonPropTest("SubmittedMessage", submittedMessageGen),
        jsonPropTest("TimeStamp", timeStampGen),
        jsonPropTest("Event", eventGen),
    )
