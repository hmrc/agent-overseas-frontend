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

package uk.gov.hmrc.agentoverseasfrontend.controllers

import org.mongodb.scala.model.Filters
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.stubs.SampleUser._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.{routes => applicationRoutes}
import uk.gov.hmrc.agentoverseasfrontend.controllers.subscription.{routes => subscriptionRoutes}
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import sttp.model.Uri.UriContext
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SignOutControllerISpec
extends BaseISpec {

  lazy val controller: SignOutController = app.injector.instanceOf[SignOutController]

  "signOutToGGRegistrationWhenSubscribing" should {
    "storeAuthProviderId and redirect to GgCreateAccount" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)
      val result = controller.signOutToGGRegistrationWhenSubscribing(request)
      val _ = result.futureValue
      val details = findByAuthProviderId("12345-credId").futureValue
      val detailsRef = details.map(_.id).get
      val continueFromGG = uri"""${"http://localhost:9414" + subscriptionRoutes.BusinessIdentificationController.returnFromGGRegistration(detailsRef)}"""
      val params = Seq(
        "accountType" -> "agent",
        "origin" -> "unknown",
        "continue" -> continueFromGG.toString
      )
      val continueUrl = uri"http://localhost:8571/government-gateway-registration-frontend?${params}"

      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe uri"""${controller.appConfig.signOutUrl}?${Map("continue" -> continueUrl.toString)}""".toString
    }
  }

  "signOutToGGRegistration" should {
    "redirect to GgCreateAccount" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)
      val continueFromGG = uri"""${controller.appConfig.selfExternalUrl + applicationRoutes.ApplicationRootController.root.url}"""
      val params = Seq(
        "accountType" -> "agent",
        "origin" -> "unknown",
        "continue" -> continueFromGG.toString
      )
      val continueUrl = uri"http://localhost:8571/government-gateway-registration-frontend?${params}"
      val result = controller.signOutToGGRegistration(request)
      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe uri"""${controller.appConfig.signOutUrl}?${Map("continue" -> continueUrl.toString)}""".toString
    }
  }

  "startFeedbackSurvey" should {
    "redirect to feedback survey page" in {
      implicit val request = authenticatedAs(subscribingAgentEnrolledForNonMTD)
      val expectedLocation = uri"${controller.appConfig.signOutUrl}?${Map("continue" -> controller.appConfig.feedbackSurveyUrl)}"
      val result = controller.startFeedbackSurvey(request)
      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe expectedLocation.toString
    }
  }

  "/sign-out" should {
    "redirect to bas-gateway-frontend" in {
      val continue = uri"${controller.appConfig.selfExternalUrl + applicationRoutes.ApplicationRootController.root.url}"
      val expectedLocation = uri"${controller.appConfig.signOutUrl}?${Map("continue" -> continue.toString)}"
      val result = controller.signOut(FakeRequest())
      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe expectedLocation.toString
    }
  }

  "/time-out" should {
    "redirect to bas-gateway-frontend" in {
      val continueUrl = uri"${controller.appConfig.selfExternalUrl + routes.SignOutController.timedOut.url}"
      val expectedLocation = uri"""${controller.appConfig.signOutUrl}?${Map("continue" -> continueUrl.toString)}"""
      val result = controller.timeOut(FakeRequest())
      status(result) shouldBe 303
      header(LOCATION, result).get shouldBe expectedLocation.toString
    }
  }

  "/timed-out" should {
    "display the timed out page" in {
      implicit val request = FakeRequest()
      val result = controller.timedOut(request)
      status(result) shouldBe 200
      checkMessageIsDefined("timed-out.header")
      checkMessageIsDefined("timed-out.p2.link")
    }
  }

  private def findByAuthProviderId(authProviderId: String): Future[Option[SessionDetails]] = sessionDetailsRepo.collection
    .find(Filters.equal("authProviderId", authProviderId))
    .toFuture()
    .map(_.headOption)

}
