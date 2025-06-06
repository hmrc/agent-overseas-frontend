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

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class MaintainerDetails(reviewedDate: LocalDateTime)

object MaintainerDetails {
  implicit val format: OFormat[MaintainerDetails] = Json.format[MaintainerDetails]
}

case class ApplicationEntityDetails(
  applicationCreationDate: LocalDateTime,
  status: ApplicationStatus,
  tradingName: String,
  businessEmail: String,
  maintainerReviewedOn: Option[LocalDateTime]
)

object ApplicationEntityDetails {
  implicit val reads: Reads[ApplicationEntityDetails] =
    ((__ \ "createdDate").read[LocalDateTime] and
      (__ \ "status").read[ApplicationStatus] and
      (__ \ "tradingDetails" \ "tradingName").read[String] and
      (__ \ "contactDetails" \ "businessEmail").read[String] and
      (__ \ "maintainerDetails")
        .readNullable[MaintainerDetails])(
      (
        createdDate,
        status,
        name,
        email,
        maintainerDetails
      ) =>
        ApplicationEntityDetails(
          createdDate,
          status,
          name,
          email,
          maintainerDetails.map(_.reviewedDate)
        )
    )
}
