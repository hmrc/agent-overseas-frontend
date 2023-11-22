/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.ApplicationAuth
import uk.gov.hmrc.agentoverseasfrontend.forms.SuccessfulFileUploadConfirmationForm
import uk.gov.hmrc.agentoverseasfrontend.models.{AgentSession, Yes, YesNo}
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, MongoDBSessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasfrontend.views.html.application._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadController @Inject()(
  sessionStoreService: MongoDBSessionStoreService,
  authAction: ApplicationAuth,
  applicationService: ApplicationService,
  upscanConnector: UpscanConnector,
  cc: MessagesControllerComponents,
  fileUploadView: file_upload,
  tradingAddressNoJsView: trading_address_no_js_check_file,
  successfulFileUploadView: successful_file_upload,
  fileUploadFailedView: file_upload_failed)(implicit ex: ExecutionContext, appConfig: AppConfig)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with SessionBehaviour
    with Logging {

  import authAction.withEnrollingEmailVerifiedAgent

  def showAmlsUploadForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { implicit agentSession =>
      showUploadForm("amls")
    }
  }

  def showTradingAddressUploadForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { implicit agentSession =>
      showUploadForm("trading-address")
    }
  }

  def showTrnUploadForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { implicit agentSession =>
      showUploadForm("trn")
    }
  }

  private def showUploadForm(
    fileType: String)(implicit agentSession: AgentSession, hc: HeaderCarrier, request: Request[_]) =
    upscanConnector
      .initiate()
      .flatMap(
        upscan =>
          sessionStoreService
            .cacheAgentSession(agentSession.copy(fileType = Some(fileType)))
            .map(_ => Ok(fileUploadView(upscan, fileType, getBackLink(fileType)))))

  def showTradingAddressNoJsCheckPage: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { agentSession =>
      Ok(tradingAddressNoJsView())
    }
  }

  //these pollStatus functions are called via ajax in the assets/javascripts/script.js
  def pollStatus(fileType: String, reference: String): Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { agentSession =>
      sessionStoreService.fetchAgentSession.flatMap {
        case Some(agentSession) =>
          applicationService
            .upscanPollStatus(reference)
            .flatMap { response =>
              if (response.fileStatus == "READY") {
                val updatedSession = fileType match {
                  case "trading-address" =>
                    agentSession.copy(tradingAddressUploadStatus = Some(response))
                  case "amls" =>
                    agentSession.copy(amlsUploadStatus = Some(response))
                  case "trn" =>
                    agentSession.copy(trnUploadStatus = Some(response))
                  case _ =>
                    throw new RuntimeException(s"invalid fileType for upscan callback $fileType")
                }
                sessionStoreService
                  .cacheAgentSession(updatedSession)
                  .flatMap(_ => {
                    logger.info(s"saving the callback response $response for fileType $fileType")
                    Ok(Json.toJson(response))
                  })
              } else {
                Ok(Json.toJson(response))
              }
            }
        case None => throw new RuntimeException("no agent session")
      }
    }
  }

  def showSuccessfulUploadedForm(): Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { agentSession =>
      sessionStoreService.fetchAgentSession.flatMap {
        case Some(agentSession) =>
          agentSession.fileType match {
            case Some(fileType) =>
              getFileNameFromSession(fileType).map { filename =>
                Ok(
                  successfulFileUploadView(
                    SuccessfulFileUploadConfirmationForm.form,
                    filename,
                    fileType,
                    backToFileUploadPage(fileType)))
              }
            case None =>
              logger.info(s"could not find fileType in session")
              Redirect(routes.FileUploadController.showAmlsUploadForm)
          }
        case None => throw new RuntimeException("no agent session")
      }
    }
  }

  private def backToFileUploadPage(fileType: String): Option[String] =
    fileType match {

      case "amls" => Some(routes.FileUploadController.showAmlsUploadForm.url)
      case "trading-address" =>
        Some(routes.FileUploadController.showTradingAddressUploadForm.url)
      case "trn" => Some(routes.FileUploadController.showTrnUploadForm.url)
      case _     => None
    }

  def submitSuccessfulFileUploadedForm: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { implicit agentSession =>
      SuccessfulFileUploadConfirmationForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val fileType = formWithErrors.data("fileType")
            getFileNameFromSession(fileType).map(filename =>
              Ok(successfulFileUploadView(formWithErrors, filename, fileType, backToFileUploadPage(fileType))))
          },
          validForm => {
            val fileType = validForm.fileType
            val newValue = YesNo(validForm.choice)
            if (Yes == newValue) {
              nextPage(fileType).map(url => Redirect(url))
            } else
              Redirect(
                backToFileUploadPage(fileType).getOrElse(
                  routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm.url))
          }
        )
    }
  }

  def showUploadFailedPage: Action[AnyContent] = Action.async { implicit request =>
    withEnrollingEmailVerifiedAgent { agentSession =>
      sessionStoreService.fetchAgentSession.map {
        case Some(agentSession) =>
          agentSession.fileType match {
            case Some(fileType) =>
              Ok(fileUploadFailedView(backToFileUploadPage(fileType)))
            case None =>
              logger.info("expecting a fileType in session for failed upload but none found")
              Redirect(routes.FileUploadController.showAmlsUploadForm)
          }
        case None => throw new RuntimeException("no agent session")
      }
    }
  }

  private def getFileNameFromSession(fileType: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(agentSession) => {
        fileType match {
          case "trading-address" => agentSession.tradingAddressUploadStatus
          case "amls"            => agentSession.amlsUploadStatus
          case "trn"             => agentSession.trnUploadStatus
          case _ =>
            throw new RuntimeException(s"could not get filename from session for fileType $fileType")
        }

      }.map(_.fileName)
        .getOrElse(throw new RuntimeException(s"filename is missing from the session for fileType $fileType"))
      case None => throw new RuntimeException("no agent session")
    }

  private def getBackLink(fileType: String)(implicit agentSession: AgentSession): Option[String] =
    if (agentSession.changingAnswers && (fileType match {
          case "trading-address" =>
            agentSession.tradingAddressUploadStatus.nonEmpty
          case "amls" => agentSession.amlsUploadStatus.nonEmpty
          case "trn"  => agentSession.trnUploadStatus.nonEmpty
          case _ =>
            logger.info("routing error for back link- unrecognized document proof file key!")
            false
        })) {
      Some(showCheckYourAnswersUrl)

    } else {
      fileType match {
        case "trading-address" =>
          Some(routes.TradingAddressController.showMainBusinessAddressForm.url)
        case "amls" =>
          Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm.url)
        case "trn" =>
          Some(routes.TaxRegController.showYourTaxRegNumbersForm.url)
        case _ =>
          logger.info("routing error for back link- unrecognized document proof file key!")
          None
      }
    }

  private def nextPage(fileType: String)(implicit agentSession: AgentSession, hc: HeaderCarrier): Future[String] =
    if (agentSession.changingAnswers) {
      sessionStoreService
        .cacheAgentSession(agentSession.copy(changingAnswers = false))
        .map(_ => showCheckYourAnswersUrl)
    } else {
      fileType match {
        case "trading-address" =>
          routes.ApplicationController.showRegisteredWithHmrcForm.url
        case "amls" => routes.ApplicationController.showContactDetailsForm.url
        case "trn"  => routes.ApplicationController.showCheckYourAnswers.url
        case _ =>
          throw new RuntimeException("routing error for next page- unrecognized document proof file key!")
      }
    }

}
