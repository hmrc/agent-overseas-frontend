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

package uk.gov.hmrc.agentoverseasfrontend.connectors

import java.time.{LocalDateTime, ZoneOffset}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, _}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpErrorFunctions._
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpErrorFunctions._

@Singleton
class AgentOverseasApplicationConnector @Inject()(
  appConfig: AppConfig,
  http: HttpClient,
  metrics: Metrics
) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] =
    Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

  val allStatuses = ApplicationStatus.allStatuses
    .map(status => s"statusIdentifier=${status.key}")
    .mkString("&")

  val urlGetAllApplications =
    s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application?$allStatuses"

  def getUserApplications(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[ApplicationEntityDetails]] =
    monitor("ConsumedAPI-Agent-Overseas-Application-application-GET") {

      http.GET[HttpResponse](urlGetAllApplications).map { response =>
        response.status match {
          case OK        => response.json.as[List[ApplicationEntityDetails]]
          case NOT_FOUND => List.empty
          case s =>
            throw new RuntimeException(
              s"Could not retrieve overseas agent application status $urlGetAllApplications, status: $s")
        }
      }
    }

  def createOverseasApplication(
    request: CreateOverseasApplicationRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val url = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application"
    monitor("ConsumedAPI-Agent-Overseas-Application-application-POST") {
      http
        .POST[CreateOverseasApplicationRequest, HttpResponse](url.toString, request)
        .map { response =>
          response.status match {
            case status if is4xx(status) || is5xx(status) =>
              throw new Exception(s"createOverseasApplication Error for $request, Http Status: ${response.status}")
            case _ => ()
          }
        }
    }
  }

  def upscanPollStatus(
    reference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FileUploadStatus] = {
    val url = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/upscan-poll-status/$reference"
    monitor("ConsumedAPI-Agent-overseas-Application-upscan-poll-status-GET") {
      http
        .GET[FileUploadStatus](url.toString)
    }
  }

  def allApplications(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[OverseasApplication]] = {
    val url = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application"
    monitor(s"ConsumedAPI-agent-overseas-application-application-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK        => response.json.as[List[OverseasApplication]]
            case NOT_FOUND => List.empty[OverseasApplication]
            case _         => throw UpstreamErrorResponse(s"allApplications error: ${response.body}", response.status)
          }
        }
    }
  }

  def updateApplicationWithAgencyDetails(
    agencyDetails: AgencyDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val url = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application"

    monitor(s"ConsumedAPI-agent-overseas-application-application-PUT") {

      import AgencyDetails.formats
      http
        .doPut[AgencyDetails](url, agencyDetails)
        .map { response: HttpResponse =>
          if (response.status == 204) ()
          else {
            val msg = s"agent-overseas-application returned status ${response.status}"
            if (is4xx(response.status))
              throw UpstreamErrorResponse(msg, response.status, 400)
            else if (is5xx(response.status))
              throw UpstreamErrorResponse(msg, response.status, 500)
            else
              throw new RuntimeException(msg)
          }
        }
    }
  }

  def updateAuthId(oldAuthId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val url = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application/auth-provider-id"

    monitor(s"ConsumedAPI-agent-overseas-application-auth-provider-id-PUT") {
      http
        .PUT[JsValue, HttpResponse](url.toString, Json.obj("authId" -> oldAuthId))
        .map { response =>
          response.status match {
            case NOT_FOUND =>
              throw new NotFoundException(
                s"createOverseasApplication not found for authId: $oldAuthId, Http Status: ${response.status}")
            case status if is4xx(status) || is5xx(status) =>
              throw UpstreamErrorResponse(s"createOverseasApplication Error for authId: $oldAuthId", response.status)
            case _ => ()
          }
        }
    }
  }
}
