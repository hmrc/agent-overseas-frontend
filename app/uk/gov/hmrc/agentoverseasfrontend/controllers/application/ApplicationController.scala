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
import uk.gov.hmrc.agentoverseasfrontend.config.view.CheckYourAnswers
import uk.gov.hmrc.agentoverseasfrontend.config.{AppConfig, CountryNamesLoader}
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.ApplicationAuth
import uk.gov.hmrc.agentoverseasfrontend.forms.YesNoRadioButtonForms.{registeredForUkTaxForm, registeredWithHmrcForm}
import uk.gov.hmrc.agentoverseasfrontend.forms._
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession.{IsRegisteredForUkTax, IsRegisteredWithHmrc}
import uk.gov.hmrc.agentoverseasfrontend.models.{AgentSession, No, Yes, _}
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasfrontend.views.html.application._

import scala.concurrent.ExecutionContext

@Singleton
class ApplicationController @Inject()(
  val env: Environment,
  authAction: ApplicationAuth,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  countryNamesLoader: CountryNamesLoader,
  cc: MessagesControllerComponents,
  contactDetailsView: contact_details,
  tradingNameView: trading_name,
  registeredWithHmrcView: registered_with_hmrc,
  saAgentCodeView: self_assessment_agent_code,
  ukTaxRegView: uk_tax_registration,
  personalDetailsView: personal_details,
  crnView: company_registration_number,
  cyaView: check_your_answers,
  applicationCompleteView: application_complete)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with SessionBehaviour
    with I18nSupport {

  import authAction.{withBasicAuthAndAgentAffinity, withEnrollingAgent}

  private val countries = countryNamesLoader.load

  def showContactDetailsForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = ContactDetailsForm.form
      if (agentSession.changingAnswers) {
        Ok(contactDetailsView(agentSession.contactDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        val backLink =
          if (agentSession.amlsRequired.getOrElse(false))
            routes.FileUploadController.showSuccessfulUploadedForm()
          else
            routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired()
        Ok(contactDetailsView(agentSession.contactDetails.fold(form)(form.fill), Some(backLink.url)))
      }
    }
  }

  def submitContactDetails: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      ContactDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (agentSession.changingAnswers) {
              Ok(contactDetailsView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(contactDetailsView(formWithErrors))
            }
          },
          validForm => {
            updateSession(agentSession.copy(contactDetails = Some(validForm)))(
              routes.ApplicationController.showTradingNameForm().url)
          }
        )
    }
  }

  def showTradingNameForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = TradingNameForm.form
      if (agentSession.changingAnswers) {
        Ok(tradingNameView(agentSession.tradingName.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(tradingNameView(agentSession.tradingName.fold(form)(form.fill)))
      }
    }
  }

  def submitTradingName: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      TradingNameForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (agentSession.changingAnswers) {
              Ok(tradingNameView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(tradingNameView(formWithErrors))
            }
          },
          validForm =>
            updateSession(agentSession.copy(tradingName = Some(validForm)))(
              routes.TradingAddressController.showMainBusinessAddressForm().url)
        )
    }
  }

  def showRegisteredWithHmrcForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = registeredWithHmrcForm
      if (agentSession.changingAnswers) {
        Ok(
          registeredWithHmrcView(
            agentSession.registeredWithHmrc.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
            Some(showCheckYourAnswersUrl)))
      } else {
        Ok(registeredWithHmrcView(agentSession.registeredWithHmrc.fold(form)(x => form.fill(YesNo.toRadioConfirm(x)))))
      }
    }
  }

  def submitRegisteredWithHmrc: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      registeredWithHmrcForm
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(registeredWithHmrcView(formWithErrors)),
          validFormValue => {
            val newValue = YesNo(validFormValue)
            val redirectTo =
              if (Yes == newValue)
                routes.ApplicationController.showAgentCodesForm().url
              else routes.ApplicationController.showUkTaxRegistrationForm().url
            val toUpdate =
              agentSession.copy(registeredWithHmrc = Some(newValue))
            if (agentSession.changingAnswers) {
              agentSession.registeredWithHmrc match {
                case Some(oldValue) =>
                  if (oldValue == newValue) {
                    updateSession(agentSession.copy(changingAnswers = false))(showCheckYourAnswersUrl)
                  } else {
                    updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
                  }
                case None =>
                  updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
              }
            } else {
              updateSession(toUpdate)(redirectTo)
            }
          }
        )
    }
  }

  def showAgentCodesForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = AgentCodesForm.form

      if (agentSession.changingAnswers) {
        Ok(saAgentCodeView(agentSession.agentCodes.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(saAgentCodeView(agentSession.agentCodes.fold(form)(form.fill)))
      }
    }
  }

  def submitAgentCodes: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      AgentCodesForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (agentSession.changingAnswers) {
              Ok(saAgentCodeView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(saAgentCodeView(formWithErrors))
            }
          },
          validFormValue => {
            updateSession(agentSession.copy(agentCodes = Some(validFormValue), changingAnswers = false))(
              routes.ApplicationController.showUkTaxRegistrationForm().url)
          }
        )
    }
  }

  def showUkTaxRegistrationForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = registeredForUkTaxForm
      if (agentSession.changingAnswers) {
        Ok(
          ukTaxRegView(
            agentSession.registeredForUkTax.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
            showCheckYourAnswersUrl))
      } else {
        Ok(
          ukTaxRegView(
            agentSession.registeredForUkTax.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
            ukTaxRegistrationBackLink(agentSession).url))
      }
    }
  }

  def submitUkTaxRegistration: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      registeredForUkTaxForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (agentSession.changingAnswers) {
              Ok(ukTaxRegView(formWithErrors, showCheckYourAnswersUrl))
            } else {
              Ok(ukTaxRegView(formWithErrors, ukTaxRegistrationBackLink(agentSession).url))
            }
          },
          validFormValue => {
            val newValue = YesNo(validFormValue)
            val redirectTo =
              if (Yes == newValue)
                routes.ApplicationController.showPersonalDetailsForm().url
              else
                routes.ApplicationController
                  .showCompanyRegistrationNumberForm()
                  .url
            val toUpdate =
              agentSession.copy(registeredForUkTax = Some(newValue))

            if (agentSession.changingAnswers) {
              agentSession.registeredForUkTax match {
                case Some(oldValue) =>
                  if (oldValue == newValue) {
                    updateSession(agentSession.copy(changingAnswers = false))(
                      routes.ApplicationController.showCheckYourAnswers().url)
                  } else {
                    updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
                  }
                case None =>
                  updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
              }
            } else {
              updateSession(toUpdate)(redirectTo)
            }
          }
        )
    }
  }

  def showPersonalDetailsForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = PersonalDetailsForm.form
      if (agentSession.changingAnswers) {
        Ok(personalDetailsView(agentSession.personalDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(personalDetailsView(agentSession.personalDetails.fold(form)(form.fill)))
      }
    }
  }

  def submitPersonalDetails: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      PersonalDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (agentSession.changingAnswers) {
              Ok(personalDetailsView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(personalDetailsView(formWithErrors))
            }
          },
          validForm => {
            updateSession(agentSession.copy(personalDetails = Some(validForm)))(
              routes.ApplicationController
                .showCompanyRegistrationNumberForm()
                .url)
          }
        )
    }
  }

  def showCompanyRegistrationNumberForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = CompanyRegistrationNumberForm.form
      if (agentSession.changingAnswers) {
        Ok(crnView(agentSession.companyRegistrationNumber.fold(form)(form.fill), showCheckYourAnswersUrl))
      } else {
        Ok(
          crnView(agentSession.companyRegistrationNumber.fold(form)(form.fill), companyRegNumberBackLink(agentSession)))
      }
    }
  }

  def submitCompanyRegistrationNumber: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      CompanyRegistrationNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (agentSession.changingAnswers) {
              Ok(crnView(formWithErrors, showCheckYourAnswersUrl))
            } else {
              Ok(crnView(formWithErrors, companyRegNumberBackLink(agentSession)))
            }
          },
          validFormValue => {
            updateSession(agentSession.copy(companyRegistrationNumber = Some(validFormValue)))(
              routes.TaxRegController.showTaxRegistrationNumberForm().url)
          }
        )
    }
  }

  private def getCountryName(agentSession: AgentSession): String = {
    val countryCode = agentSession.overseasAddress.map(_.countryCode)
    countryCode
      .flatMap(countries.get)
      .getOrElse(sys.error(s"No country found for code: '${countryCode.getOrElse("")}'"))
  }

  def showCheckYourAnswers: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      //make sure user has gone through all the required pages, if not redirect to appropriate page
      sessionStoreService.fetchAgentSession
        .map(lookupNextPage)
        .map { call =>
          if (call == routes.ApplicationController
                .showCheckYourAnswers() || call == routes.TaxRegController
                .showYourTaxRegNumbersForm()) {

            sessionStoreService.cacheAgentSession(agentSession.copy(changingAnswers = false))
            Ok(cyaView(CheckYourAnswers.form, CheckYourAnswers(agentSession, getCountryName(agentSession))))
          } else Redirect(call)
        }
    }
  }

  def submitCheckYourAnswers: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      CheckYourAnswers.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            BadRequest(
              cyaView(formWithErrors, CheckYourAnswers(agentSession, getCountryName(agentSession)))
            )
          },
          cyaConfirmation => {
            for {
              _ <- applicationService.createApplication(agentSession)
              _ <- sessionStoreService.removeAgentSession
            } yield
              Redirect(routes.ApplicationController.showApplicationComplete())
                .flashing(
                  "tradingName" -> agentSession.tradingName
                    .getOrElse(""),
                  "contactDetail" -> agentSession.contactDetails
                    .fold("")(_.businessEmail))
          }
        )

    }
  }

  def showApplicationComplete: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuthAndAgentAffinity { cRequest =>
      val tradingName = request.flash.get("tradingName")
      val contactDetail = request.flash.get("contactDetail")

      if (tradingName.isDefined && contactDetail.isDefined)
        Ok(applicationCompleteView(tradingName.get, contactDetail.get, appConfig.guidancePageApplicationUrl))
      else
        Redirect(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm())
    }
  }

  private def ukTaxRegistrationBackLink(session: AgentSession) =
    Some(session) match {
      case IsRegisteredWithHmrc(Yes) =>
        routes.ApplicationController.showAgentCodesForm()
      case IsRegisteredWithHmrc(No) =>
        routes.ApplicationController.showRegisteredWithHmrcForm()
    }

  private def companyRegNumberBackLink(session: AgentSession) =
    Some(session) match {
      case IsRegisteredForUkTax(Yes) =>
        routes.ApplicationController.showPersonalDetailsForm().url
      case IsRegisteredForUkTax(No) =>
        routes.ApplicationController.showUkTaxRegistrationForm().url
    }
}
