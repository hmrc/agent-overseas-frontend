package uk.gov.hmrc.agentoverseasfrontend.connectors

import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentSubscriptionStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class AgentOverseasSubscriptionConnectorISpec extends BaseISpec with AgentSubscriptionStubs {

  implicit val hc = HeaderCarrier()

  private val connector = app.injector.instanceOf[AgentSubscriptionConnector]

  "AgentOverseasSubscriptionConnector" should {
    "return Arn if subscription was successful" in {
      givenSubscriptionSuccessfulResponse(Arn("123"))
      await(connector.overseasSubscription) shouldBe Arn("123")
    }

    "return appropriate exception" when {
      "receiving a 401 Unauthorized response (bearer token expired/invalid)" in {
        givenSubscriptionFailedUnauthorized
        an[Upstream4xxResponse] shouldBe thrownBy(await(connector.overseasSubscription))
      }

      "receiving a 403 Forbidden response (user is not Agent or application not in 'accepted' status)" in {
        givenSubscriptionFailedForbidden
        an[Upstream4xxResponse] shouldBe thrownBy(await(connector.overseasSubscription))
      }

      "receiving a 503 response (service is unavailable)" in {
        givenSubscriptionFailedUnavailable
        an[Upstream5xxResponse] shouldBe thrownBy(await(connector.overseasSubscription))
      }

      "receiving a 500 response (internal server error)" in {
        givenSubscriptionFailedServerError
        an[Upstream5xxResponse] shouldBe thrownBy(await(connector.overseasSubscription))
      }
    }
  }
}
