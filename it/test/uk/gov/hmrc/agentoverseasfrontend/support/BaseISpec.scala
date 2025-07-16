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

package uk.gov.hmrc.agentoverseasfrontend.support

import org.apache.pekko.util.Timeout
import com.google.inject.AbstractModule
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.MatchResult
import org.scalatest.matchers.Matcher
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.Assertion
import org.scalatest.OptionValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.agentoverseasfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentoverseasfrontend.stubs.DataStreamStubs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.SessionCacheRepository
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.Future
import scala.concurrent.duration._

class BaseISpec
extends AnyWordSpecLike
with Matchers
with OptionValues
with ScalaFutures
with GuiceOneAppPerSuite
with WireMockSupport
with AuthStubs
with DataStreamStubs
with MetricsTestSupport
with DefaultAwaitTimeout
with MongoSupport {

  implicit val timeout = Timeout(5.seconds)

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "appName" -> "agent-overseas-frontend",
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.upscan.port" -> wireMockPort,
      "microservice.services.companyAuthSignInUrl" -> "/baseISpec/gg/sign-in",
      "microservice.services.guidancePageApplicationUrl" -> "guidancePageUrl",
      "government-gateway-registration-frontend.sosRedirect-path" -> "http://localhost:8571/government-gateway-registration-frontend?accountType=agent&origin=unknown",
      "agent-services-account.root-path" -> "http://localhost:9401/agent-services-account",
      "microservice.services.agent-overseas-application.host" -> wireMockHost,
      "microservice.services.agent-overseas-application.port" -> wireMockPort,
      "microservice.services.agent-subscription.host" -> wireMockHost,
      "microservice.services.agent-subscription.port" -> wireMockPort,
      "cachable.session-cache.port" -> wireMockPort,
      "cachable.session-cache.domain" -> "keystore",
      "maintainer-application-review-days" -> 28,
      "feedback-survey-url" -> "http://localhost:9514/feedback/OVERSEAS_AGENTS",
      "metrics.enabled" -> true,
      "auditing.enabled" -> true,
      "auditing.consumer.baseUri.host" -> wireMockHost,
      "auditing.consumer.baseUri.port" -> wireMockPort,
      "mongodb.uri" -> mongoUri
    )
    .overrides(new TestGuiceModule)

  override def commonStubs(): Unit = {
    givenCleanMetricRegistry()
    givenAuditConnector()
    ()
  }

  protected lazy val sessionCacheService = new TestSessionCacheService

  private class TestGuiceModule
  extends AbstractModule {
    override def configure(): Unit = bind(classOf[SessionCacheService]).toInstance(sessionCacheService)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sessionCacheService.clear()
    ()
  }

  protected implicit val materializer = app.materializer

  private def contentType(result: Result): Option[String] = result.body.contentType.map(_.split(";").take(1).mkString.trim)

  private def charset(result: Result): Option[String] =
    result.body.contentType match {
      case Some(s) if s.contains("charset=") => Some(s.split("; *charset=").drop(1).mkString.trim)
      case _ => None
    }

  private def bodyOf(result: Result): String = contentAsString(Future.successful(result))

  protected def checkHtmlResultWithBodyText(
    result: Result,
    expectedSubstring: String
  ): Assertion = {
    result.header.status shouldBe 200
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString
  protected def htmlEscapedMessage(
    key: String,
    args: Any*
  ): String = HtmlFormat.escape(Messages(key, args: _*)).toString
  protected def htmlMessage(
    key: String,
    args: Any*
  ): String = Messages(key, args: _*).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

  protected def checkMessageIsDefined(messageKey: String) =
    withClue(s"Message key ($messageKey) should be defined: ") {
      Messages.isDefinedAt(messageKey) shouldBe true
    }

  protected def checkIsHtml200(result: Result) = {
    result.header.status shouldBe 200
    charset(result) shouldBe Some("utf-8")
    contentType(result) shouldBe Some("text/html")
  }

  protected def containSubstrings(expectedSubstrings: String*): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        checkIsHtml200(result)

        val resultBody = bodyOf(result)
        val (strsPresent, strsMissing) = expectedSubstrings.partition { expectedSubstring =>
          expectedSubstring.trim should not be ""
          resultBody.contains(expectedSubstring)
        }

        MatchResult(
          strsMissing.isEmpty,
          s"Expected substrings are missing in the response: ${strsMissing.mkString(
              "\"",
              "\", \"",
              "\""
            )}",
          s"Expected substrings are present in the response : ${strsPresent.mkString(
              "\"",
              "\", \"",
              "\""
            )}"
        )
      }
    }

  protected def containMessages(expectedMessageKeys: String*): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        expectedMessageKeys.foreach(checkMessageIsDefined)
        checkIsHtml200(result)
        val resultBody = bodyOf(result)
        val (msgsPresent, msgsMissing) = expectedMessageKeys.partition { messageKey =>
          resultBody.contains(htmlEscapedMessage(messageKey))
        }
        MatchResult(
          msgsMissing.isEmpty,
          s"Content is missing in the response for message keys: ${msgsMissing.mkString(", ")}",
          s"Content is present in the response for message keys: ${msgsPresent.mkString(", ")}"
        )
      }
    }

  protected def containElement(
    id: String,
    tag: String,
    attrs: Map[String, String]
  ): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))
        val foundElement = doc.getElementById(id)
        val isAsExpected =
          Option(foundElement) match {
            case None => false
            case Some(elFound) =>
              val isExpectedTag = elFound.tagName() == tag
              val hasExpectedAttrs = attrs.forall { case (expectedAttr, expectedValue) => elFound.attr(expectedAttr) == expectedValue }
              isExpectedTag && hasExpectedAttrs
          }

        MatchResult(
          isAsExpected,
          s"""Response does not contain a "$tag" element with id of "$id" with matching attributes $attrs""",
          s"""Response contains a "$tag" element with id of "$id" with matching attributes $attrs"""
        )
      }
    }

  protected def containSubmitButton(
    expectedMessageKey: String,
    expectedElementId: String,
    expectedTagName: String = "button",
    expectedType: String = "submit"
  ): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))
        checkMessageIsDefined(expectedMessageKey)
        val foundElement = doc.getElementById(expectedElementId)
        val isAsExpected =
          Option(foundElement) match {
            case None => false
            case Some(element) =>
              val isExpectedTag = element.tagName() == expectedTagName
              val isExpectedType = element.attr("type") == expectedType
              val hasExpectedMsg = element.text() == htmlEscapedMessage(expectedMessageKey)
              isExpectedTag && isExpectedType && hasExpectedMsg
          }
        MatchResult(
          isAsExpected,
          s"""Response does not contain a submit button with id "$expectedElementId" and type "$expectedType" with content for message key "$expectedMessageKey" """,
          s"""Response contains a submit button with id "$expectedElementId" and type "$expectedType" with content for message key "$expectedMessageKey" """
        )
      }
    }

  protected def containLink(
    expectedMessageKey: String,
    expectedHref: String
  ): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))
        checkMessageIsDefined(expectedMessageKey)
        val foundElement = doc.select(s"a[href=$expectedHref]").first()
        val wasFoundWithCorrectMessage =
          Option(foundElement) match {
            case None => false
            case Some(element) => element.text() == htmlEscapedMessage(expectedMessageKey)
          }
        MatchResult(
          wasFoundWithCorrectMessage,
          s"""Response does not contain a link to "$expectedHref" with content for message key "$expectedMessageKey" """,
          s"""Response contains a link to "$expectedHref" with content for message key "$expectedMessageKey" """
        )
      }
    }

  protected def repeatMessage(
    expectedMessageKey: String,
    times: Int
  ): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        checkIsHtml200(result)
        MatchResult(
          Messages(expectedMessageKey).r.findAllMatchIn(bodyOf(result)).size == times,
          s"The message keys $expectedMessageKey does not appear $times times in the content",
          s"The message keys $expectedMessageKey appears $times times in the content"
        )
      }
    }

}
