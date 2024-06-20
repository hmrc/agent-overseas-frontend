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

import org.jsoup.Jsoup
import org.scalatest.Assertion
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption.SaUtrChoice
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.support.{BaseISpec, Css}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val overseasAddress = OverseasAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetailsChoice(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  val failureDetails: FailureDetails = FailureDetails("QUARANTINE", "a virus was found!")
  val fileUploadStatus: FileUploadStatus =
    FileUploadStatus("reference", "READY", Some("filename"), Some(failureDetails))

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    overseasAddress = Some(overseasAddress),
    personalDetails = Some(personalDetails),
    verifiedEmails = Set(contactDetails.businessEmail)
  )

  private lazy val controller: ApplicationController = app.injector.instanceOf[ApplicationController]

  "GET /contact-details" should {
    "display the contact details form" in {

      sessionStoreService
        .cacheAgentSession(
          AgentSession(amlsRequired = Some(true), Some(AmlsDetails("body", Some("123"))), changingAnswers = true)
        )
        .futureValue

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showContactDetailsForm(authenticatedRequest)

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "contactDetails.title",
        "contactDetails.form.firstName",
        "contactDetails.form.lastName",
        "contactDetails.form.jobTitle",
        "contactDetails.form.businessTelephone",
        "contactDetails.form.businessEmail"
      )
      result.futureValue should containSubstrings(routes.ApplicationController.showCheckYourAnswers.url)
    }

    "redirect to /money-laundering-registration when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showContactDetailsForm(authenticatedRequest)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url)
    }
  }

  "POST /contact-details" should {
    "submit form and then redirect to trading-name" in {

      sessionStoreService
        .cacheAgentSession(AgentSession(amlsRequired = Some(true), Some(AmlsDetails("body", Some("123"))), None))
        .futureValue

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody(
          "firstName"         -> "test",
          "lastName"          -> "last",
          "jobTitle"          -> "senior agent",
          "businessTelephone" -> "12345",
          "businessEmail"     -> "test@email.com"
        )

      val result = controller.submitContactDetails(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showTradingNameForm.url

      val mayBeContactDetails = sessionStoreService.fetchAgentSession.futureValue.get.contactDetails

      mayBeContactDetails shouldBe Some(ContactDetails("test", "last", "senior agent", "12345", "test@email.com"))
    }

    "submit form and then redirect to check-your-answers if user is changing answers" in {
      // pre state

      sessionStoreService
        .cacheAgentSession(
          AgentSession(amlsRequired = Some(true), Some(AmlsDetails("body", Some("123"))), changingAnswers = true)
        )
        .futureValue

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody(
          "firstName"         -> "test",
          "lastName"          -> "last",
          "jobTitle"          -> "senior agent",
          "businessTelephone" -> "12345",
          "businessEmail"     -> "test@email.com"
        )

      val result = controller.submitContactDetails(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showCheckYourAnswers.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.contactDetails shouldBe Some(ContactDetails("test", "last", "senior agent", "12345", "test@email.com"))

      // should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation errors when form data is incorrect" in {
      // pre state

      sessionStoreService
        .cacheAgentSession(
          AgentSession(amlsRequired = Some(true), Some(AmlsDetails("body", Some("123"))), changingAnswers = true)
        )
        .futureValue

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody(
          "firstName"         -> "",
          "lastName"          -> "last**",
          "jobTitle"          -> "senior agent",
          "businessTelephone" -> "12345",
          "businessEmail"     -> "test"
        )

      val result = controller.submitContactDetails(authenticatedRequest)

      status(result) shouldBe 200

      result.futureValue should containMessages("error.firstName.blank", "error.lastName.invalid", "error.email")
    }
  }

  "GET /trading-name" should {
    "display the trading name form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, changingAnswers = true))

      val result = controller.showTradingNameForm(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "tradingName.title",
        "tradingName.p1"
      )

      result.futureValue should containSubstrings(routes.ApplicationController.showCheckYourAnswers.url)
    }

    "redirect to /money-laundering-registration when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = controller.showTradingNameForm(authenticatedRequest)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url)
    }

    "pre-fill trading name if previously has used the endpoint POST /trading-name" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingName = Some("tradingName")))

      val result = controller.showTradingNameForm(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "tradingName.title",
        "tradingName.p1"
      )

      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("tradingName").attr("value") shouldBe "tradingName"
    }
  }

  "POST /trading-name" should {
    "submit form and then redirect to main-business-details" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, overseasAddress = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("tradingName" -> "test")

      val result = controller.submitTradingName(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.TradingAddressController.showMainBusinessAddressForm.url

      val tradingName = sessionStoreService.fetchAgentSession.futureValue.get.tradingName

      tradingName shouldBe Some("test")
    }

    "submit form and then redirect to check-your-details if user is changing answers" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, overseasAddress = None, changingAnswers = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("tradingName" -> "test")

      val result = controller.submitTradingName(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showCheckYourAnswers.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.tradingName shouldBe Some("test")

      // should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation errors when form data is incorrect" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, overseasAddress = None, changingAnswers = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("tradingName" -> "")

      val result = controller.submitTradingName(authenticatedRequest)

      status(result) shouldBe 200

      result.futureValue should containMessages("error.tradingName.blank")
    }
  }

  "GET /registered-with-hmrc" should {
    class RegisteredWithHmrcSetup(agentSession: AgentSession = agentSession.copy(registeredWithHmrc = None)) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showRegisteredWithHmrcForm(authenticatedRequest)
      val doc = Jsoup.parse(contentAsString(result))
    }

    "contain page titles and header content" in new RegisteredWithHmrcSetup {
      result.futureValue should containMessages(
        "registeredWithHmrc.title",
        "registeredWithHmrc.caption",
        "registeredWithHmrc.form.title"
      )
    }

    "ask for whether they are registered with HMRC" in new RegisteredWithHmrcSetup {

      val elRadio = doc.getElementById(s"registeredWithHmrc")
      elRadio should not be null
      elRadio.tagName() shouldBe "input"
      elRadio.attr("type") shouldBe "radio"
      elRadio.attr("value") shouldBe "true"

      checkMessageIsDefined("registeredWithHmrc.form.registered.yes")
      val elLabel = doc.select(s"label[for=registeredWithHmrc]").first()
      elLabel should not be null
      elLabel.text() shouldBe htmlEscapedMessage("registeredWithHmrc.form.registered.yes")

      val elRadio2 = doc.getElementById(s"registeredWithHmrc-2")
      elRadio2 should not be null
      elRadio2.tagName() shouldBe "input"
      elRadio2.attr("type") shouldBe "radio"
      elRadio2.attr("value") shouldBe "false"

      checkMessageIsDefined("registeredWithHmrc.form.registered.no")
      val elLabel2 = doc.select(s"label[for=registeredWithHmrc-2]").first()
      elLabel2 should not be null
      elLabel2.text() shouldBe htmlEscapedMessage("registeredWithHmrc.form.registered.no")

    }

    "show existing selection if session already contains choice" in
      new RegisteredWithHmrcSetup(agentSession.copy(registeredWithHmrc = Some(Yes))) {
        doc.select("#registeredWithHmrc[checked]") should have length 1
      }

    "contain a continue button" in new RegisteredWithHmrcSetup {
      result.futureValue should containSubmitButton(
        expectedMessageKey = "button.continue",
        expectedElementId = "continue"
      )
    }

    "contain a back link to /file-uploaded-successfully/trading-address" in new RegisteredWithHmrcSetup {
      result.futureValue should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/file-uploaded-successfully"
      )
    }

    "contain a back link to /check-your-answers if user is changing answers" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(changingAnswers = true, registeredWithHmrc = None))

      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showRegisteredWithHmrcForm(authenticatedRequest)

      result.futureValue should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers"
      )
    }

    "contain a form that would POST to /registered-with-hmrc" in new RegisteredWithHmrcSetup {
      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/registered-with-hmrc"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering-registration when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showRegisteredWithHmrcForm(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
    }
  }

  "POST /registered-with-hmrc" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(registeredWithHmrc = None, changingAnswers = true))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("registeredWithHmrc" -> "true")

      val result = controller.submitRegisteredWithHmrc(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showAgentCodesForm.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.registeredWithHmrc shouldBe Some(Yes)

      session.changingAnswers shouldBe false
    }

    "show /check-your-answers page when the user in the amending state but clicks Continue without making any changes to the state" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(registeredWithHmrc = Some(Yes), changingAnswers = true))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("registeredWithHmrc" -> "true")

      val result = controller.submitRegisteredWithHmrc(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showCheckYourAnswers.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.registeredWithHmrc shouldBe Some(Yes)

      session.changingAnswers shouldBe false
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(registeredWithHmrc = None))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))

      controller.submitRegisteredWithHmrc(authenticatedRequest).futureValue should containMessages(
        "error.registeredWithHmrc.no-radio.selected"
      )

      sessionStoreService.fetchAgentSession.futureValue.get.registeredWithHmrc shouldBe None
    }
  }

  "GET /uk-tax-registration" should {
    val defaultAgentSession = agentSession.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = None
    )
    class UkTaxRegistrationSetup(agentSession: AgentSession = defaultAgentSession) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showUkTaxRegistrationForm(authenticatedRequest)
      val doc = Jsoup.parse(contentAsString(result))
    }

    "contain page titles and header content" in new UkTaxRegistrationSetup {
      result.futureValue should containMessages(
        "ukTaxRegistration.title",
        "ukTaxRegistration.caption",
        "ukTaxRegistration.form.title"
      )
    }

    "ask for whether they are registered for UK tax" in new UkTaxRegistrationSetup {

      val elRadio = doc.getElementById(s"registeredForUkTax")
      elRadio should not be null
      elRadio.tagName() shouldBe "input"
      elRadio.attr("type") shouldBe "radio"
      elRadio.attr("value") shouldBe "true"

      checkMessageIsDefined("ukTaxRegistration.form.registered.yes")
      val elLabel = doc.select(s"label[for=registeredForUkTax]").first()
      elLabel should not be null
      elLabel.text() shouldBe htmlEscapedMessage("ukTaxRegistration.form.registered.yes")

      val elRadio2 = doc.getElementById(s"registeredForUkTax-2")
      elRadio2 should not be null
      elRadio2.tagName() shouldBe "input"
      elRadio2.attr("type") shouldBe "radio"
      elRadio2.attr("value") shouldBe "false"

      checkMessageIsDefined("ukTaxRegistration.form.registered.no")
      val elLabel2 = doc.select(s"label[for=registeredForUkTax-2]").first()
      elLabel2 should not be null
      elLabel2.text() shouldBe htmlEscapedMessage("ukTaxRegistration.form.registered.no")

    }

    "show existing selection if session already contains choice" in
      new UkTaxRegistrationSetup(defaultAgentSession.copy(registeredForUkTax = Some(Yes))) {
        doc.select("#registeredForUkTax[checked]") should have length 1
      }

    "contain a continue button" in new UkTaxRegistrationSetup {
      result.futureValue should containSubmitButton(
        expectedMessageKey = "button.continue",
        expectedElementId = "continue"
      )
    }

    "contain a back link to previous page" when {
      "previous page is /self-assessment-agent-code if they stated they are registered with HMRC" in
        new UkTaxRegistrationSetup(
          defaultAgentSession.copy(
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None, None))
          )
        ) {
          result.futureValue should containLink(
            expectedMessageKey = "button.back",
            expectedHref = "/agent-services/apply-from-outside-uk/self-assessment-agent-code"
          )
        }

      "previous page is /registered-with-hmrc if they stated they are not registered with HMRC" in
        new UkTaxRegistrationSetup(
          defaultAgentSession.copy(
            registeredWithHmrc = Some(No),
            agentCodes = None
          )
        ) {
          result.futureValue should containLink(
            expectedMessageKey = "button.back",
            expectedHref = "/agent-services/apply-from-outside-uk/registered-with-hmrc"
          )
        }

      "previous page is /check-your-answers if user is changing answers" in
        new UkTaxRegistrationSetup(
          defaultAgentSession.copy(
            registeredWithHmrc = Some(No),
            agentCodes = None,
            changingAnswers = true
          )
        ) {
          result.futureValue should containLink(
            expectedMessageKey = "button.back",
            expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers"
          )
        }
    }

    "contain a form that would POST to /uk-tax-registration" in new UkTaxRegistrationSetup {
      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/uk-tax-registration"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering-registration when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showUkTaxRegistrationForm(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
    }
  }

  "POST /uk-tax-registration" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = None,
          personalDetails = None,
          changingAnswers = true
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("registeredForUkTax" -> "true")

      val result = controller.submitUkTaxRegistration(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showPersonalDetailsForm.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get
      session.registeredForUkTax shouldBe Some(Yes)
      session.changingAnswers shouldBe false
    }

    "show /check-your-answers page when the user in the amending state but clicks Continue without making any changes to the state" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No),
          personalDetails = None,
          changingAnswers = true
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("registeredForUkTax" -> "false")

      val result = controller.submitUkTaxRegistration(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showCheckYourAnswers.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get
      session.registeredForUkTax shouldBe Some(No)
      session.changingAnswers shouldBe false
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = None
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))

      controller.submitUkTaxRegistration(authenticatedRequest).futureValue should containMessages(
        "error.registeredForUkTaxForm.no-radio.selected"
      )

      sessionStoreService.fetchAgentSession.futureValue.get.registeredForUkTax shouldBe None
    }
  }

  "GET /personal-details" should {
    val defaultAgentSession = agentSession.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(Yes)
    )
    class PersonalDetailsSetup(agentSession: AgentSession = defaultAgentSession) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showPersonalDetailsForm(authenticatedRequest)
      val doc = Jsoup.parse(contentAsString(result))
    }

    "contain page titles and header content" in new PersonalDetailsSetup {
      result.futureValue should containMessages(
        "personalDetails.title",
        "personalDetails.p1",
        "personalDetails.p2",
        "personalDetails.form.nino",
        "personalDetails.form.input.label.nino",
        "personalDetails.form.helper.nino",
        "personalDetails.form.sautr",
        "personalDetails.form.input.label.sautr",
        "personalDetails.form.helper.sautr"
      )
    }

    "contain a back link to previous page - /uk-tax-registration" in new PersonalDetailsSetup {
      result.futureValue should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/uk-tax-registration"
      )
    }

    "contain a back link to check-your-answers if user is changing answers" in new PersonalDetailsSetup(
      defaultAgentSession.copy(changingAnswers = true)
    ) {
      result.futureValue should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers"
      )
    }

    "contain a form that would POST to /personal-details" in new PersonalDetailsSetup {
      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/personal-details"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering-registration when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showUkTaxRegistrationForm(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
    }
  }

  "POST /personal-details" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("personalDetailsChoice" -> "nino", "nino" -> "AB123456A", "saUtr" -> "")

      val result = controller.submitPersonalDetails(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm.url

      val savedPersonalDetails = sessionStoreService.fetchAgentSession.futureValue.get.personalDetails.get
      savedPersonalDetails shouldBe PersonalDetailsChoice(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
    }

    "store choice in session after successful submission and redirect check-your-answers if user is changing answers" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None,
          changingAnswers = true
        )
      )

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("personalDetailsChoice" -> "nino", "nino" -> "AB123456A", "saUtr" -> "")

      val result = controller.submitPersonalDetails(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showCheckYourAnswers.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get
      session.personalDetails.get shouldBe PersonalDetailsChoice(
        Some(RadioOption.NinoChoice),
        Some(Nino("AB123456A")),
        None
      )
      session.changingAnswers shouldBe false
    }

    "show validation error if no options are selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("personalDetailsChoice" -> "", "nino" -> "", "saUtr" -> "")

      controller.submitPersonalDetails(authenticatedRequest).futureValue should containMessages(
        "error.personalDetails.no-radio.selected"
      )
      sessionStoreService.fetchAgentSession.futureValue.get.personalDetails shouldBe None
    }

    "show validation error if National Insurance number option is selected, but no value has been entered" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("personalDetailsChoice" -> "nino", "nino" -> "", "saUtr" -> "")

      controller.submitPersonalDetails(authenticatedRequest).futureValue should containMessages("error.nino.blank")
      sessionStoreService.fetchAgentSession.futureValue.get.personalDetails shouldBe None
    }

    "show validation error if SA UTR option is selected, but no value has been entered" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("personalDetailsChoice" -> "saUtr", "nino" -> "", "saUtr" -> "")

      controller.submitPersonalDetails(authenticatedRequest).futureValue should containMessages("error.sautr.blank")
      sessionStoreService.fetchAgentSession.futureValue.get.personalDetails shouldBe None
    }
  }

  "GET /company-registration-number" should {
    "display the company-registration-number form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(registeredForUkTax = Some(Yes), companyRegistrationNumber = None))

      val result = controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest()))
      val backButtonUrl = routes.ApplicationController.showPersonalDetailsForm.url

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result.futureValue should containSubstrings(backButtonUrl)
    }

    "display the company-registration-number form with check-your-answers back link if user is changing answers" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(registeredForUkTax = Some(Yes), companyRegistrationNumber = None, changingAnswers = true)
      )

      val result = controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest()))
      val backButtonUrl = routes.ApplicationController.showCheckYourAnswers.url

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result.futureValue should containSubstrings(backButtonUrl)
    }

    "display the company-registration-number form with correct back button link in case user selects No option in the /uk-tax-registration page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredForUkTax = Some(No),
          companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None))
        )
      )

      val result = controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest()))
      val backButtonUrl = routes.ApplicationController.showUkTaxRegistrationForm.url

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result.futureValue should containSubstrings(backButtonUrl)

      val doc = Jsoup.parse(contentAsString(result))

      doc.select("#confirmRegistration-2[checked]") should have length 1
    }
  }

  "POST /company-registration-number" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          companyRegistrationNumber = None,
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          agentCodes = None,
          personalDetails = Some(personalDetails)
        )
      )

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "AB123456")

      val result = controller.submitCompanyRegistrationNumber(authenticatedRequest)
      status(result) shouldBe 303

      header(LOCATION, result).get shouldBe routes.TaxRegController.showTaxRegistrationNumberForm.url
      sessionStoreService.fetchAgentSession.futureValue.get.companyRegistrationNumber shouldBe Some(
        CompanyRegistrationNumber(Some(true), Some(Crn("AB123456")))
      )
    }

    "store choice in session after successful submission and redirect to check-your-answers page if user is changing answers" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          companyRegistrationNumber = None,
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          agentCodes = None,
          personalDetails = Some(personalDetails),
          changingAnswers = true
        )
      )

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "AB123456")

      val result = controller.submitCompanyRegistrationNumber(authenticatedRequest)
      status(result) shouldBe 303

      header(LOCATION, result).get shouldBe routes.ApplicationController.showCheckYourAnswers.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.companyRegistrationNumber shouldBe Some(CompanyRegistrationNumber(Some(true), Some(Crn("AB123456"))))
      session.changingAnswers shouldBe false
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No)
        )
      )

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))

      controller.submitCompanyRegistrationNumber(authenticatedRequest).futureValue should containMessages(
        "companyRegistrationNumber.error.no-radio.selected"
      )
      sessionStoreService.fetchAgentSession.futureValue.get.companyRegistrationNumber shouldBe None
    }

    "show validation error if Yes is selected but no input passed for registrationNumber" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No)
        )
      )

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "")

      controller.submitCompanyRegistrationNumber(authenticatedRequest).futureValue should containMessages(
        "error.crn.blank"
      )
      sessionStoreService.fetchAgentSession.futureValue.get.companyRegistrationNumber shouldBe None
    }
  }

  "GET /self-assessment-agent-code" should {
    val defaultAgentSession = agentSession.copy(
      registeredWithHmrc = Some(Yes),
      agentCodes = None
    )
    class UkTaxRegistrationSetup(agentSession: AgentSession = defaultAgentSession) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showAgentCodesForm(authenticatedRequest)
      val doc = Jsoup.parse(contentAsString(result))
    }

    "contain page titles and header content" in new UkTaxRegistrationSetup {
      result.futureValue should containMessages(
        "agentCodes.title",
        "agentCodes.caption",
        "agentCodes.p1",
        "agentCodes.form.hint"
      )
    }

    "ask for optional self-assessment and corporation-tax" in new UkTaxRegistrationSetup {
      Seq("self-assessment", "corporation-tax").foreach { agentCode =>
        result.futureValue should containMessages(
          s"agentCodes.form.$agentCode.label",
          s"agentCodes.form.$agentCode.inset"
        )

        result.futureValue should containElement(
          id = s"$agentCode-checkbox",
          tag = "input",
          attrs = Map(
            "type"  -> "checkbox",
            "name"  -> s"$agentCode-checkbox",
            "value" -> "true"
          )
        )

        result.futureValue should containElement(
          id = agentCode,
          tag = "input",
          attrs = Map(
            "type" -> "text",
            "name" -> agentCode
          )
        )
      }
    }

    "show existing selection if session already contains choice" in
      new UkTaxRegistrationSetup(
        defaultAgentSession.copy(
          agentCodes = Some(AgentCodes(Some(SaAgentCode("saTestCode")), None))
        )
      ) {
        doc.select("#self-assessment-checkbox[checked]").toArray should have length 1
      }

    "contain a continue button" in new UkTaxRegistrationSetup {
      result.futureValue should containSubmitButton(
        expectedMessageKey = "button.continue",
        expectedElementId = "continue"
      )
    }

    "contain a back link to /registered-with-hmrc" in new UkTaxRegistrationSetup(
      defaultAgentSession.copy(
        registeredWithHmrc = Some(Yes)
      )
    ) {
      result.futureValue should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/registered-with-hmrc"
      )
    }

    "contain a back link to /check-your-answers if user is changing answers" in new UkTaxRegistrationSetup(
      defaultAgentSession.copy(
        registeredWithHmrc = Some(Yes),
        changingAnswers = true
      )
    ) {
      result.futureValue should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers"
      )
    }

    "contain a form that would POST to /self-assessment-agent-code" in new UkTaxRegistrationSetup {
      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/self-assessment-agent-code"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering-registration when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = controller.showUkTaxRegistrationForm(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
    }
  }

  "POST /self-assessment-agent-code" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = None,
          changingAnswers = true
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
        .withFormUrlEncodedBody(
          "self-assessment-checkbox" -> "true",
          "self-assessment"          -> "SA1234",
          "corporation-tax-checkbox" -> "true",
          "corporation-tax"          -> "123456"
        )

      val result = controller.submitAgentCodes(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showUkTaxRegistrationForm.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get

      session.agentCodes shouldBe Some(
        AgentCodes(
          Some(SaAgentCode("SA1234")),
          Some(CtAgentCode("123456"))
        )
      )

      session.changingAnswers shouldBe false
    }

    "redirect to /uk-tax-registration if no agent code is selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = None,
          changingAnswers = true
        )
      )
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "self-assessment" -> "",
          "corporation-tax" -> ""
        )
      val result = controller.submitAgentCodes(authenticatedRequest)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showUkTaxRegistrationForm.url

      val session = sessionStoreService.fetchAgentSession.futureValue.get
      session.agentCodes shouldBe Some(AgentCodes(None, None))
      session.changingAnswers shouldBe false
    }

    Seq("saAgentCode" -> "self-assessment", "ctAgentCode" -> "corporation-tax").foreach { case (key, value) =>
      s"show validation error if $value checkbox was selected but the text does not pass validation" in {

        sessionStoreService.currentSession.agentSession = Some(
          agentSession.copy(
            registeredWithHmrc = Some(Yes),
            agentCodes = None
          )
        )
        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest(POST, "/"))
          .withFormUrlEncodedBody(
            s"$value-checkbox" -> "true",
            "self-assessment"  -> "",
            "corporation-tax"  -> ""
          )

        controller.submitAgentCodes(authenticatedRequest).futureValue should containMessages(s"error.$key.blank")

        sessionStoreService.fetchAgentSession.futureValue.get.agentCodes shouldBe None
      }
    }
  }

  "GET /check-your-answers" should {

    def testAgentCodes(body: String, result: Boolean) = {
      body.contains("selfAssessmentCode") shouldBe result
      body.contains("corporationTaxCode") shouldBe result
    }

    def testMandatoryContent(result: Result): Seq[Assertion] =
      List(
        result should containMessages("checkAnswers.title"),
        result should containMessages("checkAnswers.change.button"),
        result should containMessages("checkAnswers.amlsDetails.title"),
        result should containMessages("checkAnswers.contactDetails.title"),
        result should containMessages("checkAnswers.BusinessDetails.title"),
        result should containMessages("checkAnswers.tradingName.title"),
        result should containMessages("checkAnswers.mainBusinessAddress.title"),
        result should containMessages("checkAnswers.tradingAddressFile.title"),
        result should containMessages("checkAnswers.registeredWithHmrc.title"),
        result should containMessages("checkAnswers.confirm.p1"),
        result should containMessages("checkAnswers.confirm.button")
      )

    "have correct back link" when {

      "back link to file-uploaded-successfully" in {
        sessionStoreService.currentSession.agentSession = Some(
          AgentSession(
            amlsRequired = Some(true),
            Some(amlsDetails),
            Some(contactDetails),
            Some("tradingName"),
            Some(overseasAddress),
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None, None)),
            registeredForUkTax = Some(No),
            companyRegistrationNumber = Some(CompanyRegistrationNumber(None, None)),
            hasTaxRegNumbers = Some(true),
            taxRegistrationNumbers = Some(SortedSet(Trn("someTaxRegNo"))),
            amlsUploadStatus = Some(fileUploadStatus),
            tradingAddressUploadStatus = Some(fileUploadStatus),
            trnUploadStatus = Some(fileUploadStatus),
            verifiedEmails = Set(contactDetails.businessEmail)
          )
        )

        val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

        status(result) shouldBe 200
        result.futureValue should containLink("button.back", routes.FileUploadController.showSuccessfulUploadedForm.url)
      }

      "no agent codes no taxRegNumbers provided, back link to ask if has tax-registration-number" in {
        sessionStoreService.currentSession.agentSession = Some(
          AgentSession(
            amlsRequired = Some(true),
            Some(amlsDetails),
            Some(contactDetails),
            Some("tradingName"),
            Some(overseasAddress),
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None, None)),
            registeredForUkTax = Some(No),
            companyRegistrationNumber = Some(CompanyRegistrationNumber(None, None)),
            hasTaxRegNumbers = Some(false),
            taxRegistrationNumbers = None,
            amlsUploadStatus = Some(fileUploadStatus),
            tradingAddressUploadStatus = Some(fileUploadStatus),
            trnUploadStatus = Some(fileUploadStatus),
            verifiedEmails = Set(contactDetails.businessEmail)
          )
        )

        val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

        status(result) shouldBe 200
        result.futureValue should containLink("button.back", routes.TaxRegController.showTaxRegistrationNumberForm.url)
      }
    }

    "agents codes are not available" when {
      "UkTaxRegistration is Yes" when {
        "CompanyRegistrationNumber is empty" in {
          val registeredWithHmrc = Some(Yes)
          val agentCodes =
            AgentCodes(None, None)

          sessionStoreService.currentSession.agentSession = Some(
            AgentSession(
              amlsRequired = Some(true),
              Some(amlsDetails),
              Some(contactDetails),
              Some("tradingName"),
              Some(overseasAddress),
              registeredWithHmrc,
              Some(agentCodes),
              registeredForUkTax = Some(Yes),
              personalDetails = Some(PersonalDetailsChoice(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
              companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None)),
              hasTaxRegNumbers = Some(false),
              tradingAddressUploadStatus = Some(fileUploadStatus),
              amlsUploadStatus = Some(fileUploadStatus),
              verifiedEmails = Set(contactDetails.businessEmail)
            )
          )

          val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

          status(result) shouldBe 200

          testMandatoryContent(result.futureValue)

          result.futureValue should containMessages(
            "checkAnswers.tradingAddressFile.title",
            "checkAnswers.agentCode.title",
            "checkAnswers.agentCode.empty",
            "checkAnswers.companyRegistrationNumber.title",
            "checkAnswers.companyRegistrationNumber.empty"
          )

          testAgentCodes(contentAsString(result), result = false)
        }

        "CompanyRegistrationNumber is non empty" in {
          val registeredWithHmrc = Some(Yes)
          val agentCodes =
            AgentCodes(None, None)

          sessionStoreService.currentSession.agentSession = Some(
            AgentSession(
              amlsRequired = Some(true),
              Some(amlsDetails),
              Some(contactDetails),
              Some("tradingName"),
              Some(overseasAddress),
              registeredWithHmrc,
              Some(agentCodes),
              registeredForUkTax = Some(Yes),
              personalDetails = Some(PersonalDetailsChoice(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
              companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some(Crn("999999")))),
              hasTaxRegNumbers = Some(false),
              tradingAddressUploadStatus = Some(fileUploadStatus),
              amlsUploadStatus = Some(fileUploadStatus),
              verifiedEmails = Set(contactDetails.businessEmail)
            )
          )

          val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

          status(result) shouldBe 200
          testMandatoryContent(result.futureValue)

          result.futureValue should containMessages(
            "checkAnswers.agentCode.title",
            "checkAnswers.agentCode.empty",
            "checkAnswers.companyRegistrationNumber.title"
          )

          testAgentCodes(contentAsString(result), result = false)
          contentAsString(result).contains("999999") shouldBe true
        }

        "TaxRegistrationNumbers is empty" in {
          val registeredWithHmrc = Some(Yes)
          val agentCodes = AgentCodes(None, None)

          sessionStoreService.currentSession.agentSession = Some(
            AgentSession(
              amlsRequired = Some(true),
              Some(amlsDetails),
              Some(contactDetails),
              Some("tradingName"),
              Some(overseasAddress),
              registeredWithHmrc,
              Some(agentCodes),
              registeredForUkTax = Some(Yes),
              personalDetails = Some(PersonalDetailsChoice(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
              companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None)),
              hasTaxRegNumbers = Some(false),
              taxRegistrationNumbers = None,
              tradingAddressUploadStatus = Some(fileUploadStatus),
              amlsUploadStatus = Some(fileUploadStatus),
              verifiedEmails = Set(contactDetails.businessEmail)
            )
          )

          val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

          status(result) shouldBe 200
          testMandatoryContent(result.futureValue)

          result.futureValue should containMessages(
            "checkAnswers.agentCode.title",
            "checkAnswers.agentCode.empty",
            "checkAnswers.companyRegistrationNumber.title",
            "checkAnswers.companyRegistrationNumber.empty",
            "checkAnswers.taxRegistrationNumbers.title",
            "checkAnswers.taxRegistrationNumbers.empty"
          )

          testAgentCodes(contentAsString(result), result = false)
        }

        "TaxRegistrationNumbers is non empty" in {
          val registeredWithHmrc = Some(Yes)
          val agentCodes =
            AgentCodes(None, None)

          sessionStoreService.currentSession.agentSession = Some(
            AgentSession(
              amlsRequired = Some(true),
              Some(amlsDetails),
              Some(contactDetails),
              Some("tradingName"),
              Some(overseasAddress),
              registeredWithHmrc,
              Some(agentCodes),
              registeredForUkTax = Some(Yes),
              personalDetails = Some(PersonalDetailsChoice(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
              companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some(Crn("123456")))),
              hasTaxRegNumbers = Some(true),
              taxRegistrationNumbers = Some(SortedSet(Trn("TX12345"))),
              tradingAddressUploadStatus = Some(fileUploadStatus),
              amlsUploadStatus = Some(fileUploadStatus),
              trnUploadStatus = Some(fileUploadStatus),
              verifiedEmails = Set(contactDetails.businessEmail)
            )
          )

          val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

          status(result) shouldBe 200

          testMandatoryContent(result.futureValue)
          result.futureValue should containMessages(
            "checkAnswers.registeredWithHmrc.title",
            "checkAnswers.agentCode.title",
            "checkAnswers.agentCode.empty",
            "checkAnswers.companyRegistrationNumber.title",
            "checkAnswers.taxRegistrationNumbers.title"
          )

          testAgentCodes(contentAsString(result), result = false)
          contentAsString(result).contains("TX12345") shouldBe true
          contentAsString(result).contains("tradingAddressFileName") shouldBe true
        }
      }
      "UkTaxRegistration is No" in {
        val registeredWithHmrc = Some(Yes)
        val agentCodes =
          AgentCodes(None, None)

        sessionStoreService.currentSession.agentSession = Some(
          AgentSession(
            amlsRequired = Some(true),
            Some(amlsDetails),
            Some(contactDetails),
            Some("tradingName"),
            Some(overseasAddress),
            registeredWithHmrc,
            Some(agentCodes),
            registeredForUkTax = Some(No),
            companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None)),
            hasTaxRegNumbers = Some(false),
            tradingAddressUploadStatus = Some(fileUploadStatus),
            amlsUploadStatus = Some(fileUploadStatus),
            verifiedEmails = Set(contactDetails.businessEmail)
          )
        )

        val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

        status(result) shouldBe 200

        testMandatoryContent(result.futureValue)
        result.futureValue should containMessages(
          "checkAnswers.title",
          "checkAnswers.change.button",
          "checkAnswers.amlsDetails.title",
          "checkAnswers.contactDetails.title",
          "checkAnswers.BusinessDetails.title",
          "checkAnswers.tradingName.title",
          "checkAnswers.mainBusinessAddress.title",
          "checkAnswers.registeredWithHmrc.title",
          "checkAnswers.agentCode.title",
          "checkAnswers.agentCode.empty"
        )

        result.futureValue shouldNot containMessages("checkAnswers.personalDetails.nino.title")

        testAgentCodes(contentAsString(result), result = false)
        contentAsString(result).contains("tradingAddressFileName") shouldBe true
      }
    }

    "should display the form with all data as expected when user goes through 'RegsiteredWithHmrc=No' flow" in {
      val registeredWithHmrc = Some(No)

      sessionStoreService.currentSession.agentSession = Some(
        AgentSession(
          amlsRequired = Some(true),
          Some(amlsDetails),
          Some(contactDetails),
          Some("tradingName"),
          Some(overseasAddress),
          registeredWithHmrc,
          registeredForUkTax = Some(Yes),
          personalDetails = Some(personalDetails),
          companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some(Crn("crnCode")))),
          hasTaxRegNumbers = Some(true),
          taxRegistrationNumbers = Some(SortedSet(Trn("trn1"), Trn("trn2"))),
          tradingAddressUploadStatus = Some(fileUploadStatus),
          amlsUploadStatus = Some(fileUploadStatus),
          trnUploadStatus = Some(fileUploadStatus),
          verifiedEmails = Set(contactDetails.businessEmail)
        )
      )

      val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 200

      testMandatoryContent(result.futureValue)
      result.futureValue should containMessages(
        "checkAnswers.registeredForUKTax.title",
        "checkAnswers.personalDetails.nino.title",
        "checkAnswers.companyRegistrationNumber.title",
        "checkAnswers.taxRegistrationNumbers.title"
      )

      result.futureValue shouldNot containMessages("checkAnswers.agentCode.title", "checkAnswers.agentCode.empty")

      val body = contentAsString(result)

      body.contains("No") shouldBe true
      body.contains("Yes") shouldBe true
      body.contains("AB123456A") shouldBe true
      body.contains("crnCode") shouldBe true
      body.contains("trn1") shouldBe true
      body.contains("trn2") shouldBe true
      contentAsString(result).contains("tradingAddressFileName") shouldBe true
    }

    "should redirect to the verify email stage when the email address is unverified" in {
      val registeredWithHmrc = Some(No)

      sessionStoreService.currentSession.agentSession = Some(
        AgentSession(
          amlsRequired = Some(true),
          Some(amlsDetails),
          Some(contactDetails),
          Some("tradingName"),
          Some(overseasAddress),
          registeredWithHmrc,
          registeredForUkTax = Some(Yes),
          personalDetails = Some(personalDetails),
          companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some(Crn("crnCode")))),
          hasTaxRegNumbers = Some(true),
          taxRegistrationNumbers = Some(SortedSet(Trn("trn1"), Trn("trn2"))),
          tradingAddressUploadStatus = Some(fileUploadStatus),
          amlsUploadStatus = Some(fileUploadStatus),
          trnUploadStatus = Some(fileUploadStatus),
          verifiedEmails = Set.empty
        )
      )

      val result = controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationEmailVerificationController.verifyEmail.url)
    }
  }

  "POST /check-your-answers" should {

    def initialTestSetup = {
      val registeredWithHmrc = Some(Yes)
      val agentCodes =
        AgentCodes(Some(SaAgentCode("selfAssessmentCode")), Some(CtAgentCode("corporationTaxCode")))

      val agentSession = AgentSession(
        amlsRequired = Some(true),
        amlsDetails = Some(amlsDetails),
        contactDetails = Some(contactDetails),
        tradingName = Some("tradingName"),
        overseasAddress = Some(overseasAddress),
        registeredWithHmrc = registeredWithHmrc,
        agentCodes = Some(agentCodes),
        amlsUploadStatus = Some(fileUploadStatus),
        tradingAddressUploadStatus = Some(fileUploadStatus),
        trnUploadStatus = Some(fileUploadStatus),
        verifiedEmails = Set(contactDetails.businessEmail)
      )

      sessionStoreService.currentSession.agentSession = Some(agentSession)
      agentSession
    }

    "show error when user doesn't accept confirmation" in {
      val agentSession = initialTestSetup

      givenPostOverseasApplication(201, Json.toJson(CreateOverseasApplicationRequest(agentSession)).toString())

      val result = controller.submitCheckYourAnswers(cleanCredsAgent(FakeRequest(POST, "/")))

      status(result) shouldBe 400

      sessionStoreService.fetchAgentSession.futureValue.isDefined shouldBe true
    }

    "submit the application and redirect to application-complete" in {
      val agentSession = initialTestSetup

      givenPostOverseasApplication(201, Json.toJson(CreateOverseasApplicationRequest(agentSession)).toString())

      val result =
        controller.submitCheckYourAnswers(
          cleanCredsAgent(
            FakeRequest(POST, "/").withFormUrlEncodedBody("confirmed" -> "true")
          )
        )

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.ApplicationController.showApplicationComplete.url

      flash(result).get("tradingName").get shouldBe "tradingName"
      flash(result).get("contactDetail").get shouldBe "test@email.com"

      sessionStoreService.fetchAgentSession.futureValue.isDefined shouldBe false
    }

    "return exception when agent-overseas-application backend is unavailable" in {
      val agentSession = initialTestSetup

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      givenPostOverseasApplication(503, Json.toJson(CreateOverseasApplicationRequest(agentSession)).toString())

      val e = controller
        .submitCheckYourAnswers(
          cleanCredsAgent(
            FakeRequest(POST, "/").withFormUrlEncodedBody("confirmed" -> "true")
          )
        )
        .failed
        .futureValue
      e shouldBe a[Exception]
    }

    "redirect to /money-laundering-registration when session not found" in {
      val result = controller.submitCheckYourAnswers()(cleanCredsAgent(FakeRequest(POST, "/")))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url)
    }
  }

  "GET / application-complete" should {
    "should display the page data as expected" in {
      val tradingName = "testTradingName"
      val email = "testEmail@test.com"

      val result =
        controller.showApplicationComplete(
          basicAgentRequest(FakeRequest().withFlash("tradingName" -> tradingName, "contactDetail" -> email))
        )

      status(result) shouldBe 200

      val html = Jsoup.parse(contentAsString(result))
      html
        .title() shouldBe "Application complete - Apply for an agent services account if you are not in the UK - GOV.UK"
      html.select(Css.H1).text() shouldBe "Application complete"

      val h2s = html.select(Css.H2)
      h2s.get(0).text() shouldBe "What happens next"
      h2s.get(1).text() shouldBe "What you can do next"
      h2s.get(2).text() shouldBe "If you need help"

      val paras = html.select(Css.paragraphs)
      paras.get(0).text() shouldBe "We will send a confirmation email to testEmail@test.com."
      paras.get(1).text() shouldBe "We may get in touch with you to discuss your application."
      paras.get(2).text() shouldBe "We will tell you within 28 calendar days if your application has been approved. We will also tell you how to set up your account."
      paras.get(3).text() shouldBe "If your application is rejected we will tell you why."
      paras
        .get(4)
        .text() shouldBe "Check the guidance on GOV.UK to find out how to track the progress of your application."
      paras.get(6).text() shouldBe "Print this page"
      paras.get(6).select("a").text() shouldBe "Print this page"
      paras.get(6).select("a").attr("href") shouldBe "javascript:window.print()"
      paras.get(7).text() shouldBe "Finish and sign out"
      paras.get(7).select("a").text() shouldBe "Finish and sign out"
      paras.get(7).select("a").attr("href") shouldBe "/agent-services/apply-from-outside-uk/sign-out"
      paras.get(8).text() shouldBe "What did you think of this service? (takes 30 seconds)"
    }

    "303 to JOURNEY START when no required fields in flash, authAction should deal with routing circumstances" in {
      val result = controller.showApplicationComplete(basicAgentRequest(FakeRequest()))

      // as the Agent should have no agentSession at this point, previous last created application would be shown or start of Journey
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm.url
    }
  }

  "email verification" should {
    def checkVerifyEmailIsTriggered(f: () => Future[Result]) = {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(verifiedEmails = Set.empty))
      val result = f()
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.ApplicationEmailVerificationController.verifyEmail.url
    }
    def checkVerifyEmailIsNotTriggered(f: () => Future[Result]) = {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(verifiedEmails = Set.empty))
      val result = f()
      status(result) should (equal(200) or equal(303))
      if (status(result) == 303)
        redirectLocation(result) should not be routes.ApplicationEmailVerificationController.verifyEmail.url
    }
    "be triggered with an unverified email" when {
      "show trading name" in checkVerifyEmailIsTriggered(() =>
        controller.showTradingNameForm(cleanCredsAgent(FakeRequest()))
      )
      "submit trading name" in checkVerifyEmailIsTriggered(() =>
        controller.submitTradingName(cleanCredsAgent(FakeRequest()))
      )
      "show registered with HMRC" in checkVerifyEmailIsTriggered(() =>
        controller.showRegisteredWithHmrcForm(cleanCredsAgent(FakeRequest()))
      )
      "submit registered with HMRC" in checkVerifyEmailIsTriggered(() =>
        controller.submitRegisteredWithHmrc(cleanCredsAgent(FakeRequest()))
      )
      "show agent codes form" in checkVerifyEmailIsTriggered(() =>
        controller.showAgentCodesForm(cleanCredsAgent(FakeRequest()))
      )
      "submit agent codes form" in checkVerifyEmailIsTriggered(() =>
        controller.submitAgentCodes(cleanCredsAgent(FakeRequest()))
      )
      "show uk tax reg form" in checkVerifyEmailIsTriggered(() =>
        controller.showUkTaxRegistrationForm(cleanCredsAgent(FakeRequest()))
      )
      "submit uk tax reg" in checkVerifyEmailIsTriggered(() =>
        controller.submitUkTaxRegistration(cleanCredsAgent(FakeRequest()))
      )
      "show company reg form" in checkVerifyEmailIsTriggered(() =>
        controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest()))
      )
      "submit company reg" in checkVerifyEmailIsTriggered(() =>
        controller.submitCompanyRegistrationNumber(cleanCredsAgent(FakeRequest()))
      )
      "check answers" in checkVerifyEmailIsTriggered(() =>
        controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))
      )
      "submit check answers" in checkVerifyEmailIsTriggered(() =>
        controller.submitCheckYourAnswers(cleanCredsAgent(FakeRequest()))
      )
    }
    "not be triggered when using with the email retrieved by auth" when {
      "check answers" in checkVerifyEmailIsNotTriggered { () =>
        // we use the email (authemail@email.com) which is returned in the mock auth response
        sessionStoreService.currentSession.agentSession = Some(
          agentSession.copy(
            contactDetails = agentSession.contactDetails.map(_.copy(businessEmail = "authemail@email.com")),
            verifiedEmails = Set.empty
          )
        )
        controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest()))
      }
      // also all other pages but checking one should be enough as they all use the same logic
    }

    "not be triggered even with an unverified mail" when {
      // these pages must display even with an unverified email otherwise the user couldn't enter or correct their email address!
      "show contact details form" in checkVerifyEmailIsNotTriggered(() =>
        controller.showContactDetailsForm(cleanCredsAgent(FakeRequest()))
      )
      "submit contact details" in checkVerifyEmailIsNotTriggered(() =>
        controller.submitContactDetails(cleanCredsAgent(FakeRequest()))
      )
    }
  }
}
