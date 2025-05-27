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

package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.models.AmlsDetails
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AntiMoneyLaunderingControllerISpec
extends BaseISpec
with AgentOverseasApplicationStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val controller: AntiMoneyLaunderingController = app.injector.instanceOf[AntiMoneyLaunderingController]

  "GET /money-laundering-registration" should {

    "redirect to it self when agentSession not initialised, should only be done once as auth action should initialise agentSession" in {

      given404OverseasApplications()
      val result = controller.showMoneyLaunderingRequired(cleanCredsAgent(FakeRequest()))

      redirectLocation(result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
      sessionStoreService.fetchAgentSession.futureValue.isDefined shouldBe true
    }

    "display the is money laundering required page" in {

      sessionStoreService.cacheAgentSession(AgentSession()).futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showMoneyLaunderingRequired(authenticatedRequest)
      status(result) shouldBe 200
      result.futureValue should containSubstrings(
        "Does your country require you to register with a money laundering supervisory body?",
        "Yes",
        "No"
      )
    }

    "back link should be check your answers when changing" in {

      sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showMoneyLaunderingRequired(authenticatedRequest)
      status(result) shouldBe 200

      checkHtmlResultWithBodyText(
        result.futureValue,
        "<a href=\"/agent-services/apply-from-outside-uk/check-your-answers\""
      )
    }
  }

  "POST /money-laundering-registration" should {

    "redirect to /money-laundering when YES is selected" in {

      sessionStoreService.cacheAgentSession(AgentSession()).futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("amlsRequired" -> "true")

      val result = controller.submitMoneyLaunderingRequired(authenticatedRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm.url)

      sessionStoreService.fetchAgentSession.futureValue.get.amlsRequired shouldBe Some(true)
    }

    "redirect to /contact-details when NO is selected" in {

      sessionStoreService.cacheAgentSession(AgentSession()).futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("amlsRequired" -> "false")

      val result = controller.submitMoneyLaunderingRequired(authenticatedRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showContactDetailsForm.url)

      sessionStoreService.fetchAgentSession.futureValue.get.amlsRequired shouldBe Some(false)
    }

    "redirect to /check-answers and remove AMLS details from session when changing is true and the user selects NO (changing from YES to NO)" in {

      sessionStoreService
        .cacheAgentSession(
          AgentSession(
            amlsRequired = Some(true),
            amlsDetails = Some(AmlsDetails("supervisory", Some("123"))),
            changingAnswers = true
          )
        )
        .futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("amlsRequired" -> "false")

      val result = controller.submitMoneyLaunderingRequired(authenticatedRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showCheckYourAnswers.url)

      sessionStoreService.fetchAgentSession.futureValue.get.amlsRequired shouldBe Some(false)
      sessionStoreService.fetchAgentSession.futureValue.get.amlsDetails shouldBe None
    }

    "redirect to /money-landering when changing is true and the user selects YES (changing from NO to YES)" in {

      sessionStoreService
        .cacheAgentSession(AgentSession(amlsRequired = Some(false), changingAnswers = true))
        .futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("amlsRequired" -> "true")

      val result = controller.submitMoneyLaunderingRequired(authenticatedRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm.url)

      sessionStoreService.fetchAgentSession.futureValue.get.amlsRequired shouldBe Some(true)
    }

    "redisplay the page with errors when no radio button is selected" in {

      sessionStoreService.cacheAgentSession(AgentSession()).futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))

      val result = controller.submitMoneyLaunderingRequired(authenticatedRequest)

      status(result) shouldBe 200
      result.futureValue should containSubstrings(
        "Does your country require you to register with a money laundering supervisory body?",
        "Yes",
        "No",
        "Select yes if your country requires you to register with a money laundering supervisory body"
      )
    }
  }

  "GET /money-laundering" should {

    "display the is amls required page if user has not already selected an option" in {

      sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = None)).futureValue
      val result = controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 303
      header("Location", result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
    }

    "display the contact form page if user has already selected false to amls required" in {

      sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(false))).futureValue
      val result = controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 303
      header("Location", result).get shouldBe routes.ApplicationController.showContactDetailsForm.url
    }

    "display the money-laundering form if user has selected amls required" in {

      given404OverseasApplications()
      sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(true))).futureValue
      val result = controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "amls.title",
        "amls.inset.p1",
        "amls.form.supervisory_body",
        "amls.form.membership_number",
        "amls.hint.expandable",
        "amls.hint.expandable.p1"
      )

      result.futureValue should containSubstrings(routes.ApplicationSignOutController.signOut.url)
    }

    "display the money-laundering form with correct back button link when user is CHANGING ANSWERS" in {

      given404OverseasApplications()
      sessionStoreService
        .cacheAgentSession(
          AgentSession(
            amlsDetails = Some(
              AmlsDetails(supervisoryBody = "super", membershipNumber = Some("123"))
            ),
            changingAnswers = true,
            amlsRequired = Some(true)
          )
        )
        .futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showAntiMoneyLaunderingForm(authenticatedRequest)

      status(result) shouldBe 200

      result.futureValue should containLink("button.back", routes.ApplicationController.showCheckYourAnswers.url)
    }

    "display the money-laundering form with correct back button link when user is CHANGING ANSWERS via the /anti-money-laundering-registration page" in {

      given404OverseasApplications()
      sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(true), changingAnswers = true)).futureValue
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showAntiMoneyLaunderingForm(authenticatedRequest)

      status(result) shouldBe 200

      result.futureValue should containLink(
        "button.back",
        routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
      )
    }

    "display the money-laundering form with correct back button link when user is not changing answers and Has seen previously rejected application page" in {

      sessionStoreService.cacheAgentSession(AgentSession(amlsRequired = Some(true))).futureValue
      given200GetOverseasApplications(allRejected = true)

      val result = controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 200

      result.futureValue should containLink("button.back", routes.ApplicationRootController.applicationStatus.url)
    }

  }

  "POST /money-laundering" should {

    "redirect to upload/amls" in {

      sessionStoreService.cacheAgentSession(AgentSession()).futureValue
      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody(
          "amlsBody" -> "Association of AccountingTechnicians (AAT)",
          "membershipNumber" -> "123445"
        )

      val result = controller.submitAntiMoneyLaundering(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.FileUploadController.showAmlsUploadForm.url

      val amlsDetails = sessionStoreService.fetchAgentSession.futureValue.get.amlsDetails

      amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))
    }

    "redirect to upload-proof-anti-money-laundering-registration if user is changing the details" in {

      // pre-state
      sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)).futureValue

      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody(
          "amlsBody" -> "Association of AccountingTechnicians (AAT)",
          "membershipNumber" -> "123445"
        )

      val result = controller.submitAntiMoneyLaundering(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.FileUploadController.showAmlsUploadForm.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))
    }

    "show validation error when form params are incorrect with correct back link for changing answers" in {

      sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)).futureValue

      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("amlsBody" -> "", "membershipNumber" -> "123445")

      val result = controller.submitAntiMoneyLaundering(authenticatedRequest)

      status(result) shouldBe 200

      result.futureValue should containMessages("error.moneyLaunderingCompliance.amlsbody.blank")
      result.futureValue should containLink("button.back", routes.ApplicationController.showCheckYourAnswers.url)
    }

    "show validation error when form params are incorrect with correct back link for not changing answers" in {

      sessionStoreService.cacheAgentSession(AgentSession()).futureValue

      implicit val authenticatedRequest: FakeRequest[AnyContentAsFormUrlEncoded] = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("amlsBody" -> "", "membershipNumber" -> "123445")

      val result = controller.submitAntiMoneyLaundering(authenticatedRequest)

      status(result) shouldBe 200

      result.futureValue should containMessages("error.moneyLaunderingCompliance.amlsbody.blank")
      result.futureValue should containLink(
        "button.back",
        routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
      )

    }
  }

  "email verification" should {
    "not be triggered even with an unverified mail" when {
      def checkVerifyEmailIsNotTriggered(f: () => Future[Result]) = {
        sessionStoreService
          .cacheAgentSession(AgentSession().copy(changingAnswers = true, verifiedEmails = Set.empty))
          .futureValue
        val result = f()
        status(result) should (equal(200) or equal(303))
        if (status(result) == 303)
          redirectLocation(result) should not be routes.ApplicationEmailVerificationController.verifyEmail.url
      }
      // email verification must not trigger for the AMLS pages as these come before the email input step.
      "show amls required form" in checkVerifyEmailIsNotTriggered(() =>
        controller.showMoneyLaunderingRequired(cleanCredsAgent(FakeRequest()))
      )
      "submit amls required form" in checkVerifyEmailIsNotTriggered(() =>
        controller.submitMoneyLaunderingRequired(cleanCredsAgent(FakeRequest()))
      )
      "show amls form" in checkVerifyEmailIsNotTriggered(() =>
        controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest()))
      )
      "submit amls" in checkVerifyEmailIsNotTriggered(() =>
        controller.submitAntiMoneyLaundering(cleanCredsAgent(FakeRequest()))
      )
    }
  }

}
