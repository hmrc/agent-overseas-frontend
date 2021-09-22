/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentoverseasfrontend.validators

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, EitherValues, OptionValues}
import play.api.data.{FormError, Mapping}
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators._

import scala.collection.immutable.Stream
import scala.util.Random

class CommonValidatorsSpec extends AnyWordSpecLike with Matchers with OptionValues with EitherValues {

  "saUtr bind" should {
    val utrMapping = saUtr.withPrefix("testKey")

    def bind(fieldValue: String) = utrMapping.bind(Map("testKey" -> fieldValue))

    "accept valid UTRs" in {
      bind("20000  00000") shouldBe Right("20000  00000")
    }

    "give \"error.sautr.blank\" error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", "error.sautr.blank")
    }

    "give \"error.sautr.blank\" error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", "error.sautr.blank")
    }

    "give \"error.sautr.invalid\" error" when {
      "it has more than 10 digits" in {
        bind("20000000000") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }

      "it has fewer than 10 digits" in {
        bind("200000") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }

        bind("20000000 0") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }

      "it has non-digit characters" in {
        bind("200000000B") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }

      "it has non-alphanumeric characters" in {
        bind("200000000!") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }
    }
  }

  "emailAddress bind" should {
    val emailAddress = CommonValidators.businessEmail.withPrefix("testKey")

    def bind(fieldValue: String) = emailAddress.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern { case Left(List(FormError("testKey", List("error.email.invalid"), _))) => }

    def shouldAcceptFieldValue(fieldValue: String): Assertion =
      bind(fieldValue) shouldBe Right(fieldValue)

    "reject email address" when {
      "field is not present" in {
        emailAddress.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }

      "input is empty" in {
        bind("").left.value should contain only FormError("testKey", "error.email.blank")
      }

      "input has length more than 132 characters" in {
        bind(s"${Random.alphanumeric.take(132).mkString}@example.com").left.value should contain only FormError(
          "testKey",
          "error.email.maxlength")
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", "error.email.blank")
      }

      "not a valid email" in {
        shouldRejectFieldValueAsInvalid("bademail")
        shouldRejectFieldValueAsInvalid("a.@test.com")
      }

      "has spaces" in {
        shouldRejectFieldValueAsInvalid("bad email@example.com")
      }
    }

    "accept a valid email address" in {
      shouldAcceptFieldValue("valid+email@example.com")
      shouldAcceptFieldValue("valid@test.com")
      shouldAcceptFieldValue("valid.email@test.com")
      shouldAcceptFieldValue("valid_email@test.com")
      shouldAcceptFieldValue("valid-email@test.com")
      shouldAcceptFieldValue("valid-email.address@test.com")
      shouldAcceptFieldValue("valid-email._address@test.com")
      shouldAcceptFieldValue("a1@test.com")
    }
  }

  " addressLine1, 2 bind" should {
    val unprefixedAddressLine1Mapping = addressLine12(lineNumber = 1)
    val unprefixedAddressLine2Mapping = addressLine12(lineNumber = 2)

    behave like anAddressLineValidatingMapping(unprefixedAddressLine1Mapping, 1)
    behave like anAddressLineValidatingMapping(unprefixedAddressLine2Mapping, 2)

    val addressLine1Mapping = unprefixedAddressLine1Mapping.withPrefix("testKey")

    def bind(fieldValue: String) = addressLine1Mapping.bind(Map("testKey" -> fieldValue))

    "reject the line" when {
      "field is not present" in {
        addressLine1Mapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", "error.addressline.1.empty"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain(FormError("testKey", "error.addressline.1.empty"))
      }
    }
  }

  "addressLine 3 and 4 bind" should {
    def nonOptionalAddressLine34Mapping(lineNumber: Int): Mapping[String] =
      addressLine34(lineNumber).transform(_.get, Some.apply)

    behave like anAddressLineValidatingMapping(nonOptionalAddressLine34Mapping(3), 3)
    behave like anAddressLineValidatingMapping(nonOptionalAddressLine34Mapping(4), 4)

    val addressLine23Mapping = addressLine34(3).withPrefix("testKey")

    def bind(fieldValue: String) = addressLine23Mapping.bind(Map("testKey" -> fieldValue))

    def shouldAcceptFieldValue(fieldValue: String): Assertion =
      if (fieldValue.isEmpty) bind(fieldValue) shouldBe Right(None)
      else bind(fieldValue) shouldBe Right(Some(fieldValue))

    "reject the line" when {
      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", "error.addressline.3.empty")
      }
    }

    "accept the line" when {
      "field is empty" in {
        shouldAcceptFieldValue("")
      }
    }
  }

  "first name bind" should {
    testNameBinding("firstName")(firstName.withPrefix("testKey"))
  }

  "last name bind" should {
    testNameBinding("lastName")(lastName.withPrefix("testKey"))
  }

  def testNameBinding(nameType: String)(mapping: => Mapping[String]): Unit = {

    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List(invalidErrorMessage), _))) =>
      }

    def shouldRejectFieldValueAsTooLong(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List(maxLengthErrorMessage), _))) =>
      }

    def shouldAcceptFieldValue(fieldValue: String): Assertion =
      bind(fieldValue) shouldBe Right(fieldValue)

    s"reject $nameType" when {

      "there is an ampersand character" in {
        shouldRejectFieldValueAsInvalid("My Agency & Co")
      }

      "there is a number" in {
        shouldRejectFieldValueAsInvalid("My Agency99")
      }

      "there is an invalid character" in {
        shouldRejectFieldValueAsInvalid("My Agency; His Agency #1")
        shouldRejectFieldValueAsInvalid("My Agency/ His Agency #1")
      }

      "there are more than 35 characters" in {
        shouldRejectFieldValueAsTooLong(Random.alphanumeric.take(35).mkString)
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", s"error.$nameType.blank"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", s"error.$nameType.blank")
      }

      "field is not present" in {
        mapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }
    }

    s"accept $nameType" when {
      "there are valid characters" in {
        shouldAcceptFieldValue("My Agency")
        shouldAcceptFieldValue("My--Agency")
        shouldAcceptFieldValue("My'Agency")
      }
    }
  }

  "jobTitle bind" should {

    val mapping = jobTitle.withPrefix("testKey")

    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.jobTitle.invalid"), _))) =>
      }

    def shouldRejectFieldValueAsIncorrectLength(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.jobTitle.length"), _))) =>
      }

    def shouldAcceptFieldValue(fieldValue: String): Assertion =
      bind(fieldValue) shouldBe Right(fieldValue)

    s"reject job Title" when {

      "there is an ampersand character" in {
        shouldRejectFieldValueAsInvalid("Software & Dev")
      }

      "there is a number" in {
        shouldRejectFieldValueAsInvalid("Software99")
      }

      "there is an invalid character" in {
        shouldRejectFieldValueAsInvalid("Software;  #1")
        shouldRejectFieldValueAsInvalid("Softwarey/  #1")
      }

      "there are more than 50 characters" in {
        shouldRejectFieldValueAsIncorrectLength(randomString(51))
      }

      "there are less than 2 characters" in {
        shouldRejectFieldValueAsIncorrectLength("a")
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", s"error.jobTitle.blank"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", s"error.jobTitle.blank")
      }

      "field is not present" in {
        mapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }
    }

    s"accept jobTitle" when {
      "there are valid characters" in {
        shouldAcceptFieldValue("My Agency")
        shouldAcceptFieldValue("My--Agency")
        shouldAcceptFieldValue("My'Agency")
      }
    }
  }

  "tradingName bind" should {

    val tradingNameMapping = tradingName.withPrefix("testKey")

    def bind(fieldValue: String) = tradingNameMapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.tradingName.invalid"), _))) =>
      }

    def shouldRejectFieldValueAsTooLong(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.tradingName.maxlength"), _))) =>
      }

    def shouldAcceptFieldValue(fieldValue: String): Assertion =
      bind(fieldValue) shouldBe Right(fieldValue)

    "reject trading name" when {

      "there is an ampersand character" in {
        shouldRejectFieldValueAsInvalid("My Agency & Co")
      }

      "there is an apostrophe character" in {
        shouldRejectFieldValueAsInvalid("My Agency's Co")
      }

      "there is a comma character" in {
        shouldRejectFieldValueAsInvalid("My Agency, Co")
      }

      "there is a full stop character" in {
        shouldRejectFieldValueAsInvalid("My Agency Co.")
      }

      "there is an invalid character" in {
        shouldRejectFieldValueAsInvalid("My Agency; His Agency #1")
      }

      "there are more than 40 characters" in {
        shouldRejectFieldValueAsTooLong(randomString(41))
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", "error.tradingName.blank"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", "error.tradingName.blank")
      }

      "field is not present" in {
        tradingNameMapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }
    }

    "accept trading name" when {
      "there are valid characters" in {
        shouldAcceptFieldValue("My Agency")
        shouldAcceptFieldValue("My/Agency")
        shouldAcceptFieldValue("My-Agency")
      }

      "there are numbers and letters" in {
        shouldAcceptFieldValue("The 100 Agency")
      }
    }
  }

  "SA agent code binding" should {
    testAgentCode("saAgentCode")(saAgentCode.withPrefix("testKey"))
  }

  "Corporation tax agent code binding" should {
    testAgentCode("ctAgentCode")(ctAgentCode.withPrefix("testKey"))
  }

  def testAgentCode(codeType: String)(mapping: => Mapping[String]) = {
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid $codeType" in {
      bind("SA1234") shouldBe Right("SA1234")
    }

    s"give error.$codeType.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.$codeType.blank")
    }

    s"give error.$codeType.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.$codeType.blank")
    }

    s"give error.$codeType.maxlength error" when {
      "it has more than 6 characters" in {
        bind("SA20000000000") should matchPattern {
          case Left(List(FormError("testKey", List(maxLengthMessage), _))) =>
        }
      }

      "it has fewer than 6 characters" in {
        bind("SA200") should matchPattern {
          case Left(List(FormError("testKey", List(maxLengthMessage), _))) =>
        }
      }

      "it has no-alphanumeric characters" in {
        bind("SA**12222") should matchPattern {
          case Left(List(FormError("testKey", List(invalidMessage), _))) =>
        }
      }
    }
  }

  "Nino validation" should {
    val ninoMapping = nino.withPrefix("testKey")

    def bind(fieldValue: String) = ninoMapping.bind(Map("testKey" -> fieldValue))

    "accept valid Nino" in {
      bind("AA980984B") shouldBe Right("AA980984B")
    }

    "accept valid Nino with random Spaces" in {
      bind("AA   9 8 0 98 4     B      ") shouldBe Right("AA   9 8 0 98 4     B      ")
    }

    "reject with error when invalid Nino" in {
      bind("AAAAAAAA0").left.value should contain only FormError("testKey", "error.nino.invalid")
    }

    "reject with error when nino field is empty" in {
      bind("").left.value should contain only FormError("testKey", "error.nino.blank")
    }

    "reject with error when nino field contain spaces only" in {
      bind("    ").left.value should contain only FormError("testKey", "error.nino.blank")
    }
  }

  "amlsBody bind" should {
    val amlsBodyMapping = amlsBody.withPrefix("testKey")
    def bind(fieldValue: String) = amlsBodyMapping.bind(Map("testKey" -> fieldValue))

    def shouldAcceptFieldValue(fieldValue: String): Assertion =
      bind(fieldValue) shouldBe Right(fieldValue)

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Assertion =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.moneyLaunderingCompliance.amlsbody.invalid"), _))) =>
      }

    "accept valid AMLS body" in {
      shouldAcceptFieldValue("Association of Accounting Technicians (AAT)")
      shouldAcceptFieldValue("Association of Accounting Technicians 12345")
      shouldAcceptFieldValue("Association of Accounting  & Technicians")
      shouldAcceptFieldValue("Association, Accounting, Technicians")
      shouldAcceptFieldValue("Association' Accounting'Technicians")
      shouldAcceptFieldValue("Association Accounting / Technicians")
      shouldAcceptFieldValue("Association Accounting Technicians.")
      shouldAcceptFieldValue("Association-Accounting-Technicians")
    }

    "return validation error if the field is blank" in {
      bind("").left.value should contain only FormError("testKey", "error.moneyLaunderingCompliance.amlsbody.blank")
    }

    "return validation error if the field length is more than 100 chars" in {
      bind(randomString(101)).left.value should contain only FormError(
        "testKey",
        "error.moneyLaunderingCompliance.amlsbody.maxlength")
    }

    "return validation error if the field is invalid " in {
      shouldRejectFieldValueAsInvalid("CC*")
      shouldRejectFieldValueAsInvalid("CC ££")
      shouldRejectFieldValueAsInvalid("CC $$")
      shouldRejectFieldValueAsInvalid("C++")
      shouldRejectFieldValueAsInvalid("C+===")
      shouldRejectFieldValueAsInvalid("Universitätsstadt im Süden von Deutschland")
    }
  }

  "membershipNumber bind" should {
    val membershipNumberMapping = membershipNumber.withPrefix("testKey")
    def bind(fieldValue: String) = membershipNumberMapping.bind(Map("testKey" -> fieldValue))

    "accept valid membership number" in {
      bind("123456") shouldBe Right(Some("123456"))
    }

    "return validation error if the field is invalid" in {
      bind("**").left.value should contain only FormError("testKey", "error.membershipNumber.invalid")
    }
  }

  "Company registration number binding" should {
    val mapping = companyRegistrationNumber.withPrefix("testKey")
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid CRN" in {
      bind("12345678") shouldBe Right("12345678")
      bind("CN345678") shouldBe Right("CN345678")
      bind("cn345678") shouldBe Right("cn345678")
      bind("onlyalpha") shouldBe Right("onlyalpha")
      bind("A") shouldBe Right("A")
      bind("1") shouldBe Right("1")
      bind("A 1") shouldBe Right("A 1")
      bind("A-1") shouldBe Right("A-1")
      bind("A/1") shouldBe Right("A/1")
      bind("A.1") shouldBe Right("A.1")
    }

    "it is less than or equal to 40 characters" in {
      val crn = randomString(40)
      bind(crn) shouldBe Right(crn)
    }

    s"give error.crn.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.crn.blank")
    }

    s"give error.crn.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.crn.blank")
    }

    s"give error.crn.invalid error" when {
      "it has invalid characters" in {
        bind("BAD*CRN") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }
        bind("BAD:CRN") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }

        bind("BAD#CRN") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }
      }

      "it has more than 40 characters" in {
        bind(randomString(41)) should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.maxlength"), _))) =>
        }
      }
    }
  }

  "Tax registration number binding" should {
    val mapping = taxRegistrationNumber.withPrefix("testKey")
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid TRN" in {
      bind("12345678") shouldBe Right("12345678")
      bind("TN 345678") shouldBe Right("TN 345678")
    }

    s"give error.trn.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.trn.blank")
    }

    s"give error.trn.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.trn.blank")
    }

    s"give error.trn.maxlength error when the size is more than 24 chars" in {
      bind(randomString(25)).left.value should contain only FormError("testKey", s"error.trn.maxlength")
    }

    s"give error.trn.invalid error" when {
      "it has no-alphanumeric characters" in {
        bind("VAT*$") should matchPattern {
          case Left(List(FormError("testKey", List("error.trn.invalid"), _))) =>
        }
        bind("VAT**12") should matchPattern {
          case Left(List(FormError("testKey", List("error.trn.invalid"), _))) =>
        }

        bind("VAT**12222") should matchPattern {
          case Left(List(FormError("testKey", List("error.trn.invalid"), _))) =>
        }
      }
    }
  }

  "Business telephone binding" should {
    val mapping = businessTelephone.withPrefix("testKey")
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid telephone number" in {
      bind("0048 605 555 555") shouldBe Right("0048 605 555 555")
    }

    s"give error.telephone.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.telephone.blank")
    }

    s"give error.telephone.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.telephone.blank")
    }

    s"give error.telephone.maxlength error when the size is more than 24 chars" in {
      bind((1 to 18).mkString).left.value should contain only FormError("testKey", s"error.telephone.maxlength")
    }

    s"give error.telephone.invalid error" when {
      "it has no-alphanumeric characters" in {
        bind("VAT$") should matchPattern {
          case Left(List(FormError("testKey", List("error.telephone.invalid"), _))) =>
        }
      }
      "it has letters in small case" in {
        bind("var") should matchPattern {
          case Left(List(FormError("testKey", List("error.telephone.invalid"), _))) =>
        }
      }
    }
  }

  "addressLine12 bind" should {
    def unprefixedAddressLine12Mapping(lineNumber: Int) = addressLine12(lineNumber)

    behave like anAddressLineValidatingMapping(unprefixedAddressLine12Mapping(1), 1)
    behave like anAddressLineValidatingMapping(unprefixedAddressLine12Mapping(2), 2)

    val addressLine12Mapping = unprefixedAddressLine12Mapping(1).withPrefix("testKey")

    def bind(fieldValue: String) = addressLine12Mapping.bind(Map("testKey" -> fieldValue))

    "reject the line" when {
      "field is not present" in {
        addressLine12Mapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", "error.addressline.1.empty"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain(FormError("testKey", "error.addressline.1.empty"))
      }
    }
  }

  "businessName bind" should {

    val businessNameMapping = businessName.withPrefix("testKey")

    def bind(fieldValue: String) = businessNameMapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String) =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.business-name.invalid"), _))) =>
      }

    def shouldRejectFieldValueAsTooLong(fieldValue: String) =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.business-name.maxlength"), _))) =>
      }

    def shouldAcceptFieldValue(fieldValue: String) =
      bind(fieldValue) shouldBe Right(fieldValue)

    "reject business name" when {

      "there is an ampersand character" in {
        shouldRejectFieldValueAsInvalid("My Agency & Co")
      }

      "there is an apostrophe character" in {
        shouldRejectFieldValueAsInvalid("My Agency's Co")
      }

      "there is an invalid character" in {
        shouldRejectFieldValueAsInvalid("a#a")
        shouldRejectFieldValueAsInvalid("a~a")
        shouldRejectFieldValueAsInvalid(s"a$$a")
        shouldRejectFieldValueAsInvalid("a@a")
        shouldRejectFieldValueAsInvalid("a=a")
        shouldRejectFieldValueAsInvalid("a+a")
        shouldRejectFieldValueAsInvalid("a£a")
        shouldRejectFieldValueAsInvalid("a`a")
      }

      "there are more than 40 characters" in {
        val tooLong = "12345678901234567890123456789012345678901"
        tooLong.length shouldBe 41
        shouldRejectFieldValueAsTooLong(tooLong)
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", "error.business-name.empty"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", "error.business-name.empty")
      }

      "field is not present" in {
        businessNameMapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }
    }

    "accept business name" when {
      "there are valid characters" in {
        shouldAcceptFieldValue("My Agency")
        shouldAcceptFieldValue("My/Agency")
        shouldAcceptFieldValue("My--Agency")
        shouldAcceptFieldValue("My,Agency")
        shouldAcceptFieldValue("My.Agency")
      }

      "there are numbers and letters" in {
        shouldAcceptFieldValue("The 100 Agency")
      }
    }
  }

  private def anAddressLineValidatingMapping(unprefixedAddressLineMapping: Mapping[String], lineNumber: Int): Unit = {

    val addressLine1Mapping = unprefixedAddressLineMapping.withPrefix("testKey")
    val invalidError = s"error.addressline.$lineNumber.invalid"

    def bind(fieldValue: String) = addressLine1Mapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String) =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List(`invalidError`), _))) =>
      }

    def shouldRejectFieldValueAsTooLong(fieldValue: String) =
      bind(fieldValue) shouldBe Left(
        List(FormError("testKey", List(s"error.addressline.$lineNumber.maxlength"), List(35))))

    def shouldAcceptFieldValue(fieldValue: String) =
      if (fieldValue.isEmpty) bind(fieldValue) shouldBe Right(None)
      else bind(fieldValue) shouldBe Right(fieldValue)

    s"reject the address line $lineNumber" when {
      "there is an character that is not allowed by the agency address regex" in {
        shouldRejectFieldValueAsInvalid("My Agency street<script> City")
        shouldRejectFieldValueAsInvalid("My Agency street City~City")
        shouldRejectFieldValueAsInvalid("My Agency street City/City")
      }

      "the line is too long for DES" in {
        val tooLong = "123456789012345678901234567890123456"
        tooLong.length shouldBe 36
        shouldRejectFieldValueAsTooLong(tooLong)
      }
    }

    s"accept the address line $lineNumber" when {
      "there is text and numbers" in {
        shouldAcceptFieldValue("99 My Agency address")
      }

      "there are valid symbols in the input" in {
        shouldAcceptFieldValue("a-a")
        shouldAcceptFieldValue("a,a")
        shouldAcceptFieldValue("a.a")
        shouldAcceptFieldValue("a&a")
        shouldAcceptFieldValue("a'a")
      }

      "there is a valid address" in {
        shouldAcceptFieldValue("My Agency address")
      }

      "it is the maximum allowable length" in {
        val atMax = "12345678901234567890123456789012345"
        atMax.length shouldBe 35
        shouldAcceptFieldValue(atMax)
      }
    }

    s"accumulate errors if there are multiple validation problems for addressline $lineNumber" in {
      val tooLongAndNonMatchingLine = "123456789012345678901234567890123456<"
      bind(tooLongAndNonMatchingLine) shouldBe Left(
        List(
          FormError("testKey", s"error.addressline.$lineNumber.maxlength", Seq(35)),
          FormError("testKey", s"error.addressline.$lineNumber.invalid", Seq())))
    }
  }

  "countryCode bind" should {
    val countryCode = CommonValidators.countryCode(Set("GB", "IE", "IN")).withPrefix("testKey")

    def bind(fieldValue: String) = countryCode.bind(Map("testKey" -> fieldValue))

    "return error if no country code is present" in {
      bind("").left.value should contain(FormError("testKey", "error.country.empty"))
    }

    "return error if invalid country code is present" in {
      bind("INVALID").left.value should contain(FormError("testKey", "error.country.invalid"))
    }

    "return country code if the value is present" in {
      bind("IE").right.value shouldBe "IE"
    }
  }

  def randomString(limit: Int): String = {
    def nextAlphaNum: Char = {
      val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
      chars charAt (Random.nextInt(chars.length))
    }
    (Stream.continually(nextAlphaNum)).take(limit).mkString
  }
}
