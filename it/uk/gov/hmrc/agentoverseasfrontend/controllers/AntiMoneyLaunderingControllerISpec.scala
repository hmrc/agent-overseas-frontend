package uk.gov.hmrc.agentoverseasfrontend.controllers

import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AntiMoneyLaunderingController
import uk.gov.hmrc.agentoverseasfrontend.models.{AgentSession, AmlsDetails}
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.routes
import scala.concurrent.ExecutionContext.Implicits.global

class AntiMoneyLaunderingControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val controller: AntiMoneyLaunderingController = app.injector.instanceOf[AntiMoneyLaunderingController]

  "GET /money-laundering-registration" should {

    "redirect to it self when agentSession not initialised, should only be done once as auth action should initialise agentSession" in {

      given404OverseasApplications()
      val result = await(controller.showMoneyLaunderingRequired(cleanCredsAgent(FakeRequest())))

      redirectLocation(result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url
      await(sessionStoreService.fetchAgentSession).isDefined shouldBe true
    }

    "display the is money laundering required page" in {

      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showMoneyLaunderingRequired(authenticatedRequest))
      status(result) shouldBe 200
      result should containSubstrings(
        "Does your country require you to register with a money laundering supervisory body?",
        "Yes",
        "No")
    }

    "back link should be check your answers when changing" in {

      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showMoneyLaunderingRequired(authenticatedRequest))
      status(result) shouldBe 200

      checkHtmlResultWithBodyText(
        result,
        "<a href=\"/agent-services/apply-from-outside-uk/check-your-answers\" class=\"link-back\"")
    }
  }

  "POST /money-laundering-registration" should {

    "redirect to /money-laundering when YES is selected" in {

      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsRequired" -> "true")

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)

      sessionStoreService.fetchAgentSession.get.amlsRequired shouldBe Some(true)
    }

    "redirect to /contact-details when NO is selected" in {

      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsRequired" -> "false")

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showContactDetailsForm().url)

      sessionStoreService.fetchAgentSession.get.amlsRequired shouldBe Some(false)
    }

    "redirect to /check-answers and remove AMLS details from session when changing is true and the user selects NO (changing from YES to NO)" in {

      await(
        sessionStoreService.cacheAgentSession(
          AgentSession(
            amlsRequired = Some(true),
            amlsDetails = Some(AmlsDetails("supervisory", Some("123"))),
            changingAnswers = true)))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsRequired" -> "false")

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showCheckYourAnswers().url)

      sessionStoreService.fetchAgentSession.get.amlsRequired shouldBe Some(false)
      sessionStoreService.fetchAgentSession.get.amlsDetails shouldBe None
    }

    "redirect to /money-landering when changing is true and the user selects YES (changing from NO to YES)" in {

      await(
        sessionStoreService.cacheAgentSession(
          AgentSession(
            amlsRequired = Some(false),
            changingAnswers = true)))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsRequired" -> "true")

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)

      sessionStoreService.fetchAgentSession.get.amlsRequired shouldBe Some(true)
    }

    "redisplay the page with errors when no radio button is selected" in {

      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 200
      result should containSubstrings(
        "Does your country require you to register with a money laundering supervisory body?",
        "Yes",
        "No",
        "Select yes if your country requires you to register with a money laundering supervisory body"
      )
    }
  }

  "GET /money-laundering" should {

    "display the is amls required page if user has not already selected an option" in {

      await(sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = None)))
      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url
    }

    "display the contact form page if user has already selected false to amls required" in {

      await(sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(false))))
      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.ApplicationController.showContactDetailsForm().url
    }

    "display the money-laundering form if user has selected amls required" in {

      given404OverseasApplications()
      await(sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(true))))
      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "amls.title",
        "amls.inset.p1",
        "amls.form.supervisory_body",
        "amls.form.membership_number",
        "amls.hint.expandable",
        "amls.hint.expandable.p1"
      )

      result should containSubstrings(routes.SignOutController.signOut().url)
    }

    "display the money-laundering form with correct back button link when user is CHANGING ANSWERS" in {

      given404OverseasApplications()
      await(sessionStoreService.cacheAgentSession(
        AgentSession(
          amlsDetails = Some(
            AmlsDetails(
              supervisoryBody = "super",
              membershipNumber = Some("123"))
          ),
          changingAnswers = true,
          amlsRequired = Some(true))
        )
      )
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showAntiMoneyLaunderingForm(authenticatedRequest))

      status(result) shouldBe 200

      result should containLink("button.back", routes.ApplicationController.showCheckYourAnswers().url)
    }

    "display the money-laundering form with correct back button link when user is CHANGING ANSWERS via the /anti-money-laundering-registration page" in {

      given404OverseasApplications()
      await(sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(true), changingAnswers = true)))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showAntiMoneyLaunderingForm(authenticatedRequest))

      status(result) shouldBe 200

      result should containLink("button.back", routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url)
    }

    "display the money-laundering form with correct back button link when user is not changing answers and Has seen previously rejected application page" in {

      await(sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(true))))
      given200GetOverseasApplications(allRejected = true)

      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containLink("button.back", routes.StartController.applicationStatus().url)
    }

  }

  "POST /money-laundering" should {

    "redirect to upload/amls" in {

      await(sessionStoreService.cacheAgentSession(AgentSession()))
      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "amlsBody"         -> "Association of AccountingTechnicians (AAT)",
          "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.FileUploadController.showAmlsUploadForm().url

      val amlsDetails = await(sessionStoreService.fetchAgentSession).get.amlsDetails

      amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))
    }

    "redirect to upload-proof-anti-money-laundering-registration if user is changing the details" in {

      //pre-state
      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))

      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "amlsBody"         -> "Association of AccountingTechnicians (AAT)",
          "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.FileUploadController.showAmlsUploadForm().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))
    }

    "show validation error when form params are incorrect with correct back link for changing answers" in {

      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))

      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.moneyLaunderingCompliance.amlsbody.blank")
      result should containLink("button.back", routes.ApplicationController.showCheckYourAnswers().url)
    }

    "show validation error when form params are incorrect with correct back link for not changing answers" in {

      await(sessionStoreService.cacheAgentSession(AgentSession()))

      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.moneyLaunderingCompliance.amlsbody.blank")
      result should containLink("button.back", routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url)

    }
  }
}
