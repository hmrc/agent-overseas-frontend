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

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

class SubscriptionRootControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {
  val arn = Arn("TARN0000001")

  lazy val controller = app.injector.instanceOf[SubscriptionRootController]

  "root" should {
    "303 to start of journey: showCheckAnswers" in {
      val result = controller.root(authenticatedAs(subscribingCleanAgentWithoutEnrolments))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.BusinessIdentificationController.showCheckAnswers.url
    }
  }

  "nextStep" should {
    "return 200 with create GG account page when the user does not have enrolments" in {
      testNextStepPage(authenticatedAs(subscribingCleanAgentWithoutEnrolments))
    }

    "return 200 with create GG account page when the user have un clean creds" in {
      testNextStepPage(authenticatedAs(subscribingAgentEnrolledForNonMTD))
    }

    def testNextStepPage(request: FakeRequest[AnyContentAsEmpty.type]) = {
      givenAcceptedApplicationResponse()
      val result = controller.nextStep(request)
      status(result) shouldBe 200

      result.futureValue should containMessages("createNewAccount.title", "createNewAccount.p1", "button.continue")
    }
  }

  "showApplicationIssue /cannot-check-status" should {
    "200 cannot_check_Status page is shown" in {
      val result = controller.showApplicationIssue(authenticatedAs(subscribingCleanAgentWithoutEnrolments))

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "cannot_check_status.title",
        "cannot_check_status.p1",
        "cannot_check_status.p2")
    }
  }
}
