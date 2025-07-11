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

import java.time.LocalDateTime

import play.api.libs.json._

case class SubscriptionContactDetails(businessEmail: String)

object SubscriptionContactDetails {
  implicit val format: OFormat[SubscriptionContactDetails] = Json.format[SubscriptionContactDetails]
}

case class SubscriptionTradingDetails(
  tradingName: String,
  tradingAddress: OverseasAddress
)

object SubscriptionTradingDetails {
  implicit val format: OFormat[SubscriptionTradingDetails] = Json.format[SubscriptionTradingDetails]
}

case class OverseasApplication(
  createdDate: LocalDateTime,
  status: ApplicationStatus,
  contactDetails: SubscriptionContactDetails,
  tradingDetails: SubscriptionTradingDetails,
  agencyDetails: Option[AgencyDetails]
)

object OverseasApplication {
  implicit val format: OFormat[OverseasApplication] = Json.format[OverseasApplication]
}
