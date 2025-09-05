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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.stubs.StubsTestData._
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentSubscriptionStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.agentoverseasfrontend.support.Css
import uk.gov.hmrc.http.SessionKeys

import java.util.UUID

class SubscriptionControllerISpec
extends BaseISpec
with AgentOverseasApplicationStubs
with AgentSubscriptionStubs {

  val arn = Arn("TARN0000001")

  private val agentServicesAccountBase = "http://localhost:9401"
  private val agentServicesAccountPath = "/agent-services-account"
  private val guidancePageUrl = "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account"

  lazy val controller = app.injector.instanceOf[SubscriptionController]

  "subscribe" should {
    "redirect to /complete upon successful subscription" in {
      implicit val request = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)
      givenAcceptedApplicationResponse()
      givenApplicationUpdateSuccessResponse()
      givenSubscriptionSuccessfulResponse(arn)

      val result = controller.subscribe(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.SubscriptionController.subscriptionComplete.url
    }

    "redirect to /check-answers if there's no agency details in the session" in {
      implicit val request = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      sessionCacheService.currentSession.agencyDetails = None
      givenAcceptedApplicationResponse()
      val result = controller.subscribe(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }

    "redirect to /next-steps if user has unclean credentials (they have 1 or more enrolments)" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)
      val result = controller.subscribe(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.SubscriptionRootController.nextStep.url
    }

    "redirect to /already-subscribed if the HMRC-AS-AGENT enrolment with their ARN is already allocated to a group" in {
      implicit val request = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)
      givenCompleteApplicationResponse()
      givenSubscriptionFailedConflict()
      val result = controller.subscribe(request)

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe routes.SubscriptionController.alreadySubscribed.url
    }
  }

  "subscriptionComplete" should {
    "showSubscriptionCompletePage when HMRC-AS-AGENT" in {
      implicit val request = authenticated(
        FakeRequest(),
        Enrolment(
          "HMRC-AS-AGENT",
          "AgentReferenceNumber",
          arn.value
        ),
        true
      )
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails)
      val result = controller.subscriptionComplete(request)

      status(result) shouldBe 200

      result.futureValue should containSubstrings(
        htmlMessage("subscriptionComplete.p1", "TARN0000001"),
        agentServicesAccountBase + agentServicesAccountPath,
        "test agency name",
        htmlMessage("subscriptionComplete.p2", "test-agency-email@domain.com"),
        htmlMessage("subscriptionComplete.p3.1", guidancePageUrl),
        htmlMessage("subscriptionComplete.p3.2")
      )

      result.futureValue should containMessages(
        "subscriptionComplete.title",
        "subscriptionComplete.accountName",
        "subscriptionComplete.h2",
        "subscriptionComplete.bullet-list.1",
        "subscriptionComplete.bullet-list.2",
        "subscriptionComplete.button"
      )
    }

  }

  "/already-subscribed" should {
    "show the already subscribed page for a user with Agent affinity group" in {
      implicit val request = authenticatedAs(subscribingCleanAgentWithoutEnrolments)

      val result = controller.alreadySubscribed(request)

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "already.subscribed.title",
        "already.subscribed.p1",
        "finish.signout"
      )
    }
  }

  "email verification" should {
    "be triggered if attempting to subscribe with an unverified email" in {
      implicit val request = authenticatedAs(subscribingCleanAgentWithoutEnrolments)
      sessionCacheService.currentSession.agencyDetails = Some(agencyDetails.copy(verifiedEmails = Set.empty))
      givenAcceptedApplicationResponse()
      givenApplicationUpdateSuccessResponse()
      givenSubscriptionSuccessfulResponse(arn)

      val result = controller.subscribe(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.SubscriptionEmailVerificationController.verifyEmail.url
    }
  }

  "GET /create-account/email-locked" should {
    "should display the page data as expected" in {
      stubResponseFromAuthenticationEndpoint()

      val fakeAuthenticatedRequestToViewPage = FakeRequest().withSession(
        SessionKeys.authToken -> "Bearer XYZ",
        SessionKeys.sessionId -> UUID.randomUUID().toString
      )

      val result = controller.showEmailLocked(
        fakeAuthenticatedRequestToViewPage
      )

      status(result) shouldBe 200

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "We could not confirm your identity - Apply for an agent services account if you are not in the UK - GOV.UK"
      html.select(Css.H1).text() shouldBe "We could not confirm your identity"

      val h2_headers = html.select(Css.H2)
      h2_headers.get(0).text() shouldBe "What to do next"

      val paragraphs = html.select(Css.paragraphs)
      paragraphs.get(0).text() shouldBe "We cannot check your identity because you entered an incorrect verification code too many times."
      paragraphs.get(1).text() shouldBe "The verification code was emailed to you."
      paragraphs.get(2).text() shouldBe "You can try again in 24 hours."
      paragraphs.get(
        3
      ).text() shouldBe "If you want to try again with a different email address you can change the email address you entered (opens in new tab)."

      val hyperLinks = html.select("p > a")
      hyperLinks.get(0).attr("href") shouldBe "/agent-services/apply-from-outside-uk/create-account/update-business-email"
    }

  }

  "GET /create-account/email-technical-error" should {
    "should display the page data as expected" in {
      stubResponseFromAuthenticationEndpoint()

      val fakeAuthenticatedRequestToViewPage = FakeRequest().withSession(
        SessionKeys.authToken -> "Bearer XYZ",
        SessionKeys.sessionId -> UUID.randomUUID().toString
      )

      val result = controller.showEmailTechnicalError(
        fakeAuthenticatedRequestToViewPage
      )

      status(result) shouldBe 200

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "We could not confirm your identity - Apply for an agent services account if you are not in the UK - GOV.UK"
      html.select(Css.H1).text() shouldBe "We could not confirm your identity"

      val h2_headers = html.select(Css.H2)
      h2_headers.get(0).text() shouldBe "What to do next"

      val paragraphs = html.select(Css.paragraphs)
      paragraphs.get(0).text() shouldBe "We cannot check your identity because there is a temporary problem with our service."
      paragraphs.get(1).text() shouldBe "You can try again in 24 hours."
    }

  }

}
