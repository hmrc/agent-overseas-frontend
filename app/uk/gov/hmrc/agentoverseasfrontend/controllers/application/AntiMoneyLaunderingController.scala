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
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.ApplicationAuth
import uk.gov.hmrc.agentoverseasfrontend.forms.AmlsDetailsForm
import uk.gov.hmrc.agentoverseasfrontend.forms.YesNoRadioButtonForms.amlsRequiredForm
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasfrontend.models.{AgentSession, RadioConfirm}
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, MongoDBSessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasfrontend.views.html.application._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AntiMoneyLaunderingController @Inject()(
  val env: Environment,
  sessionStoreService: MongoDBSessionStoreService,
  applicationService: ApplicationService,
  val upscanConnector: UpscanConnector,
  authAction: ApplicationAuth,
  cc: MessagesControllerComponents,
  amlsView: anti_money_laundering,
  amlsRequiredView: anti_money_laundering_required)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with SessionBehaviour
    with I18nSupport {

  import authAction.withEnrollingAgent

  def showMoneyLaunderingRequired: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val backUrl =
        if (agentSession.changingAnswers) Some(showCheckYourAnswersUrl)
        else None
      Ok(
        amlsRequiredView(
          agentSession.amlsRequired.fold(amlsRequiredForm)(amlsRequired =>
            amlsRequiredForm.fill(RadioConfirm(amlsRequired))),
          backUrl))
    }
  }

  def submitMoneyLaunderingRequired: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      amlsRequiredForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Ok(amlsRequiredView(formWithErrors))
          },
          isRequired =>
            for {
              session <- agentSession
              isChanging = session.changingAnswers
              updatedSession = updateAmlsSessionBasedOnChanging(isChanging, isRequired.value, session)
              redirectUrl = amlsRedirectUrlBasedOnChanging(isChanging, isRequired.value)
              result <- updateSessionAndRedirect(updatedSession)(redirectUrl)
            } yield result
        )
    }
  }

  private def updateAmlsSessionBasedOnChanging(
    isChanging: Boolean,
    isRequired: Boolean,
    session: AgentSession): AgentSession = {
    val updatedSessionWithRemovedAmlsDetails =
      session.copy(amlsRequired = Some(isRequired), amlsDetails = None, amlsUploadStatus = None)
    (isChanging, isRequired) match {
      case (true, false) => updatedSessionWithRemovedAmlsDetails
      case _             => session.copy(amlsRequired = Some(isRequired))
    }
  }

  private def amlsRedirectUrlBasedOnChanging(isChanging: Boolean, isRequired: Boolean): String =
    (isChanging, isRequired) match {
      case (_, true) =>
        routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm.url
      case (true, _) => showCheckYourAnswersUrl
      case _         => routes.ApplicationController.showContactDetailsForm.url
    }

  def showAntiMoneyLaunderingForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      val form = AmlsDetailsForm.form

      val backUrl: Future[Option[String]] = {
        if (agentSession.changingAnswers) {
          agentSession.amlsDetails match {
            case Some(_) => Some(showCheckYourAnswersUrl)
            case None =>
              Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url)
          }
        } else
          applicationService.getCurrentApplication.map {
            case Some(application) if application.status == Rejected =>
              Some(routes.ApplicationRootController.applicationStatus.url)
            case _ =>
              Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url)
          }
      }

      backUrl.map(url =>
        agentSession.amlsRequired match {
          case Some(true) =>
            Ok(amlsView(agentSession.amlsDetails.fold(form)(form.fill), url)) // happy path
          case Some(false) =>
            Redirect(routes.ApplicationController.showContactDetailsForm.url) // skip money laundering
          case None =>
            Redirect(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url) // go back and make a choice
      })
    }
  }

  def submitAntiMoneyLaundering: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingAgent { agentSession =>
      AmlsDetailsForm.form.bindFromRequest
        .fold(
          formWithErrors => {
            sessionStoreService.fetchAgentSession.map {
              case Some(session) if session.changingAnswers =>
                Ok(amlsView(formWithErrors, Some(showCheckYourAnswersUrl)))
              case _ =>
                Ok(amlsView(formWithErrors, Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired.url)))
            }
          },
          validForm => {
            agentSession
              .map(_.copy(amlsDetails = Some(validForm)))
              .flatMap(updateSessionAndRedirect(_)(routes.FileUploadController.showAmlsUploadForm.url))
          }
        )
    }
  }
}
