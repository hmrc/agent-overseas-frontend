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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class OverseasSubscriptionResponse(arn: Arn)

object OverseasSubscriptionResponse {
  implicit val formats: Format[OverseasSubscriptionResponse] = Json.format[OverseasSubscriptionResponse]
}

@Singleton
class AgentSubscriptionConnector @Inject()(http: HttpClient)(implicit val appConfig: AppConfig) {

  def overseasSubscription(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Arn] = {
    val url = s"${appConfig.agentSubscriptionBaseUrl}/agent-subscription/overseas-subscription"

    http.PUT[String, OverseasSubscriptionResponse](url, "").map(_.arn)
  }
}
