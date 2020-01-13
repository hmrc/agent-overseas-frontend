package uk.gov.hmrc.agentoverseasfrontend.controllers
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.LOCATION
import uk.gov.hmrc.agentoverseasfrontend.controllers.application._
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class ChangingAnswersControllerISpec extends BaseISpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val agentSession = AgentSession()

  private lazy val controller: ChangingAnswersController = app.injector.instanceOf[ChangingAnswersController]

  class SetUp {
    await(sessionStoreService.cacheAgentSession(agentSession))
    val authenticatedRequest = cleanCredsAgent(FakeRequest())
  }

  "GET /change-amls-details" should {

    "update session with changingAnswers=true and redirect to money-laundering form" in new SetUp {

      val result = await(controller.changeAmlsDetails(authenticatedRequest))

      verify(result, routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)
    }
  }

  "GET /change-contact-details" should {

    "update session with changingAnswers=true and redirect to contact-details form" in new SetUp {

      val result = await(controller.changeContactDetails(authenticatedRequest))

      verify(result, routes.ApplicationController.showContactDetailsForm().url)
    }
  }

  "GET /change-trading-name" should {

    "update session with changingAnswers=true and redirect to trading-name form" in new SetUp {

      val result = await(controller.changeTradingName(authenticatedRequest))

      verify(result, routes.ApplicationController.showTradingNameForm().url)
    }
  }

  "GET /change-trading-address" should {

    "update session with changingAnswers=true and redirect to trading-address form" in new SetUp {

      val result = await(controller.changeTradingAddress(authenticatedRequest))

      verify(result, routes.TradingAddressController.showMainBusinessAddressForm().url)
    }
  }

  "GET /change-trading-address-file" should {

    "update session with changingAnswers=true and redirect to trading-address-file-upload form" in new SetUp {

      val result = await(controller.changeTradingAddressFile(authenticatedRequest))

      verify(result, routes.FileUploadController.showTradingAddressUploadForm().url)
    }
  }

  "GET /change-registered-with-hmrc" should {

    "update session with changingAnswers=true and redirect to registered-with-hmrc form" in new SetUp {

      val result = await(controller.changeRegisteredWithHmrc(authenticatedRequest))

      verify(result, routes.ApplicationController.showRegisteredWithHmrcForm().url)
    }
  }

  "GET /change-agent-codes" should {

    "update session with changingAnswers=true and redirect to agent-codes form" in new SetUp {

      val result = await(controller.changeAgentCodes(authenticatedRequest))

      verify(result, routes.ApplicationController.showAgentCodesForm().url)
    }
  }

  "GET /change-registered-with-uk-tax" should {

    "update session with changingAnswers=true and redirect to uk-tax-registration form" in new SetUp {

      val result = await(controller.changeRegisteredForUKTax(authenticatedRequest))

      verify(result, routes.ApplicationController.showUkTaxRegistrationForm().url)
    }
  }

  "GET /change-personal-details" should {

    "update session with changingAnswers=true and redirect to personal-details form" in new SetUp {

      val result = await(controller.changePersonalDetails(authenticatedRequest))

      verify(result, routes.ApplicationController.showPersonalDetailsForm().url)
    }
  }

  "GET /change-company-reg-number" should {

    "update session with changingAnswers=true and redirect to company-reg-number form" in new SetUp {
      val result = await(controller.changeCompanyRegistrationNumber(authenticatedRequest))

      verify(result, routes.ApplicationController.showCompanyRegistrationNumberForm().url)
    }
  }

  "GET /change-your-tax-reg-numbers" should {

    "update session with changingAnswers=true and redirect to your-tax-registration-numbers form" in new SetUp {
      val result = await(controller.changeYourTaxRegistrationNumbers(authenticatedRequest))

      verify(result, routes.TaxRegController.showYourTaxRegNumbersForm().url)
    }
  }

  private def verify(result: Result, url: String) = {
    status(result) shouldBe 303
    result.header.headers(LOCATION) shouldBe url
    await(sessionStoreService.fetchAgentSession).get.changingAnswers shouldBe true
  }
}
