package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import org.jsoup.Jsoup
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasfrontend.models.{AgentSession, FileUploadStatus}
import uk.gov.hmrc.agentoverseasfrontend.stubs.{AgentOverseasApplicationStubs, UpscanStubs}
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class FileUploadControllerISpec extends BaseISpec with AgentOverseasApplicationStubs with UpscanStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val controller: FileUploadController = app.injector.instanceOf[FileUploadController]

  private val agentSession = AgentSession()

  "GET /upload-proof-trading-address" should {
    "display the upload trading address form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = await(controller.showTradingAddressUploadForm()(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.caption",
        "fileUpload.title.trading-address",
        "fileUpload.p1.trading-address",
        "fileUpload.p2",
        "fileUpload.li.1.trading-address",
        "fileUpload.li.2",
        "fileUpload.li.3",
        "fileUpload.upload",
        "fileUpload.inset",
        "fileUpload.button"
      )
    }
  }

  "GET /trading-address-no-js-check-file" should {
    "display the page with correct content" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = await(controller.showTradingAddressNoJsCheckPage(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUploadTradingAddress.no_js_page.caption",
        "fileUploadTradingAddress.no_js_page.title",
        "fileUploadTradingAddress.no_js_page.p1",
        "fileUploadTradingAddress.no_js_page.p2"
      )
    }
  }

  "GET /poll-status/:fileType/:ref" should {
    "given fileStatus NOT_READY return Ok with FileStatus response body and not store FileStatus in session" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      given200UpscanPollStatusNotReady()

      val result = await(controller.pollStatus("amls","reference")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      bodyOf(result) shouldBe """{"reference":"reference","fileStatus":"NOT_READY"}"""

      await(sessionStoreService.fetchAgentSession.flatMap(_.flatMap(_.amlsUploadStatus))) shouldBe None

    }

    "given fileStatus READY return Ok with FileStatus response body and store FileStatus in session" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      given200UpscanPollStatusReady()

      val result = await(controller.pollStatus("amls","reference")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      val fileUploadStatus = """{"reference":"reference","fileStatus":"READY","fileName":"some"}"""

      bodyOf(result) shouldBe fileUploadStatus

      await(sessionStoreService.fetchAgentSession.flatMap(_.flatMap(_.amlsUploadStatus))) shouldBe
        Some(Json.parse(fileUploadStatus).as[FileUploadStatus])
    }
  }

  "GET /file-uploaded-successfully" should {
    "display the page with correct content" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename"))), fileType = Some("trading-address")))

      val result = await(controller.showSuccessfulUploadedForm()(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no"
      )

      result should containSubstrings("Is filename the correct file?")

    }
  }

  "POST /file-uploaded-successfully" should {
    "read the form and redirect to /registered-with-hmrc page if the user selects Yes" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address","choice.correctFile" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showRegisteredWithHmrcForm().url)
    }

    "read the form and redirect to /upload-proof-trading-address page if the user selects No" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctFile" -> "false"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showTradingAddressUploadForm().url)
    }

    "show the form with errors when invalid value for 'correctFile' is passed in the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctFile" -> "abcd"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 200
      result should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no",
        "error.boolean"
      )

      result should containSubstrings("Is filename the correct file?")
    }

    "show the form with errors when 'correctFile' field is missing the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctabxgd" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 200
      result should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no",
        "fileUpload.correctFile.no-radio.selected"
      )

      result should containSubstrings("Is filename the correct file?")
    }

    "show the form with errors when 'fileType' field has been modified by the user and contains invalid value" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "invalid", "choice.correctFile" -> "true"))

      an[RuntimeException] shouldBe thrownBy(await(controller.submitSuccessfulFileUploadedForm(request)))
    }
  }

  "GET /file-upload-failed" should {

    val tradingAddressAddressUploadStatus = FileUploadStatus("reference","READY",Some("filename"))

    "display page as expected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(tradingAddressAddressUploadStatus), fileType = Some("trading-address")))

      val request = cleanCredsAgent(FakeRequest())

      val result = await(controller.showUploadFailedPage()(request))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.failed.caption",
        "fileUpload.failed.title",
        "fileUpload.failed.p1",
        "fileUpload.failed.try-again.label"
      )

      val tradingAddressUploadFormUrl = routes.FileUploadController.showTradingAddressUploadForm().url

      val doc = Jsoup.parse(bodyOf(result))
      val tryAgainLink = doc.getElementById("file-upload-failed")
      tryAgainLink.text() shouldBe "Try again"
      tryAgainLink.attr("href") shouldBe tradingAddressUploadFormUrl

      result should containLink("button.back", tradingAddressUploadFormUrl)
    }
  }

  "GET /upload-proof-anti-money-laundering-registration" should {
    "display the upload amls form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = await(controller.showAmlsUploadForm()(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.caption",
        "fileUpload.title.amls",
        "fileUpload.p1.amls",
        "fileUpload.p2",
        "fileUpload.li.1.amls",
        "fileUpload.li.2",
        "fileUpload.li.3",
        "fileUpload.upload",
        "fileUpload.inset",
        "fileUpload.button"
      )
    }
  }

  "POST /file-uploaded-successfully" should {
    "read the form and redirect to /contact-details page if the user selects Yes" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "amls", "choice.correctFile" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showContactDetailsForm().url)
    }

    "read the form and redirect to /upload-proof-anti-money-laundering-registration page if the user selects No" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "amls", "choice.correctFile" -> "false"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showAmlsUploadForm().url)
    }
  }

  "GET /upload-proof-tax-registration" should {
    "display the upload trn form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = await(controller.showTrnUploadForm()(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.caption",
        "fileUpload.title.trn",
        "fileUpload.p1.trn",
        "fileUpload.p2",
        "fileUpload.li.1.trn",
        "fileUpload.li.2",
        "fileUpload.li.3",
        "fileUpload.upload",
        "fileUpload.inset",
        "fileUpload.button"
      )
    }
  }

  "POST /file-uploaded-successfully" should {
    "read the form and redirect to /check-your-answers page if the user selects Yes" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trn","choice.correctFile" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showCheckYourAnswers().url)
    }

    "read the form and redirect to /upload-proof-tax-registration-number page if the user selects No" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trn", "choice.correctFile" -> "false"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showTrnUploadForm().url)
    }
  }
}
