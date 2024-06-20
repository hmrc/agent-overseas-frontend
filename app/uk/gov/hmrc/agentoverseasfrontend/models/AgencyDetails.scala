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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import uk.gov.hmrc.agentoverseasfrontend.utils.compareEmail

case class AgencyDetails(
  agencyName: String,
  agencyEmail: String,
  agencyAddress: OverseasAddress,
  verifiedEmails: Set[String]
) {
  def isEmailVerified(email: String): Boolean = verifiedEmails.exists(compareEmail(email, _))
  def isEmailVerified: Boolean = isEmailVerified(agencyEmail)
}

object AgencyDetails {
  // manual instance for backwards compatibility if there are any stored details without 'verifiedEmails' field
  // We can remove this and have an auto-generated Format after this has been in production for some time.
  val reads: Reads[AgencyDetails] = (
    (__ \ "agencyName").read[String] and
      (__ \ "agencyEmail").read[String] and
      (__ \ "agencyAddress").read[OverseasAddress] and
      (__ \ "verifiedEmails").readWithDefault[Set[String]](Set.empty)
  )(AgencyDetails.apply _)
  val writes: Writes[AgencyDetails] = Json.writes[AgencyDetails]
  implicit val formats: Format[AgencyDetails] = Format(reads, writes)

  def fromOverseasApplication(overseasApplication: OverseasApplication): AgencyDetails =
    AgencyDetails(
      agencyName = overseasApplication.tradingDetails.tradingName,
      agencyEmail = overseasApplication.contactDetails.businessEmail,
      agencyAddress = overseasApplication.tradingDetails.tradingAddress,
      verifiedEmails = Set(
        overseasApplication.contactDetails.businessEmail
      ) // When creating AgencyDetails from an overseas application we assume the email has already been verified.
    )
}
