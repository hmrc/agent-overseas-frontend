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
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.models.FileUploadStatus
import uk.gov.hmrc.agentoverseasfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasfrontend.stubs.UpscanStubs
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

class FileUploadControllerISpec
extends BaseISpec
with AgentOverseasApplicationStubs
with UpscanStubs {

  private lazy val controller: FileUploadController = app.injector.instanceOf[FileUploadController]

  private val agentSession = AgentSession()

  "GET /upload-proof-trading-address" should {
    "display the upload trading address form" in {
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = controller.showTradingAddressUploadForm()(request)

      status(result) shouldBe 200

      result.futureValue should containMessages(
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
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = controller.showTradingAddressNoJsCheckPage(request)

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "fileUploadTradingAddress.no_js_page.caption",
        "fileUploadTradingAddress.no_js_page.title",
        "fileUploadTradingAddress.no_js_page.p1",
        "fileUploadTradingAddress.no_js_page.p2"
      )
    }
  }

  "GET /poll-status/:fileType/:ref" should {
    "given fileStatus NOT_READY return Ok with FileStatus response body and not store FileStatus in session" in {
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      given200UpscanPollStatusNotReady()

      val result = controller.pollStatus("amls", "reference")(request)

      status(result) shouldBe 200

      contentAsString(result) shouldBe """{"reference":"reference","fileStatus":"NOT_READY"}"""

      sessionStoreService.fetchAgentSession.futureValue.flatMap(_.amlsUploadStatus) shouldBe None

    }

    "given fileStatus READY return Ok with FileStatus response body and store FileStatus in session" in {
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      given200UpscanPollStatusReady()

      val result = controller.pollStatus("amls", "reference")(request)

      status(result) shouldBe 200

      val fileUploadStatus = """{"reference":"reference","fileStatus":"READY","fileName":"some"}"""

      contentAsString(result) shouldBe fileUploadStatus

      sessionStoreService.fetchAgentSession.futureValue.flatMap(_.amlsUploadStatus) shouldBe
        Some(Json.parse(fileUploadStatus).as[FileUploadStatus])
    }
  }

  "GET /file-uploaded-successfully" should {
    "display the page with correct content" in {
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          tradingAddressUploadStatus = Some(FileUploadStatus(
            "reference",
            "READY",
            Some("filename")
          )),
          fileType = Some("trading-address")
        )
      )

      val result = controller.showSuccessfulUploadedForm()(request)

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no"
      )

      result.futureValue should containSubstrings("Is filename the correct file?")

    }
  }

  "POST /file-uploaded-successfully" should {
    "read the form and redirect to /registered-with-hmrc page if the user selects Yes" in {
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctFile" -> "true")
      )

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showRegisteredWithHmrcForm.url)
    }

    "read the form and redirect to /upload-proof-trading-address page if the user selects No" in {
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctFile" -> "false")
      )

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showTradingAddressUploadForm.url)
    }

    "show the form with errors when invalid value for 'correctFile' is passed in the form" in {
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctFile" -> "abcd")
      )

      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(tradingAddressUploadStatus =
          Some(FileUploadStatus(
            "reference",
            "READY",
            Some("filename")
          ))
        )
      )

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no",
        "error.boolean"
      )

      result.futureValue should containSubstrings("Is filename the correct file?")
    }

    "show the form with errors when 'correctFile' field is missing the form" in {
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctabxgd" -> "true")
      )

      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(tradingAddressUploadStatus =
          Some(FileUploadStatus(
            "reference",
            "READY",
            Some("filename")
          ))
        )
      )

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 200
      result.futureValue should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no",
        "fileUpload.correctFile.no-radio.selected"
      )

      result.futureValue should containSubstrings("Is filename the correct file?")
    }

    "show the form with errors when 'fileType' field has been modified by the user and contains invalid value" in {
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "invalid", "choice.correctFile" -> "true")
      )

      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(tradingAddressUploadStatus =
          Some(FileUploadStatus(
            "reference",
            "READY",
            Some("filename")
          ))
        )
      )

      val e = controller.submitSuccessfulFileUploadedForm(request).failed.futureValue
      e shouldBe a[RuntimeException]
    }
  }

  "GET /file-upload-failed" should {

    val tradingAddressAddressUploadStatus = FileUploadStatus(
      "reference",
      "READY",
      Some("filename")
    )

    "display page as expected" in {
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          tradingAddressUploadStatus = Some(tradingAddressAddressUploadStatus),
          fileType = Some("trading-address")
        )
      )

      val result = controller.showUploadFailedPage()(request)

      status(result) shouldBe 200

      result.futureValue should containMessages(
        "fileUpload.failed.caption",
        "fileUpload.failed.title",
        "fileUpload.failed.p1",
        "fileUpload.failed.try-again.label"
      )

      val tradingAddressUploadFormUrl = routes.FileUploadController.showTradingAddressUploadForm.url

      val doc = Jsoup.parse(contentAsString(result))
      val tryAgainLink = doc.getElementById("file-upload-failed")
      tryAgainLink.text() shouldBe "Try again"
      tryAgainLink.attr("href") shouldBe tradingAddressUploadFormUrl

      result.futureValue should containLink("button.back", tradingAddressUploadFormUrl)
    }
  }

  "GET /upload-proof-anti-money-laundering-registration" should {
    "display the upload amls form" in {
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = controller.showAmlsUploadForm()(request)

      status(result) shouldBe 200

      result.futureValue should containMessages(
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
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "amls", "choice.correctFile" -> "true")
      )

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showContactDetailsForm.url)
    }

    "read the form and redirect to /upload-proof-anti-money-laundering-registration page if the user selects No" in {
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "amls", "choice.correctFile" -> "false")
      )

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showAmlsUploadForm.url)
    }
  }

  "GET /upload-proof-tax-registration" should {
    "display the upload trn form" in {
      implicit val request = cleanCredsAgent(FakeRequest())

      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = controller.showTrnUploadForm()(request)

      status(result) shouldBe 200

      result.futureValue should containMessages(
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
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "trn", "choice.correctFile" -> "true")
      )

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showCheckYourAnswers.url)
    }

    "read the form and redirect to /upload-proof-tax-registration-number page if the user selects No" in {
      implicit val request = cleanCredsAgent(
        FakeRequest(POST, "/").withFormUrlEncodedBody("fileType" -> "trn", "choice.correctFile" -> "false")
      )

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = controller.submitSuccessfulFileUploadedForm(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showTrnUploadForm.url)
    }
  }

}
