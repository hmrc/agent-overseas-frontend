/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasfrontend.models

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentoverseasfrontend.utils.compareEmail
import uk.gov.hmrc.mongo.cache.DataKey

import scala.collection.immutable.SortedSet

case class AgentSession(
  amlsRequired: Option[Boolean] = None,
  amlsDetails: Option[AmlsDetails] = None,
  contactDetails: Option[ContactDetails] = None,
  tradingName: Option[String] = None,
  overseasAddress: Option[OverseasAddress] = None,
  registeredWithHmrc: Option[YesNo] = None,
  agentCodes: Option[AgentCodes] = None,
  registeredForUkTax: Option[YesNo] = None,
  personalDetails: Option[PersonalDetailsChoice] = None,
  companyRegistrationNumber: Option[CompanyRegistrationNumber] = None,
  hasTaxRegNumbers: Option[Boolean] = None,
  taxRegistrationNumbers: Option[SortedSet[Trn]] = None,
  tradingAddressUploadStatus: Option[FileUploadStatus] = None,
  amlsUploadStatus: Option[FileUploadStatus] = None,
  trnUploadStatus: Option[FileUploadStatus] = None,
  fileType: Option[String] = None,
  changingAnswers: Boolean = false,
  hasTrnsChanged: Boolean = false,
  verifiedEmails: Set[String] = Set.empty
) {

  def emailNeedsVerifying(email: String): Boolean = !verifiedEmails.exists(compareEmail(email, _))

  def emailNeedsVerifying: Boolean = contactDetails.exists(details => emailNeedsVerifying(details.businessEmail))

  def sanitize: AgentSession = {
    val agentCodes =
      if (this.registeredWithHmrc.contains(Yes))
        this.agentCodes
      else
        None

    val registeredForUkTax = this.registeredForUkTax

    val personalDetails =
      if (registeredForUkTax.contains(Yes))
        this.personalDetails
      else
        None
    val companyRegistrationNumber = registeredForUkTax.flatMap(_ => this.companyRegistrationNumber)
    val taxRegistrationNumbers = registeredForUkTax.flatMap(_ => this.taxRegistrationNumbers)

    AgentSession(
      this.amlsRequired,
      this.amlsDetails,
      this.contactDetails,
      this.tradingName,
      this.overseasAddress,
      this.registeredWithHmrc,
      agentCodes,
      registeredForUkTax,
      personalDetails,
      companyRegistrationNumber,
      this.hasTaxRegNumbers,
      taxRegistrationNumbers,
      this.tradingAddressUploadStatus,
      this.amlsUploadStatus,
      this.trnUploadStatus,
      this.fileType,
      this.changingAnswers,
      this.hasTrnsChanged,
      this.verifiedEmails
    )
  }

}

object AgentSession {

  val sessionKey: DataKey[AgentSession] = DataKey[AgentSession]("agentSession")

  def empty: AgentSession = AgentSession()

  implicit val format: OFormat[AgentSession] = Json.format[AgentSession]

  object MissingAmlsRequired {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.amlsRequired.isEmpty)
  }

  object MissingAmlsDetails {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.amlsRequired.contains(true)) && session.exists(_.amlsDetails.isEmpty)
  }

  object MissingAmlsUploadStatus {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.amlsRequired.contains(true)) && session.exists(_.amlsUploadStatus.isEmpty)
  }

  object MissingContactDetails {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.contactDetails.isEmpty)
  }

  object MissingTradingName {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.tradingName.isEmpty)
  }

  object MissingTradingAddress {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.overseasAddress.isEmpty)
  }

  object MissingTradingAddressUploadStatus {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.tradingAddressUploadStatus.isEmpty)
  }

  object MissingRegisteredWithHmrc {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.registeredWithHmrc.isEmpty)
  }

  object IsRegisteredWithHmrc {
    def unapply(session: Option[AgentSession]): Option[YesNo] = session.flatMap(_.registeredWithHmrc)
  }

  object MissingAgentCodes {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.agentCodes.isEmpty)
  }

  object HasAnsweredAgentCodes {
    def unapply(session: Option[AgentSession]): Boolean = session.flatMap(_.agentCodes).isDefined
  }

  object MissingRegisteredForUkTax {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.registeredForUkTax.isEmpty)
  }

  object IsRegisteredForUkTax {
    def unapply(session: Option[AgentSession]): Option[YesNo] = session.flatMap(_.registeredForUkTax)
  }

  object MissingPersonalDetails {
    def unapply(session: Option[AgentSession]): Boolean =
      session.flatMap(_.registeredForUkTax) match {
        case Some(No) => false
        case _ => session.exists(_.personalDetails.isEmpty)
      }
  }

  object MissingCompanyRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.companyRegistrationNumber.isEmpty)
  }

  object MissingHasTaxRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.hasTaxRegNumbers.isEmpty)
  }

  object HasTaxRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.hasTaxRegNumbers.getOrElse(false))
  }

  object NoTaxRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(
      _.hasTaxRegNumbers.getOrElse(true) == false
    ) // interested in false so getOrElse(true) is the bad case
  }

  object TaxRegistrationNumbersEmpty {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.taxRegistrationNumbers.isEmpty)
  }

  object MissingTaxRegFile {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.trnUploadStatus.isEmpty)
  }

  object EmailUnverified {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.emailNeedsVerifying)
  }

}
