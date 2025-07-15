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

import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec

import scala.concurrent.Future

class ChangingAnswersControllerISpec
extends BaseISpec {

  private val agentSession = AgentSession()

  private lazy val controller: ChangingAnswersController = app.injector.instanceOf[ChangingAnswersController]

  class SetUp {

    implicit val request = cleanCredsAgent(FakeRequest())

    sessionStoreService.cacheAgentSession(agentSession).futureValue

  }

  "GET /change-amls-details" should {

    "update session with changingAnswers=true and redirect to money-laundering form" in new SetUp {

      val result = controller.changeAmlsDetails(request)
      verify(result, routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm.url)
    }
  }

  "GET /change-contact-details" should {

    "update session with changingAnswers=true and redirect to contact-details form" in new SetUp {

      val result = controller.changeContactDetails(request)

      verify(result, routes.ApplicationController.showContactDetailsForm.url)
    }
  }

  "GET /change-trading-name" should {

    "update session with changingAnswers=true and redirect to trading-name form" in new SetUp {

      val result = controller.changeTradingName(request)

      verify(result, routes.ApplicationController.showTradingNameForm.url)
    }
  }

  "GET /change-trading-address" should {

    "update session with changingAnswers=true and redirect to trading-address form" in new SetUp {

      val result = controller.changeTradingAddress(request)

      verify(result, routes.TradingAddressController.showMainBusinessAddressForm.url)
    }
  }

  "GET /change-trading-address-file" should {

    "update session with changingAnswers=true and redirect to trading-address-file-upload form" in new SetUp {

      val result = controller.changeTradingAddressFile(request)

      verify(result, routes.FileUploadController.showTradingAddressUploadForm.url)
    }
  }

  "GET /change-registered-with-hmrc" should {

    "update session with changingAnswers=true and redirect to registered-with-hmrc form" in new SetUp {

      val result = controller.changeRegisteredWithHmrc(request)

      verify(result, routes.ApplicationController.showRegisteredWithHmrcForm.url)
    }
  }

  "GET /change-agent-codes" should {

    "update session with changingAnswers=true and redirect to agent-codes form" in new SetUp {

      val result = controller.changeAgentCodes(request)

      verify(result, routes.ApplicationController.showAgentCodesForm.url)
    }
  }

  "GET /change-registered-with-uk-tax" should {

    "update session with changingAnswers=true and redirect to uk-tax-registration form" in new SetUp {

      val result = controller.changeRegisteredForUKTax(request)

      verify(result, routes.ApplicationController.showUkTaxRegistrationForm.url)
    }
  }

  "GET /change-personal-details" should {

    "update session with changingAnswers=true and redirect to personal-details form" in new SetUp {

      val result = controller.changePersonalDetails(request)

      verify(result, routes.ApplicationController.showPersonalDetailsForm.url)
    }
  }

  "GET /change-company-reg-number" should {

    "update session with changingAnswers=true and redirect to company-reg-number form" in new SetUp {
      val result = controller.changeCompanyRegistrationNumber(request)

      verify(result, routes.ApplicationController.showCompanyRegistrationNumberForm.url)
    }
  }

  "GET /change-your-tax-reg-numbers" should {

    "update session with changingAnswers=true and redirect to your-tax-registration-numbers form" in new SetUp {
      val result = controller.changeYourTaxRegistrationNumbers(request)

      verify(result, routes.TaxRegController.showYourTaxRegNumbersForm.url)
    }
  }

  private def verify(
    result: Future[Result],
    url: String
  )(implicit request: Request[AnyContent]) = {
    status(result) shouldBe 303
    header(LOCATION, result).get shouldBe url
    sessionStoreService.fetchAgentSession.futureValue.get.changingAnswers shouldBe true
  }

}
