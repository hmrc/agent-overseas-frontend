package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import java.net.URLEncoder

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

class ApplicationSignOutControllerISpec extends BaseISpec{

  private val controller: ApplicationSignOutController = app.injector.instanceOf[ApplicationSignOutController]

  "signOut" should {
    "303 lose existing session and redirect to gg sign-in" in {
      val someExistingKey = "storedInSessionKey"
      implicit val requestWithSession = FakeRequest().withSession(someExistingKey -> "valueForKeyInSession")

      val result = controller.signOut(requestWithSession)

      result.futureValue.session.get(someExistingKey) shouldBe None
      redirectLocation(result) shouldBe Some("/baseISpec/gg/sign-in")
    }
  }

  "signOutWithContinueUrl" should {
    "303 to GG-registration-frontend with continueUrl to start of overseas journey" in {
      val someExistingKey = "storedInSessionKey"

      implicit val request = FakeRequest().withSession(someExistingKey -> "testValue")
      val result = controller.signOutWithContinueUrl(request)

      result.futureValue.session.get(someExistingKey) shouldBe None
      val continueUrl = "http://localhost:9414/agent-services/apply-from-outside-uk"
      redirectLocation(result).get shouldBe s"http://localhost:8571/government-gateway-registration-frontend?accountType=agent&origin=unknown&continue=${URLEncoder.encode(continueUrl, "utf-8")}"
    }
  }

  "startSurvey" should {
    "redirect to feedback survey page" in {
      val result = controller.startFeedbackSurvey(basicRequest(FakeRequest()))

      status(result) shouldBe 303
      result.futureValue.header.headers(LOCATION) should include("/feedback/OVERSEAS_AGENTS")
    }
  }

  "timedOut" should {
    "display the timed out page" in {
      val result = controller.timedOut(basicRequest(FakeRequest()))

      status(result) shouldBe 403
      contentAsString(result).contains("You have been signed out")
    }
  }
}
