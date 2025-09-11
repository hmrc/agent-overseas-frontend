/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentoverseasfrontend.models.Arn
import uk.gov.hmrc.agentoverseasfrontend.support.WireMockSupport

trait AgentSubscriptionStubs { me: WireMockSupport =>

  private val pathOverseasSubscription = "/agent-subscription/overseas-subscription"

  def givenSubscriptionSuccessfulResponse(arn: Arn): StubMapping = stubFor(
    put(urlEqualTo(pathOverseasSubscription)).willReturn(
      okJson(s"""{ "arn" : "${arn.value}" }""")
        .withStatus(201)
    )
  )

  def givenSubscriptionFailed(withStatus: Int): StubMapping = stubFor(
    put(urlEqualTo(pathOverseasSubscription))
      .willReturn(
        aResponse()
          .withStatus(withStatus)
      )
  )

  def givenSubscriptionFailedUnauthorized(): StubMapping = givenSubscriptionFailed(401)
  def givenSubscriptionFailedForbidden(): StubMapping = givenSubscriptionFailed(403)
  def givenSubscriptionFailedConflict(): StubMapping = givenSubscriptionFailed(409)
  def givenSubscriptionFailedServerError(): StubMapping = givenSubscriptionFailed(500)
  def givenSubscriptionFailedUnavailable(): StubMapping = givenSubscriptionFailed(503)

}
