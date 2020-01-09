/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.data.Forms.{optional, text}
import play.api.data.Mapping
import play.api.data.validation._
import uk.gov.hmrc.domain.Nino

object CommonValidators {

  private val TelephoneNumberRegex = "^[A-Z0-9 )\\/(\\-*#]*"
  private val EmailLocalPartRegex =
    """^[a-zA-Z0-9\.!#$%&'*+\/=?^_`{|}~-]+(?<!\.)$"""
  private val EmailDomainRegex =
    """[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  private val JobTitleRegex = "[a-zA-Z' \\-\\s]+"
  private val NameRegex = "[a-zA-Z' \\-\\s]+"
  private val MembershipNumberRegex = "[a-zA-Z0-9\\/ \\-\\s]+"
  private val AgentCodeRegex = """^[a-zA-Z0-9]*$"""
  private val CrnRegex = "^[A-Za-z0-9 \\-.\\/]*$"
  private val TrnRegex = """^[a-zA-Z0-9 ]*$"""
  private val OverseasTradingNameRegex = "^[A-Za-z0-9 \\-\\/]*"
  private val OverseasAddressRegex = "^[A-Za-z0-9 \\-,.&']*"
  private val AmlsBodyRegex = "^[A-Za-z0-9 \\-,.'&()\\/]*$"

  private val AgentCodeMaxLength = 6
  private val UtrMaxLength = 10
  private val JobTitleMinLength = 2
  private val JobTitleMaxLength = 50
  private val NameMaxLength = 35
  private val MembershipNumberMaxLength = 24
  private val TelephoneMaxLength = 24
  private val EmailMaxLength = 132
  private val TradingNameMaxLength = 40
  private val AddresslineMaxLength = 35
  private val CrnMaxLength = 40
  private val TrnMaxLength = 24
  private val AmlsBodyMaxLength = 100

  private type UtrErrors = (String, String)
  private val DefaultUtrErrors = ("error.utr.blank", "error.utr.invalid")

  def saAgentCode = text verifying agentCodeConstraint("saAgentCode")

  def ctAgentCode: Mapping[String] =
    text verifying agentCodeConstraint("ctAgentCode")

  def saUtr: Mapping[String] =
    text verifying utrConstraint(("error.sautr.blank", "error.sautr.invalid"))

  def nino: Mapping[String] = text verifying ninoConstraint

  def amlsBody: Mapping[String] =
    text verifying commonFormConstraint("moneyLaunderingCompliance.amlsbody", AmlsBodyRegex, AmlsBodyMaxLength)

  def jobTitle: Mapping[String] = text verifying jobTitleConstraint

  def firstName: Mapping[String] =
    text verifying commonFormConstraint("firstName", NameRegex, NameMaxLength)

  def lastName: Mapping[String] =
    text verifying commonFormConstraint("lastName", NameRegex, NameMaxLength)

  def membershipNumber: Mapping[Option[String]] =
    optional(
      text
        .verifying(maxLength(MembershipNumberMaxLength, "error.membershipNumber.maxlength"))
        .verifying(validateRegex(MembershipNumberRegex, "error.membershipNumber.invalid")))

  def businessTelephone: Mapping[String] =
    text verifying commonFormConstraint("telephone", TelephoneNumberRegex, TelephoneMaxLength)

  def businessEmail: Mapping[String] = text verifying validEmailAddress

  def tradingName: Mapping[String] =
    text verifying commonFormConstraint("tradingName", OverseasTradingNameRegex, TradingNameMaxLength)

  def addressLine12(lineNumber: Int): Mapping[String] =
    text verifying commonFormConstraint(s"addressline.$lineNumber", OverseasAddressRegex, AddresslineMaxLength)

  def addressLine34(lineNumber: Int): Mapping[Option[String]] =
    optional(addressLine12(lineNumber))

  def companyRegistrationNumber: Mapping[String] =
    text verifying commonFormConstraint("crn", CrnRegex, CrnMaxLength)

  def taxRegistrationNumber: Mapping[String] =
    text verifying commonFormConstraint("trn", TrnRegex, TrnMaxLength)

  def countryCode(validCountryCodes: Set[String]): Mapping[String] =
    text.verifying(validCountryCode(validCountryCodes))

  def radioInputSelected[T](message: String = "error.no-radio-selected"): Constraint[Option[T]] =
    Constraint[Option[T]] { fieldValue: Option[T] =>
      if (fieldValue.isDefined)
        Valid
      else
        Invalid(ValidationError(message))
    }

  private def validCountryCode(codes: Set[String]) = Constraint { fieldValue: String =>
    val code = fieldValue.trim
    nonEmptyWithMessage("error.country.empty")(code) match {
      case i: Invalid => i
      case Valid =>
        if (codes.contains(code) && code != "GB" && code.size == 2)
          Valid
        else
          Invalid(ValidationError("error.country.invalid"))
    }
  }

  private def agentCodeConstraint(
    codeType: String,
    regex: String = AgentCodeRegex,
    maxLength: Int = AgentCodeMaxLength): Constraint[String] =
    Constraint[String] { fieldValue: String =>
      val formattedCode = fieldValue.replace(" ", "")

      if (formattedCode.isEmpty)
        Invalid(ValidationError(s"error.$codeType.blank"))
      else if (!formattedCode.matches(regex))
        Invalid(ValidationError(s"error.$codeType.invalid"))
      else if (formattedCode.length != maxLength)
        Invalid(ValidationError(s"error.$codeType.maxlength"))
      else
        Valid
    }

  private def utrConstraint(errorMessages: UtrErrors = DefaultUtrErrors): Constraint[String] =
    Constraint[String] { fieldValue: String =>
      val formattedField = fieldValue.replace(" ", "")
      val (blank, invalid) = errorMessages

      def isNumber(str: String): Boolean =
        str.map(_.isDigit).reduceOption(_ && _).getOrElse(false)

      Constraints.nonEmpty.apply(formattedField) match {
        case _: Invalid => Invalid(ValidationError(blank))
        case _ if !isNumber(formattedField) || formattedField.length != UtrMaxLength =>
          Invalid(ValidationError(invalid))
        case _ => Valid
      }
    }

  private val ninoConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    val formattedField = fieldValue.replaceAll("\\s", "").toUpperCase

    Constraints.nonEmpty.apply(formattedField) match {
      case _: Invalid => Invalid(ValidationError("error.nino.blank"))
      case _ if !Nino.isValid(formattedField) =>
        Invalid(ValidationError("error.nino.invalid"))
      case _ => Valid
    }
  }

  private def amlsBodyConstraint(bodies: Set[String]): Constraint[String] =
    Constraint[String] { fieldValue: String =>
      Constraints.nonEmpty.apply(fieldValue) match {
        case _: Invalid =>
          Invalid(ValidationError("error.moneyLaunderingCompliance.amlsbody.empty"))
        case _ if !validateAMLSBodies(fieldValue, bodies) =>
          Invalid(ValidationError("error.moneyLaunderingCompliance.amlsbody.invalid"))
        case _ => Valid
      }
    }

  private val jobTitleConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty.apply(fieldValue) match {
      case _: Invalid => Invalid(ValidationError("error.jobTitle.blank"))
      case _ if !fieldValue.matches(JobTitleRegex) =>
        Invalid(ValidationError("error.jobTitle.invalid"))
      case _ if fieldValue.length < JobTitleMinLength || fieldValue.length > JobTitleMaxLength =>
        Invalid(ValidationError("error.jobTitle.length"))
      case _ => Valid
    }
  }

  private def commonFormConstraint(formType: String, regex: String, maxLength: Int): Constraint[String] =
    Constraint[String] { fieldValue: String =>
      Constraints.nonEmpty.apply(fieldValue) match {
        case _: Invalid => Invalid(ValidationError(s"error.$formType.blank"))
        case _ if !fieldValue.matches(regex) =>
          Invalid(ValidationError(s"error.$formType.invalid"))
        case _ if fieldValue.length > maxLength =>
          Invalid(ValidationError(s"error.$formType.maxlength"))
        case _ => Valid
      }
    }

  private def validateRegex(regex: String, msgKeyInvalid: String): Constraint[String] =
    Constraint[String] { fieldValue: String =>
      Constraints.nonEmpty.apply(fieldValue) match {
        case i: Invalid => i
        case Valid =>
          fieldValue match {
            case value if !value.matches(regex) =>
              Invalid(ValidationError(msgKeyInvalid))
            case _ => Valid
          }
      }
    }

  // Same as play.api.data.validation.Constraints.maxLength but with a chance to use a custom message instead of error.maxLength
  private def maxLength(length: Int, messageKey: String = "error.maxLength"): Constraint[String] =
    Constraint[String]("constraint.maxLength", length) { o =>
      require(length >= 0, "string maxLength must not be negative")
      if (o == null) Invalid(ValidationError(messageKey, length))
      else if (o.length <= length) Valid
      else Invalid(ValidationError(messageKey, length))
    }

  def nonEmptyTextWithMsg(errorMessageKey: String): Mapping[String] =
    text verifying nonEmptyWithMessage(errorMessageKey)

  // Same as play.api.data.validation.Constraints.nonEmpty but with a custom message instead of error.required
  private def nonEmptyWithMessage(messageKey: String): Constraint[String] =
    Constraint[String] { (o: String) =>
      if (o == null) Invalid(ValidationError(messageKey))
      else if (o.trim.isEmpty) Invalid(ValidationError(messageKey))
      else Valid
    }

  private def validEmailAddress = Constraint { fieldValue: String =>
    nonEmptyWithMessage("error.email.blank")(fieldValue) match {
      case i: Invalid => i
      case Valid => {
        if (fieldValue.size > EmailMaxLength) {
          Invalid(ValidationError("error.email.maxlength"))
        } else if (fieldValue.contains('@')) {
          val email = fieldValue.split('@')
          if (!email(0).matches(EmailLocalPartRegex) || !email(1).matches(EmailDomainRegex)) {
            Invalid(ValidationError("error.email.invalid"))
          } else Constraints.emailAddress(fieldValue)
        } else
          Invalid(ValidationError("error.email.invalid"))
      }
    }
  }

  private def validateAMLSBodies(amlsBody: String, bodies: Set[String]): Boolean =
    bodies.contains(amlsBody)
}
