package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import java.time.{Clock, LocalDateTime}

import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.stubs._
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

class ApplicationRootControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  private lazy val controller = app.injector.instanceOf[ApplicationRootController]

  "GET /not-agent" should {
    "display the non-agent  page when the current user is logged in" in {
      val result = await(controller.showNotAgent(basicRequest(FakeRequest())))

      status(result) shouldBe 200
      result should containMessages("nonAgent.title")
      result should containSubstrings(htmlMessage("nonAgent.p1", routes.ApplicationSignOutController.signOut().url),htmlMessage("nonAgent.p2", routes.ApplicationSignOutController.signOutWithContinueUrl()))
    }
  }

  "GET / " should {
    "simply redirect to start of journey showMoneyLaunderingRequired" in {
      given200GetOverseasApplications(true)
      val result = await(controller.root(basicRequest(FakeRequest())))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url
    }
  }

  "/application-status applicationStatus PENDING status" should {
    "200 display content with application creation date & default 0 days if beyond 28 days from creation date" in {
      given200OverseasPendingApplication(Some("2018-02-01T15:11:51.729"))

      val result = await(controller.applicationStatus(basicRequest(FakeRequest())))

      status(result) shouldBe 200
      result should containSubstrings(htmlMessage("application_not_ready.p1", "Testing Agency", "1 February 2018"),
        htmlMessage("application_not_ready.p3", 0))
      result should containMessages("application_not_ready.title",
        "application_not_ready.h2",
        "application_not_ready.p2",
        "application_not_ready.p4",
        "application_not_ready.h3",
        "application_not_ready.p5")
    }

    "200 28 days to review when fresh application" in {
      given200OverseasPendingApplication(Some(LocalDateTime.now(Clock.systemUTC()).toString))

      val result = await(controller.applicationStatus(basicRequest(FakeRequest())))

      result should containSubstrings(htmlMessage("application_not_ready.p3", 28))
    }

    "redirect to application root page when Pending application not found" in {
      given404OverseasApplications()

      val result = await(controller.applicationStatus(basicRequest(FakeRequest())))

      redirectLocation(result).get shouldBe routes.ApplicationRootController.root().url
    }
  }

  "GET /application-status applicationStatus Rejected status" should {
    "200 show detail about last rejected application with link to start new application" in {
      given200GetOverseasApplications(true)
      val result = await(controller.applicationStatus(basicRequest(FakeRequest())))

      val stubMatchingTradingName = "Testing Agency"
      val stubMatchingEmail = "test@test.com"

      status(result) shouldBe 200
      result should containMessages("statusRejected.title", "statusRejected.heading", "statusRejected.para3")
      result should containSubstrings(htmlEscapedMessage("statusRejected.para1", stubMatchingTradingName),
        htmlMessage("statusRejected.para2", s"<strong class=bold-small>$stubMatchingEmail</strong>"))

      result should containLink("statusRejected.link.text", routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url)
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

  def redirectToSubscriptionFrontend(status: String, action: Action[AnyContent]): Unit = {
    "303 when application status neither Rejected nor Pending" in {
      given200OverseasRedirectStatusApplication(status)
      redirectLocation(await(action(basicRequest(FakeRequest())))).get shouldBe "http://localhost:9414/agent-services/apply-from-outside-uk/create-account"
    }
  }
}