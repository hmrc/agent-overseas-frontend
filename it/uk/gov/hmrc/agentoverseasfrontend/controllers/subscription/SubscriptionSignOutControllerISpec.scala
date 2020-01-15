package uk.gov.hmrc.agentoverseasfrontend.controllers.subscription

import java.net.URLEncoder

import play.api.test.FakeRequest
import play.api.test.Helpers.LOCATION
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class SubscriptionSignOutControllerISpec extends BaseISpec {
  lazy val controller = app.injector.instanceOf[SubscriptionSignOutController]

  "signOutWithContinueUrl" should {
    "storeAuthProviderId and redirect to GgCreateAccount" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)

      val result = await(controller.signOutWithContinueUrl(request))

      val details = findByAuthProviderId("12345-credId")
      val detailsRef = details.map(_.id).get

      status(result) shouldBe 303

      val continueUrl = URLEncoder.encode(s"http://localhost:9414${routes.BusinessIdentificationController.returnFromGGRegistration(detailsRef)}", "UTF-8")
      result.header.headers(LOCATION) should
        include(s"http://localhost:8571/government-gateway-registration-frontend?accountType=agent&origin=unknown&continue=$continueUrl")
    }
  }

  "startSurvey" should {
    "redirect to feedback survey page" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)

      val result = await(controller.startFeedbackSurvey(request))

      status(result) shouldBe 303
      result.header.headers(LOCATION) should include("/feedback/OVERSEAS_AGENTS")
    }
  }

  "/finish-sign-out" should {
    "redirect to the root page" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)

      val result = await(controller.signOut(request))

      status(result) shouldBe 303

      result.header.headers(LOCATION) should include("/")
    }
  }

  "/signed-out" should {
    "forbidden with signed_out page containing link to root" in {
      implicit val request = FakeRequest()

      val result = await(controller.signedOut(request))

      status(result) shouldBe 403

      checkMessageIsDefined("signed-out.header")
      checkMessageIsDefined("signed-out.p1")
    }
  }

  "/timed-out" should {
    "display the timed out page" in {
      implicit val request = FakeRequest()

      val result = await(controller.timedOut(request))

      status(result) shouldBe 403

      checkMessageIsDefined("timed-out.header")
      checkMessageIsDefined("timed-out.p2.link")
    }
  }


  private def findByAuthProviderId(authProviderId: String): Option[SessionDetails] = {
    await(sessionDetailsRepo.find("authProviderId" -> authProviderId).map(results => results.headOption))
  }

}
