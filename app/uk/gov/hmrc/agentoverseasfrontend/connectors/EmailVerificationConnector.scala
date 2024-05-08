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

package uk.gov.hmrc.agentoverseasfrontend.connectors

import play.api.Logging
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models.{VerificationStatusResponse, VerifyEmailRequest, VerifyEmailResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject()(http: HttpClient)(implicit val appConfig: AppConfig) extends Logging {

  def verifyEmail(request: VerifyEmailRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[VerifyEmailResponse]] = {
    val url = s"${appConfig.emailVerificationBaseUrl}/email-verification/verify-email"

    http.POST[VerifyEmailRequest, HttpResponse](url, request).map { response =>
      response.status match {
        case 201 => Some(response.json.as[VerifyEmailResponse])
        case status =>
          logger.error(s"verifyEmail error for $request; HTTP status: $status, message: $response")
          None
      }
    }
  }

  def checkEmail(
    credId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[VerificationStatusResponse]] = {
    val url = s"${appConfig.emailVerificationBaseUrl}/email-verification/verification-status/$credId"

    http.GET[HttpResponse](url).map { response =>
      response.status match {
        case 200 => Some(response.json.as[VerificationStatusResponse])
        case 404 => Some(VerificationStatusResponse(List.empty))
        case status =>
          logger.error(s"email verification status error for $credId; HTTP status: $status, message: $response")
          None
      }
    }
  }
}
