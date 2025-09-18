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

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentoverseasfrontend.models.Arn
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.utils.RequestSupport._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class OverseasSubscriptionResponse(arn: Arn)

object OverseasSubscriptionResponse {
  implicit val formats: Format[OverseasSubscriptionResponse] = Json.format[OverseasSubscriptionResponse]
}

@Singleton
class AgentSubscriptionConnector @Inject() (
  http: HttpClientV2,
  val metrics: Metrics
)(implicit
  val appConfig: AppConfig,
  val ec: ExecutionContext
) {

  def overseasSubscription(implicit rh: RequestHeader): Future[Arn] = {
    val url = url"${appConfig.agentSubscriptionBaseUrl}/agent-subscription/overseas-subscription"
    http.put(url)
      .execute[OverseasSubscriptionResponse]
      .map(_.arn)
  }
}
