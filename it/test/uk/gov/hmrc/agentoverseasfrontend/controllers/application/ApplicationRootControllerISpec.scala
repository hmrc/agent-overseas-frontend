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

import java.time.Clock
import java.time.LocalDateTime
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.stubs._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.agentoverseasfrontend.support.Css

class ApplicationRootControllerISpec
extends BaseISpec
with AgentOverseasApplicationStubs {

  private lazy val controller = app.injector.instanceOf[ApplicationRootController]

  "GET /not-agent" should {
    "display the non-agent  page when the current user is logged in" in {
      val result = controller.showNotAgent(basicRequest(FakeRequest()))

      status(result) shouldBe 200

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "You have not signed in with an agent user ID - Apply for an agent services account if you are not in the UK - GOV.UK"

      val paras = html.select(Css.paragraphs)
      paras.get(0).text() shouldBe "To continue, sign in with an agent user ID."
      paras.get(0).select("a").attr("href") shouldBe "/agent-services/apply-from-outside-uk/sign-out"
      paras.get(0).select("a").text() shouldBe "sign in with an agent user ID"
      paras.get(1).text() shouldBe "If you do not have one, create a new Government Gateway user ID, selecting the ‘Agent’ option."
      paras.get(1).select("a").attr("href") shouldBe "/agent-services/apply-from-outside-uk/sign-out/create-account"
      paras.get(1).select("a").text() shouldBe "create a new Government Gateway user ID"

    }
  }

  "GET / " should {
    "simply redirect to start of journey showMoneyLaunderingRequired" in {
      given200GetOverseasApplications(true)
      val result = controller.root(basicRequest(FakeRequest()))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
    }
  }

  "/application-status applicationStatus PENDING status" should {
    "200 display content with application creation date & default 0 days if beyond 28 days from creation date" in {
      given200OverseasPendingApplication(Some("2018-02-01T15:11:51.729"))

      val result = controller.applicationStatus(basicRequest(FakeRequest()))

      status(result) shouldBe 200

      val html = Jsoup.parse(contentAsString(result))
      html
        .title() shouldBe "Application received - Apply for an agent services account if you are not in the UK - GOV.UK"
      html.select(Css.H1).text() shouldBe "Application received"

      val h2s = html.select(Css.H2)
      h2s.get(0).text() shouldBe "What happens next"
      h2s.get(1).text() shouldBe "If you need help"

      val paras = html.select(Css.paragraphs)
      paras.get(0).text() shouldBe "We received your application for approval to create an agent services account for Testing Agency on 1 February 2018."
      paras.get(1).text() shouldBe "We may get in touch with you to discuss your application."
      paras.get(
        2
      ).text() shouldBe "We will tell you within 0 calendar days if your application has been approved. We will also tell you how to set up your account."
      paras.get(3).text() shouldBe "If your application is rejected we will tell you why."
      paras.get(4).text() shouldBe "If you need help using this service, use the ‘get help with this page’ link at the bottom of this page."
      paras.get(5).text() shouldBe "Finish and sign out"
      paras.get(5).select("a").text() shouldBe "Finish and sign out"
      paras.get(5).select("a").attr("href") shouldBe "/agent-services/apply-from-outside-uk/sign-out"
    }

    "200 28 days to review when fresh application" in {
      given200OverseasPendingApplication(Some(LocalDateTime.now(Clock.systemUTC()).toString))

      val result = controller.applicationStatus(basicRequest(FakeRequest()))

      result.futureValue should containSubstrings(htmlMessage("application_not_ready.p3", 28))
    }

    "redirect to application root page when Pending application not found" in {
      given404OverseasApplications()

      val result = controller.applicationStatus(basicRequest(FakeRequest()))

      redirectLocation(result).get shouldBe routes.ApplicationRootController.root.url
    }
  }

  "GET /application-status applicationStatus Rejected status" should {
    "200 show detail about last rejected application with link to start new application" in {
      given200GetOverseasApplications(true)
      val result = controller.applicationStatus(basicRequest(FakeRequest()))

      val stubMatchingTradingName = "Testing Agency"
      val stubMatchingEmail = "test@test.com"

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "statusRejected.title",
        "statusRejected.heading",
        "statusRejected.para3"
      )
      result.futureValue should containSubstrings(
        htmlEscapedMessage("statusRejected.para1", stubMatchingTradingName),
        htmlMessage("statusRejected.para2", s"<strong class=bold-small>$stubMatchingEmail</strong>")
      )

      result.futureValue should containLink(
        "statusRejected.link.text",
        routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url
      )
    }
  }

  "GET / application-status applicationStatus Accepted status" should {

    behave like redirectToSubscriptionFrontend("accepted", controller.applicationStatus)
  }

  "GET / application-status applicationStatus Registered status" should {

    behave like redirectToSubscriptionFrontend("registered", controller.applicationStatus)
  }

  "GET / application-status applicationStatus Complete status" should {

    behave like redirectToSubscriptionFrontend("complete", controller.applicationStatus)
  }

  def redirectToSubscriptionFrontend(
    status: String,
    action: Action[AnyContent]
  ): Unit =
    "303 when application status neither Rejected nor Pending" in {
      given200OverseasRedirectStatusApplication(status)
      redirectLocation(
        action(basicRequest(FakeRequest()))
      ).get shouldBe "http://localhost:9414/agent-services/apply-from-outside-uk/create-account"
    }

}
