package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class TradingAddressControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val overseasAddress = OverseasAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val personalDetails = PersonalDetailsChoice(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    overseasAddress = Some(overseasAddress),
    personalDetails = Some(personalDetails)
  )

  private lazy val controller: TradingAddressController = app.injector.instanceOf[TradingAddressController]

  "GET /main-business-address" should {
    "display the trading address form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

      val result = controller.showMainBusinessAddressForm(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "mainBusinessAddress.caption",
        "mainBusinessAddress.title"
      )
      result.futureValue should containSubstrings(routes.ApplicationController.showCheckYourAnswers().url)
    }

    "redirect to /money-laundering-registration when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showMainBusinessAddressForm(authenticatedRequest)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url)
    }
  }

  "POST /main-business-address" should {
    "submit form and then redirect to trading-address-upload page" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(overseasAddress = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "IE")

      val result = controller.submitMainBusinessAddress(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.FileUploadController.showTradingAddressUploadForm().url

      val tradingAddress = sessionStoreService.fetchAgentSession.futureValue.get.overseasAddress

      tradingAddress shouldBe Some(OverseasAddress("line1", "line2", None, None, "IE"))
    }

    "submit form and then redirect to check-your-answers page if user is changing answers" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "IE")

      val result = controller.submitMainBusinessAddress(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.overseasAddress shouldBe Some(OverseasAddress("line1", "line2", None, None, "IE"))

      //should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation errors when form data is incorrect" when {
      "address line 1 is blank" in {
        sessionStoreService.currentSession.agentSession =
          Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody("addressLine1" -> "", "addressLine2" -> "line2", "countryCode" -> "IE")

        val result = controller.submitMainBusinessAddress(authenticatedRequest)

        status(result) shouldBe 200

        result.futureValue should containMessages("error.addressline.1.empty")
      }
      "country code is GB" in {
        sessionStoreService.currentSession.agentSession =
          Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody("addressLine1" -> "Some address", "addressLine2" -> "line2", "countryCode" -> "GB")

        val result = controller.submitMainBusinessAddress(authenticatedRequest)

        status(result) shouldBe 200

        result.futureValue should containMessages("error.country.invalid")

      }
    }
  }

}
