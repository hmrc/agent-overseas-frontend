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

import play.api.libs.json._
import uk.gov.hmrc.agentmtdidentifiers.model.Utr

case class CreateOverseasApplicationRequest(
  amlsRequired: Boolean,
  amls: Option[AmlsDetails],
  contactDetails: ContactDetails,
  tradingDetails: TradingDetails,
  personalDetails: PersonalDetails,
  amlsFileRef: Option[String],
  tradingAddressFileRef: String,
  taxRegFileRef: Option[String]
)

object CreateOverseasApplicationRequest {

  implicit val formats: OFormat[CreateOverseasApplicationRequest] = Json.format

  def apply(agentSession: AgentSession): CreateOverseasApplicationRequest =
    (for {
      amlsRequired <- agentSession.amlsRequired
      contactDetails <- agentSession.contactDetails
      tradingName <- agentSession.tradingName
      businessAddress <- agentSession.overseasAddress
      isHmrcAgentRegistered <- agentSession.registeredWithHmrc
      tradingAddressFileRef <- agentSession.tradingAddressUploadStatus.map(_.reference)
    } yield CreateOverseasApplicationRequest(
      amlsRequired,
      agentSession.amlsDetails,
      contactDetails,
      TradingDetails(
        tradingName,
        businessAddress,
        agentSession.registeredForUkTax,
        isHmrcAgentRegistered,
        agentSession.agentCodes.flatMap(_.selfAssessment),
        agentSession.agentCodes.flatMap(_.corporationTax),
        agentSession.companyRegistrationNumber.flatMap(_.registrationNumber),
        agentSession.taxRegistrationNumbers
      ),
      PersonalDetails(
        agentSession.personalDetails.flatMap(_.saUtr.map(u => Utr(u.toString()))),
        agentSession.personalDetails.flatMap(_.nino)
      ),
      agentSession.amlsUploadStatus.map(_.reference),
      tradingAddressFileRef,
      agentSession.trnUploadStatus.map(_.reference)
    )).getOrElse(throw new Exception("Could not create application request from agent session"))

}
