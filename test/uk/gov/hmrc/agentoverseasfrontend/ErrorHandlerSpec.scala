/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasfrontend

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.BadGatewayException
import uk.gov.hmrc.agentoverseasfrontend.support.LogCapturing

import scala.concurrent.Future

class ErrorHandlerSpec
    extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach with LogCapturing {

  val handler: ErrorHandler = app.injector.instanceOf[ErrorHandler]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("en")

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().configure("auditing.enabled" -> false)

  "ErrorHandler should show the error page with log" when {
    "a server error occurs of type BadGateway" in {
      withCaptureOfLoggingFrom(handler.logger) { logEvents =>
        val result = handler.onServerError(FakeRequest(), new BadGatewayException("some error"))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        checkIncludesMessages(result, "global.error.500.title", "global.error.500.heading", "global.error.500.message")

        logEvents.count(_.getMessage.contains(s"uk.gov.hmrc.http.BadGatewayException: some error")) mustBe 1
      }
    }
  }

  "a client error (400) occurs with log" in {
    withCaptureOfLoggingFrom(handler.logger) { logEvents =>
      val result = handler.onClientError(FakeRequest(), BAD_REQUEST, "some error")

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some(HTML)
      checkIncludesMessages(result, "global.error.400.title", "global.error.400.heading", "global.error.400.message")

      logEvents.count(_.getMessage.contains(s"onClientError some error")) mustBe 1
    }
  }

  "a client error (404) occurs with log" in {
    withCaptureOfLoggingFrom(handler.logger) { logEvents =>
      val result = handler.onClientError(FakeRequest(), NOT_FOUND, "some error")

      status(result) mustBe NOT_FOUND
      contentType(result) mustBe Some(HTML)
      checkIncludesMessages(result, "global.error.404.title", "global.error.404.heading", "global.error.404.message")

      logEvents.count(_.getMessage.contains(s"onClientError some error")) mustBe 1
    }
  }

  private def checkIncludesMessages(result: Future[Result], messageKeys: String*): Unit =
    messageKeys.foreach { messageKey =>
      messagesApi.isDefinedAt(messageKey) mustBe true
      contentAsString(result) must include(HtmlFormat.escape(messagesApi(messageKey)).toString)
    }

}
