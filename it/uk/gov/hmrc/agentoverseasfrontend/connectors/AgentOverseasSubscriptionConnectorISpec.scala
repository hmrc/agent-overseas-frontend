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
      connector.overseasSubscription.futureValue shouldBe Arn("123")
    }

    "return appropriate exception" when {
      "receiving a 401 Unauthorized response (bearer token expired/invalid)" in {
        givenSubscriptionFailedUnauthorized

        val e = connector.overseasSubscription.failed.futureValue
        e shouldBe a[UpstreamErrorResponse]
      }

      "receiving a 403 Forbidden response (user is not Agent or application not in 'accepted' status)" in {
        givenSubscriptionFailedForbidden

        val e = connector.overseasSubscription.failed.futureValue
        e shouldBe a[UpstreamErrorResponse]
      }

      "receiving a 503 response (service is unavailable)" in {
        givenSubscriptionFailedUnavailable

        val e = connector.overseasSubscription.failed.futureValue
        e shouldBe a[UpstreamErrorResponse]
      }

      "receiving a 500 response (internal server error)" in {
        givenSubscriptionFailedServerError

        val e = connector.overseasSubscription.failed.futureValue
        e shouldBe a[UpstreamErrorResponse]
      }
    }
  }
}
