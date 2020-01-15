package uk.gov.hmrc.agentoverseasfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.support.WireMockSupport

trait AgentSubscriptionStubs { me: WireMockSupport =>
  private val pathOverseasSubscription = "/agent-subscription/overseas-subscription"

  def givenSubscriptionSuccessfulResponse(arn: Arn): StubMapping =
    stubFor(put(urlEqualTo(pathOverseasSubscription)).willReturn(
      okJson(s"""{ "arn" : "${arn.value}" }""")
        .withStatus(201)
    ))

  def givenSubscriptionFailed(withStatus: Int): StubMapping =
    stubFor(put(urlEqualTo(pathOverseasSubscription))
      .willReturn(aResponse()
        .withStatus(withStatus))
    )

  def givenSubscriptionFailedUnauthorized(): StubMapping = givenSubscriptionFailed(401)
  def givenSubscriptionFailedForbidden(): StubMapping = givenSubscriptionFailed(403)
  def givenSubscriptionFailedConflict(): StubMapping = givenSubscriptionFailed(409)
  def givenSubscriptionFailedServerError(): StubMapping = givenSubscriptionFailed(500)
  def givenSubscriptionFailedUnavailable(): StubMapping = givenSubscriptionFailed(503)
}
