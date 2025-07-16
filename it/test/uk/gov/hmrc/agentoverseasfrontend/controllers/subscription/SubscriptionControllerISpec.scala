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

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.stubs.StubsTestData._
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentSubscriptionStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

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

}
