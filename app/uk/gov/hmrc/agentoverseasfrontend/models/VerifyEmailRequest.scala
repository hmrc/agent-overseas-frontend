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

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

case class VerifyEmailRequest(
  credId: String,
  continueUrl: String,
  origin: String,
  deskproServiceName: Option[String],
  accessibilityStatementUrl: String,
  email: Option[Email],
  lang: Option[String],
  backUrl: Option[String],
  pageTitle: Option[String]
)

case class Email(
  address: String,
  enterUrl: String
)

object Email {
  implicit val format: Format[Email] = Json.format[Email]
}
object VerifyEmailRequest {
  implicit val writes: Writes[VerifyEmailRequest] = Json.writes[VerifyEmailRequest]
}

case class VerifyEmailResponse(redirectUri: String)

object VerifyEmailResponse {
  implicit val formats: Format[VerifyEmailResponse] = Json.format[VerifyEmailResponse]
}

case class CompletedEmail(
  emailAddress: String,
  verified: Boolean,
  locked: Boolean
)

object CompletedEmail {
  implicit val reads: Reads[CompletedEmail] = Json.reads[CompletedEmail]
}

case class VerificationStatusResponse(emails: List[CompletedEmail])

object VerificationStatusResponse {
  implicit val reads: Reads[VerificationStatusResponse] = Json.reads[VerificationStatusResponse]
}

sealed trait EmailVerificationStatus
object EmailVerificationStatus {

  case object Verified
  extends EmailVerificationStatus
  case object Unverified
  extends EmailVerificationStatus
  case object Locked
  extends EmailVerificationStatus
  case object Error
  extends EmailVerificationStatus

}
