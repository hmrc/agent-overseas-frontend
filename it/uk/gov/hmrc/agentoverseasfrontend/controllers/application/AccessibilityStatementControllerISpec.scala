package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

class AccessibilityStatementControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  private lazy val controller = app.injector.instanceOf[AccessibilityStatementController]

  "GET /accessibility-statement" should {
    "show the accessibility statement content" in {
      val result = await(controller.showAccessibilityStatement(basicRequest(FakeRequest().withHeaders(HeaderNames.REFERER -> "foo"))))

      status(result) shouldBe 200
      result should containMessages("application.accessibility.statement.h1")
      result should containSubstrings("/contact/accessibility?service=AOSS&userAction=foo")
     }
  }
}