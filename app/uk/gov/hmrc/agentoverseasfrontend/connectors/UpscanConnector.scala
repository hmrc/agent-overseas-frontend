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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models.upscan.UpscanInitiate
import uk.gov.hmrc.agentoverseasfrontend.utils.HttpAPIMonitor
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UpscanConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient,
  val metrics: Metrics
)(implicit
  val ec: ExecutionContext
)
extends HttpAPIMonitor {

  val upscanUrl = s"${appConfig.upscanBaseUrl}/upscan/initiate"

  val callBackUrl = s"${appConfig.agentOverseasApplicationBaseUrl}/agent-overseas-application/upscan-callback"

  val maxFileSize = 5000000 // 5MB

  val payload: JsValue = Json.parse(s"""{
                                       |"callbackUrl": "$callBackUrl",
                                       |"minimumFileSize": 1000,
                                       |"maximumFileSize": $maxFileSize
                                       |}
    """.stripMargin)

  def initiate()(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UpscanInitiate] =
    monitor("ConsumedAPI-upscan-initiate-POST") {
      httpClient
        .POST[JsValue, JsValue](
          upscanUrl,
          payload,
          Seq("content-Type" -> "application/json")
        )
        .map(_.as[UpscanInitiate])

    }

}
