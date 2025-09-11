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

package uk.gov.hmrc.agentoverseasfrontend.controllers.subscription

import org.jsoup.Jsoup
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.test.Helpers
import uk.gov.hmrc.agentoverseasfrontend.models.Arn
import uk.gov.hmrc.agentoverseasfrontend.models.ProviderId
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.stubs.StubsTestData._
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentSubscriptionStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class BusinessIdentificationControllerISpec
extends BaseISpec
with AgentOverseasApplicationStubs
with AgentSubscriptionStubs {

  lazy val controller: BusinessIdentificationController = app.injector.instanceOf[BusinessIdentificationController]

  "GET /check-answers" should {
    "display the check-answers page if status is Accepted" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.showCheckAnswers(request)
      status(result) shouldBe 200

      val htmlString = Helpers.contentAsString(result)
      val html = Jsoup.parse(htmlString)

      html.title().contains("Confirm your contact details before creating your account")

      result.futureValue should containMessages(
        "subscription.checkAnswers.title",
        "subscription.checkAnswers.description.p1",
        "subscription.checkAnswers.description.p2a",
        "subscription.checkAnswers.description.p2b",
        "subscription.checkAnswers.description.p2c",
        "subscription.checkAnswers.description.p3",
        "subscription.checkAnswers.businessName.label",
        "subscription.checkAnswers.businessAddress.label",
        "subscription.checkAnswers.businessEmailAddress.label",
        "subscription.checkAnswers.confirm.button",
        "checkAnswers.change.button"
      )

      result.futureValue should containSubstrings(
        agencyDetails.agencyName,
        agencyDetails.agencyEmail,
        agencyDetails.agencyAddress.addressLine1,
        agencyDetails.agencyAddress.addressLine2,
        agencyDetails.agencyAddress.addressLine3.get,
        agencyDetails.agencyAddress.addressLine4.get,
        "Belgium"
      )

      result.futureValue should containSubmitButton("subscription.checkAnswers.confirm.button", "continue")
    }

    "redirect to application root path page if no active application available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenApplicationEmptyResponse()

      val result = controller.showCheckAnswers(request)
      status(result) shouldBe 303

      header(LOCATION, result).get shouldBe "http://localhost:9414/agent-services/apply-from-outside-uk"
    }

    "redirect to /application-status if Pending" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenPendingApplicationResponse()

      val result = controller.showCheckAnswers(request)
      status(result) shouldBe 303

      header(
        LOCATION,
        result
      ).get shouldBe "http://localhost:9414/agent-services/apply-from-outside-uk/application-status"
    }

    "redirect to /application-status if Rejected" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenRejectedApplicationResponse()

      val result = controller.showCheckAnswers(request)
      status(result) shouldBe 303

      header(
        LOCATION,
        result
      ).get shouldBe "http://localhost:9414/agent-services/apply-from-outside-uk/application-status"
    }

    "attempt subscribeAndEnrol if Registered then redirect to /complete" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenRegisteredApplicationResponse()
      givenApplicationUpdateSuccessResponse()
      givenSubscriptionSuccessfulResponse(Arn("TARN0000001"))

      val result: Future[Result] = controller.showCheckAnswers(request)
      status(result) shouldBe 303

      header(LOCATION, result).get shouldBe "/agent-services/apply-from-outside-uk/create-account/complete"

    }

    "redirect to next-steps if Registered with unclean credential" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingAgentEnrolledForNonMTD)
      givenRegisteredApplicationResponse()

      val result: Future[Result] = controller.showCheckAnswers(request)
      status(result) shouldBe 303

      header(LOCATION, result).get shouldBe "/agent-services/apply-from-outside-uk/create-account/next-step"
    }

    "attempt subscribeAndEnrol if Complete then redirect to /agent-services-account" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingAgentEnrolledForHMRCASAGENT)
      givenCompleteApplicationResponse()

      val result = controller.showCheckAnswers(request)
      status(result) shouldBe 303

      header(LOCATION, result).get shouldBe "http://localhost:9401/agent-services-account"

    }
  }

  "GET /check-business-address" should {
    "display the business address page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.showCheckBusinessAddress(request)
      status(result) shouldBe 200

      result.futureValue should containMessages(
        "contactTradingAddressCheck.title",
        "contactTradingAddressCheck.p",
        "contactTradingAddressCheck.option.yes",
        "contactTradingAddressCheck.option.no",
        "button.continue",
        "button.back"
      )
    }
  }

  "POST /check-business-address" should {
    "show validation error when the form is submitted blank" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(
        subscribingCleanAgentWithoutEnrolments,
        POST
      ).withFormUrlEncodedBody("useThisAddress" -> "")
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessAddress(request)

      result.futureValue should containMessages("error.contact-trading-address-check.invalid")
    }

    "redirect to check-answers page for a Yes answer" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisAddress" -> "true"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessAddress(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

    "redirect to 'update address' page for a No answer" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisAddress" -> "false"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessAddress(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showUpdateBusinessAddressForm.url
    }

    "redirect to check-answers page for a No answer without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisAddress" -> "false"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = None

      val result = controller.submitCheckBusinessAddress(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

  }

  "GET /update-business-address" should {

    "display the business address page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.showUpdateBusinessAddressForm(request)
      status(result) shouldBe 200

      result.futureValue should containMessages(
        "updateBusinessAddress.title",
        "updateBusinessAddress.p1",
        "updateBusinessAddress.address_line_1.title",
        "updateBusinessAddress.address_line_2.title",
        "updateBusinessAddress.address_line_3.title",
        "button.continue",
        "button.back"
      )
    }

    "redirect to checkAnswers page if no session details is available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()

      val result = controller.showUpdateBusinessAddressForm(request)
      status(result) shouldBe 303

      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }
  }

  "POST /update-business-address" should {
    "redirect to check-answers page for a valid form with session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> "new addressline 1",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "addressLine4" -> "new addressline 4",
        "countryCode" -> "IE"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessAddressForm(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url

      val updatedBusinessAddress = sessionCacheService.fetchAgencyDetails.futureValue.get.agencyAddress
      updatedBusinessAddress.addressLine1 shouldBe "new addressline 1"
      updatedBusinessAddress.addressLine2 shouldBe "new addressline 2"
      updatedBusinessAddress.addressLine3 shouldBe Some("new addressline 3")
      updatedBusinessAddress.addressLine4 shouldBe Some("new addressline 4")
      updatedBusinessAddress.countryCode shouldBe "IE"
    }

    "redirect to check-answers page for a valid form without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> "new addressline 1",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "countryCode" -> "IE"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = None

      val result = controller.submitUpdateBusinessAddressForm(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

    "show validation error when the form is submitted with empty address line 1" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> " ",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "countryCode" -> "IE"
      )
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessAddressForm(request)

      result.futureValue should containMessages(
        "updateBusinessAddress.address_line_1.title",
        "error.addressline.1.empty"
      )
    }

    "show validation error when the form is submitted with invalid address line 3" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> "address line 1",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline **!",
        "countryCode" -> "IE"
      )
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessAddressForm(request)

      result.futureValue should containMessages(
        "updateBusinessAddress.address_line_3.title",
        "error.addressline.3.invalid"
      )
    }

    "show validation error when the form is submitted with invalid address line 4" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> "address line 1",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "addressLine4" -> "new addressline **!",
        "countryCode" -> "IE"
      )
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessAddressForm(request)

      result.futureValue should containMessages(
        "updateBusinessAddress.address_line_4.title",
        "error.addressline.4.invalid"
      )
    }

    "show validation error when the form is submitted with invalid address line 1" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> "address line 1**",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "countryCode" -> "IE"
      )
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessAddressForm(request)

      result.futureValue should containMessages(
        "updateBusinessAddress.address_line_1.title",
        "error.addressline.1.invalid"
      )
    }

    "show validation error when the form is submitted with empty country code" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> " ",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "countryCode" -> ""
      )
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessAddressForm(request)

      result.futureValue should containMessages("updateBusinessAddress.address_line_1.title", "error.country.empty")
    }

    "show validation error when the form is submitted with invalid country code" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "addressLine1" -> " ",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "countryCode" -> "INVALID"
      )
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessAddressForm(request)

      result.futureValue should containMessages("updateBusinessAddress.address_line_1.title", "error.country.invalid")
    }
  }

  "GET /check-business-email" should {
    "display the business email page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.showCheckBusinessEmail(request)
      status(result) shouldBe 200

      result.futureValue should containMessages(
        "contactTradingEmailCheck.title",
        "contactTradingEmailCheck.p",
        "contactTradingEmailCheck.option.yes",
        "contactTradingEmailCheck.option.no",
        "button.continue",
        "button.back"
      )
    }
  }

  "POST /check-business-email" should {
    "show validation error when the form is submitted blank" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(
        subscribingCleanAgentWithoutEnrolments,
        POST
      ).withFormUrlEncodedBody("useThisEmail" -> "")
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessEmail(request)

      result.futureValue should containMessages("error.contact-trading-email-check.invalid")
    }

    "redirect to check-answers page for a Yes answer" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisEmail" -> "true"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessEmail(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

    "redirect to 'update email' page for a No answer" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisEmail" -> "false"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessEmail(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showUpdateBusinessEmailForm.url
    }

    "redirect to check-answers page for a No answer without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisEmail" -> "false"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = None

      val result = controller.submitCheckBusinessEmail(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

  }

  "GET /update-business-email" should {
    "display the business email page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.showUpdateBusinessEmailForm(request)
      status(result) shouldBe 200

      result.futureValue should containMessages(
        "updateBusinessEmail.title",
        "updateBusinessEmail.description",
        "button.continue"
      )
    }

    "redirect to checkAnswers page if no session details are available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()

      val result = controller.showUpdateBusinessEmailForm(request)
      status(result) shouldBe 303

      header(LOCATION, result).get should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }
  }

  "POST /update-business-email" should {
    "redirect to check-answers page for a valid form with session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "email" -> "newemail@example.com"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessEmailForm(request)

      status(result) shouldBe 303
      header(LOCATION, result).get should include("/agent-services/apply-from-outside-uk/create-account/check-answers")

      sessionCacheService.fetchAgencyDetails.futureValue.get.agencyEmail shouldBe "newemail@example.com"
    }

    "redirect to check-answers page for a valid form without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "email" -> "newemail@example.com"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessEmailForm(request)

      status(result) shouldBe 303
      header(LOCATION, result).get should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }

    "show validation error when the form is submitted with empty email address" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(
        subscribingCleanAgentWithoutEnrolments,
        POST
      ).withFormUrlEncodedBody("email" -> " ")
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessEmailForm(request)

      result.futureValue should containMessages("updateBusinessEmail.title", "error.business-email.empty")
    }
  }

  "GET /check-business-name" should {
    "display the business name page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.showCheckBusinessName(request)
      status(result) shouldBe 200

      result.futureValue should containMessages(
        "contactTradingNameCheck.title",
        "contactTradingNameCheck.p",
        "contactTradingNameCheck.option.yes",
        "contactTradingNameCheck.option.no",
        "button.continue",
        "button.back"
      )
    }
  }

  "POST /check-business-name" should {
    "show validation error when the form is submitted blank" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(
        subscribingCleanAgentWithoutEnrolments,
        POST
      ).withFormUrlEncodedBody("useThisName" -> "")
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessName(request)

      result.futureValue should containMessages("error.contact-trading-name-check.invalid")
    }

    "redirect to check-answers page for a Yes answer" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisName" -> "true"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessName(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

    "redirect to 'update name' page for a No answer" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisName" -> "false"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitCheckBusinessName(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showUpdateBusinessNameForm.url
    }

    "redirect to check-answers page for a No answer without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "useThisName" -> "false"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = None

      val result = controller.submitCheckBusinessName(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

  }

  "GET /update-business-name" should {
    "display the business name page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.showUpdateBusinessNameForm(request)
      status(result) shouldBe 200

      result.futureValue should containMessages(
        "updateBusinessName.title",
        "updateBusinessName.description",
        "button.continue"
      )
    }

    "redirect to checkAnswers page if no session details are available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()

      val result = controller.showUpdateBusinessNameForm(request)
      status(result) shouldBe 303

      header(LOCATION, result).get should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }
  }

  "POST /update-business-name" should {
    "redirect to check-answers page for a valid form with session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(
        subscribingCleanAgentWithoutEnrolments,
        POST
      ).withFormUrlEncodedBody("name" -> "New name")

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessNameForm(request)

      status(result) shouldBe 303
      header(LOCATION, result).get should include("/agent-services/apply-from-outside-uk/create-account/check-answers")

      sessionCacheService.fetchAgencyDetails.futureValue.get.agencyName shouldBe "New name"
    }

    "redirect to check-answers page for a valid form without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments, POST).withFormUrlEncodedBody(
        "name" -> "New name"
      )

      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = None

      val result = controller.submitUpdateBusinessNameForm(request)

      status(result) shouldBe 303
      header(LOCATION, result).get should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }

    "show validation error when the form is submitted with empty business name" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(
        subscribingCleanAgentWithoutEnrolments,
        POST
      ).withFormUrlEncodedBody("name" -> " ")
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)

      val result = controller.submitUpdateBusinessNameForm(request)

      result.futureValue should containMessages("updateBusinessName.title", "error.business-name.empty")
    }
  }

  "GET /return-from-gg-registration" should {
    "redirect to check-answers page" when {
      "a valid session id found" in {
        val oldSessionRequest = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
        sessionCacheService.cacheProviderId(ProviderId("credId-12345"))(oldSessionRequest).futureValue

        implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribing2ndCleanAgentWithoutEnrolments)
        givenUpdateAuthIdSuccessResponse("credId-12345")

        val result = controller.returnFromGGRegistration(oldSessionRequest.session.apply(SessionKeys.sessionId))(request)

        status(result) shouldBe 303
        header(LOCATION, result).get should include(
          "/agent-services/apply-from-outside-uk/create-account/check-answers"
        )

        verifyUpdateAuthIdRequest(1)
      }

      "an invalid session id found" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
        val result = controller.returnFromGGRegistration("invalid-id")(request)

        status(result) shouldBe 303
        header(LOCATION, result).get should include(
          "/agent-services/apply-from-outside-uk/create-account/check-answers"
        )

        verifyUpdateAuthIdRequest(0)
      }
    }
  }

  "email verification" should {
    def checkVerifyEmailIsTriggered(f: Request[AnyContent] => Future[Result]) = {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = cleanCredsAgent(FakeRequest())
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails.copy(verifiedEmails = Set.empty))
      val result = f(request)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.SubscriptionEmailVerificationController.verifyEmail.url)
    }

    def checkVerifyEmailIsNotTriggered(f: Request[AnyContent] => Future[Result]) = {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = cleanCredsAgent(FakeRequest())
      givenAcceptedApplicationResponse()
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails.copy(verifiedEmails = Set.empty))
      val result = f(request)
      status(result) should (equal(200) or equal(303))
      if (status(result) == 303)
        redirectLocation(result) should not be Some(routes.SubscriptionEmailVerificationController.verifyEmail.url)
    }

    "be triggered with an unverified email" when {
      "show check your answers" in checkVerifyEmailIsTriggered { request =>
        controller.showCheckAnswers(request)
      }
      "show check business address" in checkVerifyEmailIsTriggered { request =>
        controller.showCheckBusinessAddress(request)
      }
      "submit check business address" in checkVerifyEmailIsTriggered { request =>
        controller.submitCheckBusinessAddress(request)
      }
      "show update business address" in checkVerifyEmailIsTriggered { request =>
        controller.showUpdateBusinessAddressForm(request)
      }
      "submit update business address" in checkVerifyEmailIsTriggered { request =>
        controller.submitUpdateBusinessAddressForm(request)
      }
      "show check business name" in checkVerifyEmailIsTriggered { request =>
        controller.showCheckBusinessName(request)
      }
      "submit check business name" in checkVerifyEmailIsTriggered { request =>
        controller.submitCheckBusinessName(request)
      }
      "show update business name" in checkVerifyEmailIsTriggered { request =>
        controller.showUpdateBusinessNameForm(request)
      }
      "submit update business name" in checkVerifyEmailIsTriggered { request =>
        controller.submitUpdateBusinessNameForm(request)
      }
    }
    "not be triggered when using with the email retrieved by auth" when {
      "show check your answers" in checkVerifyEmailIsNotTriggered { implicit request =>
        // we use the email (authemail@email.com) which is returned in the mock auth response
        sessionCacheService.currentSession.agencyDetails = Some(agencyDetails.copy(agencyEmail = "authemail@email.com", verifiedEmails = Set.empty))
        controller.showCheckAnswers(request)
      }
      // also all other pages but checking one should be enough as they all use the same logic
    }
    "not be triggered even with an unverified mail" when {
      // these pages must display even with an unverified email otherwise the user couldn't enter or correct their email address!
      "show check business email" in checkVerifyEmailIsNotTriggered { request =>
        controller.showCheckBusinessEmail(request)
      }
      "submit check business email" in checkVerifyEmailIsNotTriggered { request =>
        controller.submitCheckBusinessEmail(request)
      }
      "show update business email" in checkVerifyEmailIsNotTriggered { request =>
        controller.showUpdateBusinessEmailForm(request)
      }
      "submit update business email" in checkVerifyEmailIsNotTriggered { request =>
        controller.submitUpdateBusinessEmailForm(request)
      }
    }
  }

}
