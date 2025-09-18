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

import play.api.http.Status.NOT_FOUND
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.utils.RequestSupport._
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentOverseasApplicationConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2,
  val metrics: Metrics
)(implicit val ec: ExecutionContext) {

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

  private val queryParams: String = ApplicationStatus.allStatuses
    .map(status => s"statusIdentifier=${status.key}")
    .mkString("&")
  private val urlString: String = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application?$queryParams"

  def getUserApplications(implicit rh: RequestHeader): Future[List[ApplicationEntityDetails]] = http.get(url"$urlString")
    .execute[HttpResponse]
    .map {
      response =>
        response.status match {
          case OK => response.json.as[List[ApplicationEntityDetails]]
          case NOT_FOUND => List.empty
          case s =>
            throw new RuntimeException(
              s"Could not retrieve overseas agent application status $urlString, status: $s"
            )
        }
    }

  def createOverseasApplication(
    request: CreateOverseasApplicationRequest
  )(implicit rh: RequestHeader): Future[Unit] = {
    val url = url"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case status if is4xx(status) || is5xx(status) => throw new Exception(s"createOverseasApplication returned status ${response.status}")
          case _ => ()
        }
      }
  }

  def upscanPollStatus(
    reference: String
  )(implicit rh: RequestHeader): Future[FileUploadStatus] = {
    val url = url"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/upscan-poll-status/$reference"
    http.get(url).execute[FileUploadStatus]
  }

  def allApplications(implicit rh: RequestHeader): Future[List[OverseasApplication]] = {
    val url = url"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application"
    http.get(url)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => response.json.as[List[OverseasApplication]]
          case NOT_FOUND => List.empty[OverseasApplication]
          case _ => throw UpstreamErrorResponse(s"allApplications error: ${response.body}", response.status)
        }
      }
  }

  def updateApplicationWithAgencyDetails(
    agencyDetails: AgencyDetails
  )(implicit rh: RequestHeader): Future[Unit] = {
    val url = url"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application"

    import AgencyDetails.formats
    http.put(url)
      .withBody((Json.toJson(agencyDetails)))
      .execute[HttpResponse]
      .map { response: HttpResponse =>
        if (response.status == 204)
          ()
        else {
          val msg = s"agent-overseas-application returned status ${response.status}"
          if (is4xx(response.status))
            throw UpstreamErrorResponse(
              msg,
              response.status,
              400
            )
          else if (is5xx(response.status))
            throw UpstreamErrorResponse(
              msg,
              response.status,
              500
            )
          else
            throw new RuntimeException(msg)
        }
      }
  }

  def updateAuthId(oldAuthId: ProviderId)(implicit rh: RequestHeader): Future[Unit] = {
    val url = url"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application/auth-provider-id"
    http.put(url)
      .withBody(Json.obj("authId" -> oldAuthId.value))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case NOT_FOUND =>
            throw new NotFoundException(
              s"createOverseasApplication not found for authId: $oldAuthId, Http Status: ${response.status}"
            )
          case status if is4xx(status) || is5xx(status) =>
            throw UpstreamErrorResponse(s"createOverseasApplication Error for authId: $oldAuthId", response.status)
          case _ => ()
        }
      }
  }

}
