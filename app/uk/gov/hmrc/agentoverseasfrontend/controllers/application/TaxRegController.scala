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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Environment, Logger}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.{ApplicationAuth, AuthBase}
import uk.gov.hmrc.agentoverseasfrontend.forms.YesNoRadioButtonForms.removeTrnForm
import uk.gov.hmrc.agentoverseasfrontend.forms.{AddTrnForm, DoYouWantToAddAnotherTrnForm, TaxRegistrationNumberForm, UpdateTrnForm}
import uk.gov.hmrc.agentoverseasfrontend.models.{AgentSession, TaxRegistrationNumber, Trn}
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasfrontend.views.html._
import uk.gov.hmrc.agentoverseasfrontend.views.html.application._

import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext

@Singleton
class TaxRegController @Inject()(
  val env: Environment,
  authAction: ApplicationAuth,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  cc: MessagesControllerComponents,
  trnView: tax_registration_number,
  taxMoreInfoNeededView: tax_more_info_needed,
  addTrnView: add_tax_registration_number,
  yourTrnsView: your_tax_registration_numbers,
  updateTrnView: update_tax_registration_number,
  removeTrnView: remove_tax_reg_number,
  errorTemplateView: error_template)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with SessionBehaviour
    with I18nSupport {

  import authAction.{withEnrollingAgent}

  def showTaxRegistrationNumberForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val storedTrns = agentSession.taxRegistrationNumbers
        .getOrElse(SortedSet.empty[Trn])

      val whichTrnToPopulate = if (storedTrns.size == 1) {
        storedTrns.headOption
      } else {
        None
      }

      val prePopulate =
        TaxRegistrationNumber(agentSession.hasTaxRegNumbers, whichTrnToPopulate)
      Ok(trnView(TaxRegistrationNumberForm.form.fill(prePopulate)))
    }
  }

  def submitTaxRegistrationNumber: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      TaxRegistrationNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(trnView(formWithErrors)),
          validForm => {

            val (updatedSession, redirectLink) =
              if (validForm.canProvideTaxRegNo.contains(true)) {
                (
                  agentSession.copy(
                    hasTaxRegNumbers = validForm.canProvideTaxRegNo,
                    taxRegistrationNumbers = validForm.value.flatMap(taxId => Some(SortedSet(taxId))),
                    hasTrnsChanged = validForm.value.isDefined
                  ),
                  routes.TaxRegController.showYourTaxRegNumbersForm().url)
              } else {
                (
                  agentSession.copy(
                    hasTaxRegNumbers = None,
                    taxRegistrationNumbers = None,
                    trnUploadStatus = None,
                    hasTrnsChanged = false
                  ),
                  routes.TaxRegController.showMoreInformationNeeded().url)
              }

            updateSession(updatedSession)(redirectLink)
          }
        )
    }
  }

  def showMoreInformationNeeded: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      Ok(taxMoreInfoNeededView())
    }
  }

  def showAddTaxRegNoForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      Ok(addTrnView(AddTrnForm.form))
    }
  }

  def submitAddTaxRegNo: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      AddTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(addTrnView(formWithErrors)),
          validForm => {
            val trns = agentSession.taxRegistrationNumbers match {
              case Some(numbers) => numbers + Trn(validForm)
              case None          => SortedSet(validForm).map(Trn.apply)
            }
            updateSession(
              agentSession
                .copy(
                  taxRegistrationNumbers = Some(trns),
                  hasTaxRegNumbers = Some(true),
                  changingAnswers = false,
                  hasTrnsChanged = true))(routes.TaxRegController.showYourTaxRegNumbersForm().url)
          }
        )
    }
  }

  def showYourTaxRegNumbersForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val trns = agentSession.taxRegistrationNumbers
        .getOrElse(SortedSet.empty[Trn])
      if (agentSession.changingAnswers) {
        Ok(yourTrnsView(DoYouWantToAddAnotherTrnForm.form, trns.map(_.value), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(yourTrnsView(DoYouWantToAddAnotherTrnForm.form, trns.map(_.value)))
      }
    }
  }

  def submitYourTaxRegNumbers: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      DoYouWantToAddAnotherTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val trns = agentSession.taxRegistrationNumbers
              .getOrElse(SortedSet.empty[Trn])
            if (agentSession.changingAnswers) {
              Ok(yourTrnsView(formWithErrors, trns.map(_.value), Some(showCheckYourAnswersUrl)))
            } else {
              Ok(yourTrnsView(formWithErrors, trns.map(_.value)))
            }
          },
          validForm => {
            validForm.value match {
              case Some(true) =>
                Redirect(routes.TaxRegController.showAddTaxRegNoForm().url)
              case _ =>
                if (agentSession.hasTrnsChanged) {
                  updateSession(agentSession.copy(trnUploadStatus = None, hasTrnsChanged = false))(
                    routes.FileUploadController.showTrnUploadForm().url)
                } else {
                  updateSession(agentSession.copy(hasTrnsChanged = false))(
                    routes.ApplicationController.showCheckYourAnswers().url)
                }
            }
          }
        )
    }
  }

  def submitUpdateTaxRegNumber: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      UpdateTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Logger.warn(
              s"error during updating tax registration number ${formWithErrors.errors.map(_.message).mkString(",")}")
            Ok(updateTrnView(formWithErrors))
          },
          validForm =>
            validForm.updated match {
              case Some(updatedTrn) =>
                val updatedSet = agentSession.taxRegistrationNumbers
                  .fold[SortedSet[Trn]](SortedSet.empty)(trns => trns - Trn(validForm.original) + Trn(updatedTrn))

                updateSession(agentSession
                  .copy(taxRegistrationNumbers = Some(updatedSet), changingAnswers = false, hasTrnsChanged = true))(
                  routes.TaxRegController.showYourTaxRegNumbersForm().url)

              case None =>
                Ok(updateTrnView(UpdateTrnForm.form.fill(validForm.copy(updated = Some(validForm.original)))))
          }
        )
    }
  }

  def showRemoveTaxRegNumber(trn: String): Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      if (agentSession.taxRegistrationNumbers.exists(_.contains(Trn(trn))))
        Ok(removeTrnView(removeTrnForm, trn))
      else
        Ok(errorTemplateView("global.error.404.title", "global.error.404.heading", "global.error.404.message"))
    }
  }

  def submitRemoveTaxRegNumber(trn: String): Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      removeTrnForm
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(removeTrnView(formWithErrors, trn)),
          validForm => {
            if (validForm.value) {
              val updatedSet = agentSession.taxRegistrationNumbers
                .fold[SortedSet[Trn]](SortedSet.empty)(trns => trns - Trn(trn))
              val toUpdate: AgentSession =
                if (updatedSet.isEmpty)
                  agentSession
                    .copy(
                      hasTaxRegNumbers = None,
                      taxRegistrationNumbers = None,
                      trnUploadStatus = None,
                      changingAnswers = false,
                      hasTrnsChanged = true)
                else
                  agentSession
                    .copy(taxRegistrationNumbers = Some(updatedSet), changingAnswers = false, hasTrnsChanged = true)

              val redirectUrl =
                if (updatedSet.nonEmpty)
                  routes.TaxRegController.showYourTaxRegNumbersForm().url
                else routes.TaxRegController.showTaxRegistrationNumberForm().url
              updateSession(toUpdate)(redirectUrl)
            } else {
              Redirect(routes.TaxRegController.showYourTaxRegNumbersForm())
            }
          }
        )
    }
  }

}
