package chat.model

import zio.* 
import zio.test.* 
import chat.model.DomainTypes.TimeStamp
import zio.prelude.Validation
import chat.model.UserTypes.*
import chat.model.LocationTypes.*
import chat.model.MessageTypes.*
import java.time.LocalDateTime
import zio.prelude.newtypes.Last
import zio.prelude.NonEmptySet

object DomainGenerators:
    private def newSubtypeGen[R, A, B <: A](gen: Gen[R, A], fn: A => Validation[String, B]): Gen[R, B] =
        gen.map(a => fn(a).toOption).collect { case Some(v) => v }

    private val maxTime: LocalDateTime = LocalDateTime.of(2050, 1, 1, 0, 0, 0)

    val timeStampGen: Gen[Any, TimeStamp] =
        newSubtypeGen(Gen.localDateTime(TimeStamp.minTime, maxTime), TimeStamp.make)

    val userHandleGen: Gen[Any, UserHandle] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 32), UserHandle.make)

    val birthDateGen: Gen[Any, BirthDate] =
        newSubtypeGen(Gen.localDate(BirthDate.minDate, maxTime.toLocalDate), BirthDate.make)
    
    val firstNameGen: Gen[Any, FirstName] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 50), FirstName.make)

    val lastNameGen: Gen[Any, LastName] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 100), LastName.make)

    val aboutUserGen: Gen[Any, AboutUser] =
        newSubtypeGen(Gen.stringBounded(1, 500)(Gen.char), AboutUser.make)

    val countryGen: Gen[Any, Country] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 66), Country.make)

    val stateProvinceGen: Gen[Any, StateProvince] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 96), StateProvince.make)

    val cityGen: Gen[Any, City] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 96), City.make)

    val streetNameGen: Gen[Any, StreetName] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 57), StreetName.make)

    val streetNumberGen: Gen[Any, StreetNumber] =
        newSubtypeGen(Gen.int(0, 10000000), StreetNumber.make)

    val postalCodeGen: Gen[Any, PostalCode] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 15), PostalCode.make)

    val apartmentGen: Gen[Any, Apartment] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 15), Apartment.make)

    val addressGen: Gen[Any, Address] =
        (streetNameGen <*> streetNumberGen <*> apartmentGen <*> postalCodeGen)
            .map((street, number, apartment, postalCode) => Address(street, number, apartment, postalCode))

    val locationGen: Gen[Any, Location] =
        (countryGen <*> Gen.option(stateProvinceGen) <*> Gen.option(cityGen) <*> Gen.option(addressGen))
            .map((country, stateOrProvince, city, address) => Location(country, stateOrProvince, city, address))
    
    val userGen: Gen[Any, User] =
        (userHandleGen <*> locationGen <*> birthDateGen <*> Gen.option(firstNameGen) <*> Gen.option(lastNameGen) <*> Gen.option(aboutUserGen) <*> timeStampGen) 
            .map((handle, location, birthday, firstName, lastName, about, creationTs) => User(handle, location, birthday, firstName, lastName, about, creationTs))

    val messageTagGen: Gen[Any, MessageTag] =
        newSubtypeGen(Gen.alphaNumericStringBounded(1, 30), MessageTag.make)

    val textSpanGen: Gen[Any, TextSpan] =
        newSubtypeGen(Gen.stringBounded(1, 200)(Gen.char), TextSpan.make)

    val tagGen: Gen[Any, MessageComponent.Tag] =
        messageTagGen.map(MessageComponent.Tag.apply)

    val textGen: Gen[Any, MessageComponent.Text] =
        textSpanGen.map(MessageComponent.Text.apply)

    val messageComponentGen: Gen[Any, MessageComponent] =
        Gen.oneOf(textGen, tagGen)

    val messageBodyGen: Gen[Any, MessageBody] =
        Gen.listOfBounded(1, 15)(messageComponentGen).map(MessageBody.apply)

    def nonEmptySetGen[R, A](min: Int, max: Int)(gen: Gen[R, A]): Gen[R, NonEmptySet[A]] =
        Gen.setOf(gen).map(set => set.headOption match
            case None => None
            case Some(value) => Some(NonEmptySet.fromSet(value, set))
        ).collect { case Some(value) => value }

    val messageGen: Gen[Any, Message] =
        (messageBodyGen <*> userHandleGen <*> nonEmptySetGen(1, 50)(userHandleGen))
            .map((body, sender, recipients) => Message(body, sender, recipients))

    val submittedMessageGen: Gen[Any, SubmittedMessage] =
        (messageBodyGen <*> userHandleGen <*> nonEmptySetGen(1, 50)(userHandleGen) <*> timeStampGen)
            .map((body, sender, recipients, timestamp) => SubmittedMessage(body, sender, recipients, timestamp))

    val messageSubmissionGen: Gen[Any, Event.MessageSubmission] =
        submittedMessageGen.map(Event.MessageSubmission.apply)

    val messageIdGen: Gen[Any, MessageId] =
        (userHandleGen <*> nonEmptySetGen(1, 20)(userHandleGen) <*> timeStampGen)

    val messageReceivedGen: Gen[Any, Event.MessageReceived] =
        (messageIdGen <*> userHandleGen)
            .map((a, b, c, userHandle) => Event.MessageReceived((a, b, c), userHandle))

    val messageReadGen: Gen[Any, Event.MessageRead] =
        (messageIdGen <*> userHandleGen)
            .map((a, b, c, userHandle) => Event.MessageRead((a, b, c), userHandle))

    val userConnectedGen: Gen[Any, Event.UserConnected] =
        userHandleGen.map(Event.UserConnected.apply)

    val userDisconnectedGen: Gen[Any, Event.UserDisconnected] =
        userHandleGen.map(Event.UserDisconnected.apply)

    val userInactiveGen: Gen[Any, Event.UserInactive] =
        userHandleGen.map(Event.UserInactive.apply)

    val userAddedGen: Gen[Any, Event.UserAdded] =
        userGen.map(Event.UserAdded.apply)

    val userUpdatedGen: Gen[Any, Event.UserUpdated] =
        userGen.map(Event.UserUpdated.apply)

    val userRemovedGen: Gen[Any, Event.UserRemoved] =
        userHandleGen.map(Event.UserRemoved.apply)

    val eventGen: Gen[Any, Event] =
        Gen.oneOf(
            messageSubmissionGen,
            messageReceivedGen,
            messageReadGen,
            userAddedGen,
            userUpdatedGen,
            userRemovedGen,
            userConnectedGen,
            userDisconnectedGen,
            userInactiveGen
        )
