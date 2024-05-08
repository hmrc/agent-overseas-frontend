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

import org.mongodb.scala.model.Filters
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

import java.net.URLEncoder
import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionSignOutControllerISpec extends BaseISpec {
  lazy val controller = app.injector.instanceOf[SubscriptionSignOutController]

  "signOutWithContinueUrl" should {
    "storeAuthProviderId and redirect to GgCreateAccount" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)

      val result = controller.signOutWithContinueUrl(request)
      val _ = result.futureValue // await the completion

      val details = findByAuthProviderId("12345-credId")
      val detailsRef = details.map(_.id).get

      status(result) shouldBe 303

      val continueUrl = URLEncoder.encode(
        s"http://localhost:9414${routes.BusinessIdentificationController.returnFromGGRegistration(detailsRef)}",
        "UTF-8")
      header(LOCATION, result).get should
        include(
          s"http://localhost:8571/government-gateway-registration-frontend?accountType=agent&origin=unknown&continue=$continueUrl")
    }
  }

  "startSurvey" should {
    "redirect to feedback survey page" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)

      val result = controller.startFeedbackSurvey(request)

      status(result) shouldBe 303
      header(LOCATION, result).get should include("/feedback/OVERSEAS_AGENTS")
    }
  }

  "/finish-sign-out" should {
    "redirect to the root page" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)

      val result = controller.signOut(request)

      status(result) shouldBe 303

      header(LOCATION, result).get should include("/")
    }
  }

  "/signed-out" should {
    "forbidden with signed_out page containing link to root" in {
      implicit val request = FakeRequest()

      val result = controller.signedOut(request)

      status(result) shouldBe 403

      checkMessageIsDefined("signed-out.header")
      checkMessageIsDefined("signed-out.p1")
    }
  }

  "/timed-out" should {
    "display the timed out page" in {
      implicit val request = FakeRequest()

      val result = controller.timedOut(request)

      status(result) shouldBe 403

      checkMessageIsDefined("timed-out.header")
      checkMessageIsDefined("timed-out.p2.link")
    }
  }

  private def findByAuthProviderId(authProviderId: String): Option[SessionDetails] =
    sessionDetailsRepo.collection
      .find(Filters.equal("authProviderId", authProviderId))
      .toFuture()
      .map(_.headOption)
      .futureValue

}
