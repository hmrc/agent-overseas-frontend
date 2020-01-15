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
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.{ApplicationAuth, AuthBase}
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
    withEnrollingAgent { cRequest =>
      val form = ContactDetailsForm.form
      if (cRequest.agentSession.changingAnswers) {
        Ok(
          contactDetailsView(cRequest.agentSession.contactDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        val backLink =
          if (cRequest.agentSession.amlsRequired.getOrElse(false))
            routes.FileUploadController.showSuccessfulUploadedForm()
          else
            routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired()
        Ok(contactDetailsView(cRequest.agentSession.contactDetails.fold(form)(form.fill), Some(backLink.url)))
      }
    }
  }

  def submitContactDetails: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      ContactDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (cRequest.agentSession.changingAnswers) {
              Ok(contactDetailsView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(contactDetailsView(formWithErrors))
            }
          },
          validForm => {
            updateSession(cRequest.agentSession.copy(contactDetails = Some(validForm)))(
              routes.ApplicationController.showTradingNameForm().url)
          }
        )
    }
  }

  def showTradingNameForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      val form = TradingNameForm.form
      if (cRequest.agentSession.changingAnswers) {
        Ok(tradingNameView(cRequest.agentSession.tradingName.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(tradingNameView(cRequest.agentSession.tradingName.fold(form)(form.fill)))
      }
    }
  }

  def submitTradingName: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      TradingNameForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (cRequest.agentSession.changingAnswers) {
              Ok(tradingNameView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(tradingNameView(formWithErrors))
            }
          },
          validForm =>
            updateSession(cRequest.agentSession.copy(tradingName = Some(validForm)))(
              routes.TradingAddressController.showMainBusinessAddressForm().url)
        )
    }
  }

  def showRegisteredWithHmrcForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      val form = registeredWithHmrcForm
      if (cRequest.agentSession.changingAnswers) {
        Ok(
          registeredWithHmrcView(
            cRequest.agentSession.registeredWithHmrc.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
            Some(showCheckYourAnswersUrl)))
      } else {
        Ok(registeredWithHmrcView(cRequest.agentSession.registeredWithHmrc.fold(form)(x =>
          form.fill(YesNo.toRadioConfirm(x)))))
      }
    }
  }

  def submitRegisteredWithHmrc: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
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
              cRequest.agentSession.copy(registeredWithHmrc = Some(newValue))
            if (cRequest.agentSession.changingAnswers) {
              cRequest.agentSession.registeredWithHmrc match {
                case Some(oldValue) =>
                  if (oldValue == newValue) {
                    updateSession(cRequest.agentSession.copy(changingAnswers = false))(showCheckYourAnswersUrl)
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
    withEnrollingAgent { cRequest =>
      val form = AgentCodesForm.form

      if (cRequest.agentSession.changingAnswers) {
        Ok(saAgentCodeView(cRequest.agentSession.agentCodes.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(saAgentCodeView(cRequest.agentSession.agentCodes.fold(form)(form.fill)))
      }
    }
  }

  def submitAgentCodes: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      AgentCodesForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (cRequest.agentSession.changingAnswers) {
              Ok(saAgentCodeView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(saAgentCodeView(formWithErrors))
            }
          },
          validFormValue => {
            updateSession(cRequest.agentSession.copy(agentCodes = Some(validFormValue), changingAnswers = false))(
              routes.ApplicationController.showUkTaxRegistrationForm().url)
          }
        )
    }
  }

  def showUkTaxRegistrationForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      val form = registeredForUkTaxForm
      if (cRequest.agentSession.changingAnswers) {
        Ok(
          ukTaxRegView(
            cRequest.agentSession.registeredForUkTax.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
            showCheckYourAnswersUrl))
      } else {
        Ok(
          ukTaxRegView(
            cRequest.agentSession.registeredForUkTax.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
            ukTaxRegistrationBackLink(cRequest.agentSession).url))
      }
    }
  }

  def submitUkTaxRegistration: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      registeredForUkTaxForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (cRequest.agentSession.changingAnswers) {
              Ok(ukTaxRegView(formWithErrors, showCheckYourAnswersUrl))
            } else {
              Ok(ukTaxRegView(formWithErrors, ukTaxRegistrationBackLink(cRequest.agentSession).url))
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
              cRequest.agentSession.copy(registeredForUkTax = Some(newValue))

            if (cRequest.agentSession.changingAnswers) {
              cRequest.agentSession.registeredForUkTax match {
                case Some(oldValue) =>
                  if (oldValue == newValue) {
                    updateSession(cRequest.agentSession.copy(changingAnswers = false))(
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
    withEnrollingAgent { cRequest =>
      val form = PersonalDetailsForm.form
      if (cRequest.agentSession.changingAnswers) {
        Ok(
          personalDetailsView(
            cRequest.agentSession.personalDetails.fold(form)(form.fill),
            Some(showCheckYourAnswersUrl)))
      } else {
        Ok(personalDetailsView(cRequest.agentSession.personalDetails.fold(form)(form.fill)))
      }
    }
  }

  def submitPersonalDetails: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      PersonalDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (cRequest.agentSession.changingAnswers) {
              Ok(personalDetailsView(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(personalDetailsView(formWithErrors))
            }
          },
          validForm => {
            updateSession(cRequest.agentSession.copy(personalDetails = Some(validForm)))(
              routes.ApplicationController
                .showCompanyRegistrationNumberForm()
                .url)
          }
        )
    }
  }

  def showCompanyRegistrationNumberForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      val form = CompanyRegistrationNumberForm.form
      if (cRequest.agentSession.changingAnswers) {
        Ok(crnView(cRequest.agentSession.companyRegistrationNumber.fold(form)(form.fill), showCheckYourAnswersUrl))
      } else {
        Ok(
          crnView(
            cRequest.agentSession.companyRegistrationNumber.fold(form)(form.fill),
            companyRegNumberBackLink(cRequest.agentSession)))
      }
    }
  }

  def submitCompanyRegistrationNumber: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      CompanyRegistrationNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (cRequest.agentSession.changingAnswers) {
              Ok(crnView(formWithErrors, showCheckYourAnswersUrl))
            } else {
              Ok(crnView(formWithErrors, companyRegNumberBackLink(cRequest.agentSession)))
            }
          },
          validFormValue => {
            updateSession(cRequest.agentSession.copy(companyRegistrationNumber = Some(validFormValue)))(
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
    withEnrollingAgent { cRequest =>
      //make sure user has gone through all the required pages, if not redirect to appropriate page
      sessionStoreService.fetchAgentSession
        .map(lookupNextPage)
        .map { call =>
          if (call == routes.ApplicationController
                .showCheckYourAnswers() || call == routes.TaxRegController
                .showYourTaxRegNumbersForm()) {

            sessionStoreService.cacheAgentSession(cRequest.agentSession.copy(changingAnswers = false))
            Ok(
              cyaView(
                CheckYourAnswers.form,
                CheckYourAnswers(cRequest.agentSession, getCountryName(cRequest.agentSession))))
          } else Redirect(call)
        }
    }
  }

  def submitCheckYourAnswers: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { cRequest =>
      CheckYourAnswers.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            BadRequest(
              cyaView(formWithErrors, CheckYourAnswers(cRequest.agentSession, getCountryName(cRequest.agentSession)))
            )
          },
          cyaConfirmation => {
            for {
              _ <- applicationService.createApplication(cRequest.agentSession)
              _ <- sessionStoreService.removeAgentSession
            } yield
              Redirect(routes.ApplicationController.showApplicationComplete())
                .flashing(
                  "tradingName" -> cRequest.agentSession.tradingName
                    .getOrElse(""),
                  "contactDetail" -> cRequest.agentSession.contactDetails
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
