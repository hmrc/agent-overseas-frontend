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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, _}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

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
      http
        .GET[List[ApplicationEntityDetails]](urlGetAllApplications.toString)
        .recover {
          case _: NotFoundException => List.empty
          case e =>
            throw new RuntimeException(s"Could not retrieve overseas agent application status: ${e.getMessage}")
        }
    }

  def createOverseasApplication(request: CreateOverseasApplicationRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {
    val url =
      s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application"
    monitor("ConsumedAPI-Agent-Overseas-Application-application-POST") {
      http
        .POST[CreateOverseasApplicationRequest, HttpResponse](url.toString, request)
        .map(_ => ())
    }
  }

  def upscanPollStatus(
    reference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FileUploadStatus] = {
    val url =
      s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/upscan-poll-status/$reference"
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
        .map {
          case response if response.status == 200 =>
            response.json.as[List[OverseasApplication]]
        }
        .recover {
          case _: NotFoundException => List.empty[OverseasApplication]
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
            if (response.status >= 400 && response.status < 500)
              throw new Upstream4xxResponse(msg, upstreamResponseCode = response.status, reportAs = 400)
            else if (response.status >= 500)
              throw new Upstream5xxResponse(msg, upstreamResponseCode = response.status, reportAs = 500)
            else
              throw new RuntimeException(msg)
          }
        }
    }
  }

  def updateAuthId(oldAuthId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val url = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/application/auth-provider-id"

    monitor(s"ConsumedAPI-agent-overseas-application-auth-provider-id-PUT") {
      http.PUT[JsValue, HttpResponse](url.toString, Json.obj("authId" -> oldAuthId)).map(_ => ())
    }
  }
}
