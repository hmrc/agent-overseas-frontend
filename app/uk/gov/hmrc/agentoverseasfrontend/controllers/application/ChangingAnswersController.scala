/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Environment
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentoverseasfrontend.config.{AppConfig, CountryNamesLoader}
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.AuthAction
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService}

import scala.concurrent.ExecutionContext

@Singleton
class ChangingAnswersController @Inject()(
  val env: Environment,
  authAction: AuthAction,
  override val sessionStoreService: SessionStoreService,
  override val applicationService: ApplicationService,
  countryNamesLoader: CountryNamesLoader,
  cc: MessagesControllerComponents)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with SessionBehaviour
    with I18nSupport {

  import authAction._

  def changeAmlsRequired: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url
      )
    }
  }

  def changeAmlsDetails: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)
    }
  }

  def changeAmlsFile: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.FileUploadController.showAmlsUploadForm().url)
    }
  }

  def changeContactDetails: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.ApplicationController.showContactDetailsForm().url)
    }
  }

  def changeTradingName: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.ApplicationController.showTradingNameForm().url)
    }
  }

  def changeTradingAddress: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.TradingAddressController.showMainBusinessAddressForm().url)
    }
  }

  def changeTradingAddressFile: Action[AnyContent] =
    Action.async { implicit request =>
      withEnrollingAgent { cRequest =>
        updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
          routes.FileUploadController.showTradingAddressUploadForm().url)
      }
    }

  def changeRegisteredWithHmrc: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.ApplicationController.showRegisteredWithHmrcForm().url)
    }
  }

  def changeAgentCodes: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.ApplicationController.showAgentCodesForm().url)
    }
  }

  def changeRegisteredForUKTax: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.ApplicationController.showUkTaxRegistrationForm().url)
    }
  }

  def changePersonalDetails: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.ApplicationController.showPersonalDetailsForm().url)
    }
  }

  def changeCompanyRegistrationNumber: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.ApplicationController.showCompanyRegistrationNumberForm().url)
    }
  }

  def changeYourTaxRegistrationNumbers: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.TaxRegController.showYourTaxRegNumbersForm().url)
    }
  }

  def changeYourTaxRegistrationNumbersFile: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      updateSessionAndRedirect(cRequest.agentSession.copy(changingAnswers = true))(
        routes.FileUploadController.showTrnUploadForm().url)
    }
  }
}