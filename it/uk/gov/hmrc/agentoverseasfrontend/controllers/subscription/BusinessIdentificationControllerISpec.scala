package uk.gov.hmrc.agentoverseasfrontend.controllers.subscription

import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.LOCATION
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.stubs.StubsTestData._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessIdentificationControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  lazy val controller: BusinessIdentificationController = app.injector.instanceOf[BusinessIdentificationController]

  "GET /check-answers" should {
    "display the check-answers page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result: Result = await(controller.showCheckAnswers(request))
      status(result) shouldBe 200

      result should containMessages(
        "subscription.checkAnswers.title",
        "subscription.checkAnswers.description.p1",
        "subscription.checkAnswers.description.p2",
        "subscription.checkAnswers.businessName.label",
        "subscription.checkAnswers.businessAddress.label",
        "subscription.checkAnswers.businessEmailAddress.label",
        "subscription.checkAnswers.confirm.button",
        "checkAnswers.change.button")


      result should containSubstrings(agencyDetails.agencyName, agencyDetails.agencyEmail,
        agencyDetails.agencyAddress.addressLine1,
        agencyDetails.agencyAddress.addressLine2,
        agencyDetails.agencyAddress.addressLine3.get,
        agencyDetails.agencyAddress.addressLine4.get,
        "Belgium"
      )

      result should containSubmitButton("subscription.checkAnswers.confirm.button", "continue")
    }

    "redirect to application root path page if no active application available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenApplicationEmptyResponse()

      val result = await(controller.showCheckAnswers(request))
      status(result) shouldBe 303

      result.header.headers(LOCATION) shouldBe "http://localhost:9414/agent-services/apply-from-outside-uk"
    }
  }

  "GET /update-business-address" should {

    "display the business address page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.showUpdateBusinessAddressForm(request))
      status(result) shouldBe 200

      result should containMessages(
        "updateBusinessAddress.title",
        "updateBusinessAddress.p1",
        "updateBusinessAddress.p2",
        "updateBusinessAddress.address_line_1.title",
        "updateBusinessAddress.address_line_2.title",
        "updateBusinessAddress.address_line_3.title",
        "updateBusinessAddress.continue",
        "button.back"
      )
    }

    "redirect to checkAnswers page if no session details is available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()

      val result = await(controller.showUpdateBusinessAddressForm(request))
      status(result) shouldBe 303

      result.header.headers(LOCATION) shouldBe routes.BusinessIdentificationController.showCheckAnswers().url
    }
  }

  "POST /update-business-address" should {
    "redirect to check-answers page for a valid form with session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
        "addressLine1" -> "new addressline 1",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "addressLine4" -> "new addressline 4",
        "countryCode"     -> "IE"
      )

      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.BusinessIdentificationController.showCheckAnswers().url

      val updatedBusinessAddress = await(sessionStoreService.fetchAgencyDetails).get.agencyAddress
      updatedBusinessAddress.addressLine1 shouldBe "new addressline 1"
      updatedBusinessAddress.addressLine2 shouldBe "new addressline 2"
      updatedBusinessAddress.addressLine3 shouldBe Some("new addressline 3")
      updatedBusinessAddress.addressLine4 shouldBe Some("new addressline 4")
      updatedBusinessAddress.countryCode shouldBe "IE"
    }

    "redirect to check-answers page for a valid form without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
        "addressLine1" -> "new addressline 1",
        "addressLine2" -> "new addressline 2",
        "addressLine3" -> "new addressline 3",
        "countryCode"     -> "IE"
      )

      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = None

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.BusinessIdentificationController.showCheckAnswers().url
    }

    "show validation error when the form is submitted with empty address line 1" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "addressLine1" -> " ",
          "addressLine2" -> "new addressline 2",
          "addressLine3" -> "new addressline 3",
          "countryCode"     -> "IE")
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      result should containMessages("updateBusinessAddress.address_line_1.title", "error.addressline.1.empty")
    }

    "show validation error when the form is submitted with invalid address line 3" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "addressLine1" -> "address line 1",
          "addressLine2" -> "new addressline 2",
          "addressLine3" -> "new addressline **!",
          "countryCode"     -> "IE"
        )
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      result should containMessages("updateBusinessAddress.address_line_3.title", "error.addressline.3.invalid")
    }

    "show validation error when the form is submitted with invalid address line 4" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "addressLine1" -> "address line 1",
          "addressLine2" -> "new addressline 2",
          "addressLine3" -> "new addressline 3",
          "addressLine4" -> "new addressline **!",
          "countryCode"     -> "IE"
        )
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      result should containMessages("updateBusinessAddress.address_line_4.title", "error.addressline.4.invalid")
    }

    "show validation error when the form is submitted with invalid address line 1" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "addressLine1" -> "address line 1**",
          "addressLine2" -> "new addressline 2",
          "addressLine3" -> "new addressline 3",
          "countryCode"     -> "IE"
        )
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      result should containMessages("updateBusinessAddress.address_line_1.title", "error.addressline.1.invalid")
    }

    "show validation error when the form is submitted with empty country code" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "addressLine1" -> " ",
          "addressLine2" -> "new addressline 2",
          "addressLine3" -> "new addressline 3",
          "countryCode"     -> "")
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      result should containMessages("updateBusinessAddress.address_line_1.title", "error.country.empty")
    }

    "show validation error when the form is submitted with invalid country code" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "addressLine1" -> " ",
          "addressLine2" -> "new addressline 2",
          "addressLine3" -> "new addressline 3",
          "countryCode"     -> "INVALID")
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessAddressForm(request))

      result should containMessages("updateBusinessAddress.address_line_1.title", "error.country.invalid")
    }
  }

  "GET /update-business-email" should {
    "display the business email page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.showUpdateBusinessEmailForm(request))
      status(result) shouldBe 200

      result should containMessages(
        "updateBusinessEmail.title",
        "updateBusinessEmail.description",
        "updateBusinessEmail.continue"
      )
    }

    "redirect to checkAnswers page if no session details are available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()

      val result = await(controller.showUpdateBusinessEmailForm(request))
      status(result) shouldBe 303

      result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }
  }

  "POST /update-business-email" should {
    "redirect to check-answers page for a valid form with session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
        "email" -> "newemail@example.com")

      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessEmailForm(request))

      status(result) shouldBe 303
      result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")

      await(sessionStoreService.fetchAgencyDetails).get.agencyEmail shouldBe "newemail@example.com"
    }

    "redirect to check-answers page for a valid form without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
        "email" -> "newemail@example.com"
      )

      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessEmailForm(request))

      status(result) shouldBe 303
      result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }

    "show validation error when the form is submitted with empty email address" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "email" -> " ")
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessEmailForm(request))

      result should containMessages("updateBusinessEmail.title", "error.business-email.empty")
    }
  }

  "GET /update-business-name" should {
    "display the business name page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.showUpdateBusinessNameForm(request))
      status(result) shouldBe 200

      result should containMessages(
        "updateBusinessName.title",
        "updateBusinessName.description",
        "updateBusinessName.continue"
      )
    }

    "redirect to checkAnswers page if no session details are available" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      givenAcceptedApplicationResponse()

      val result = await(controller.showUpdateBusinessNameForm(request))
      status(result) shouldBe 303

      result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }
  }

  "POST /update-business-name" should {
    "redirect to check-answers page for a valid form with session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
        "name" -> "New name")

      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessNameForm(request))

      status(result) shouldBe 303
      result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")

      await(sessionStoreService.fetchAgencyDetails).get.agencyName shouldBe "New name"
    }

    "redirect to check-answers page for a valid form without session data" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
        "name" -> "New name"
      )

      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = None

      val result = await(controller.submitUpdateBusinessNameForm(request))

      status(result) shouldBe 303
      result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")
    }

    "show validation error when the form is submitted with empty business name" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        authenticatedAs(subscribingCleanAgentWithoutEnrolments).withFormUrlEncodedBody(
          "name" -> " ")
      givenAcceptedApplicationResponse()
      sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)

      val result = await(controller.submitUpdateBusinessNameForm(request))

      result should containMessages("updateBusinessName.title", "error.business-name.empty")
    }
  }

  "GET /return-from-gg-registration" should {
    "redirect to check-answers page" when {
      "a valid session id found" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)

        val sessionId = await(sessionDetailsRepo.create("credId-12345"))

        givenUpdateAuthIdSuccessResponse("credId-12345")

        val result = await(controller.returnFromGGRegistration(sessionId)(request))

        status(result) shouldBe 303
        result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")

        verifyUpdateAuthIdRequest(1)
      }

      "an invalid session id found" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
        val result = await(controller.returnFromGGRegistration("invalid-id")(request))

        status(result) shouldBe 303
        result.header.headers(LOCATION) should include("/agent-services/apply-from-outside-uk/create-account/check-answers")

        verifyUpdateAuthIdRequest(0)
      }
    }
  }
}