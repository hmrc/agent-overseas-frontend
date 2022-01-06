/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.agentoverseasfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.ApplicationAuth
import uk.gov.hmrc.agentoverseasfrontend.forms.MainBusinessAddressForm
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, MongoDBSessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasfrontend.views.html.application.main_business_address

import scala.concurrent.ExecutionContext

@Singleton
class TradingAddressController @Inject()(
  val env: Environment,
  sessionStoreService: MongoDBSessionStoreService,
  applicationService: ApplicationService,
  val upscanConnector: UpscanConnector,
  countryNamesLoader: CountryNamesLoader,
  authAction: ApplicationAuth,
  cc: MessagesControllerComponents,
  mainBusinessAddressView: main_business_address)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with SessionBehaviour
    with I18nSupport {

  import authAction.withEnrollingAgent

  private val countries = countryNamesLoader.load
  private val validCountryCodes = countries.keys.toSet

  def showMainBusinessAddressForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form =
        MainBusinessAddressForm.mainBusinessAddressForm(validCountryCodes)
      if (agentSession.changingAnswers) {
        Ok(
          mainBusinessAddressView(
            agentSession.overseasAddress.fold(form)(form.fill),
            countries,
            Some(showCheckYourAnswersUrl)))
      } else {
        Ok(mainBusinessAddressView(agentSession.overseasAddress.fold(form)(form.fill), countries))
      }
    }
  }

  def submitMainBusinessAddress: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      MainBusinessAddressForm
        .mainBusinessAddressForm(validCountryCodes)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (agentSession.changingAnswers) {
              Ok(mainBusinessAddressView(formWithErrors, countries, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(mainBusinessAddressView(formWithErrors, countries))
            }
          },
          validForm =>
            updateSession(agentSession.copy(overseasAddress = Some(validForm)))(
              routes.FileUploadController.showTradingAddressUploadForm().url)
        )
    }
  }
}
