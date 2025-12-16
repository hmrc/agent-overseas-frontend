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

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

import javax.inject.Inject
import play.api.Environment
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.ApplicationAuth
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Pending
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.NotReceivedInDms
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.agentoverseasfrontend.views.html.application.application_not_ready
import uk.gov.hmrc.agentoverseasfrontend.views.html.application.not_agent
import uk.gov.hmrc.agentoverseasfrontend.views.html.application.status_rejected

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ApplicationRootController @Inject() (
  val env: Environment,
  authAction: ApplicationAuth,
  sessionStoreService: SessionCacheService,
  applicationService: ApplicationService,
  cc: MessagesControllerComponents,
  notAgentView: not_agent,
  applicationNotReadyView: application_not_ready,
  statusRejectedView: status_rejected
)(implicit
  appConfig: AppConfig,
  ec: ExecutionContext
)
extends AgentOverseasBaseController(
  sessionStoreService,
  applicationService,
  cc
)
with I18nSupport {

  import authAction.withBasicAuth

  def root: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth { _ =>
      Future.successful(Redirect(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired))
    }
  }

  def showNotAgent: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth { _ =>
      Future.successful(Ok(notAgentView()))
    }
  }

  def applicationStatus: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth { _ =>
      applicationService.getCurrentApplication.map {
        case Some(application) if application.status == Pending =>
          val createdOnPrettifyDate: String = application.applicationCreationDate.format(
            DateTimeFormatter.ofPattern("d MMMM YYYY").withZone(ZoneOffset.UTC)
          )
          val daysUntilReviewed: Int = daysUntilApplicationReviewed(application.applicationCreationDate)
          Ok(applicationNotReadyView(
            application.tradingName,
            createdOnPrettifyDate,
            daysUntilReviewed
          ))
        case Some(application) if application.status == Rejected || application.status == NotReceivedInDms => Ok(statusRejectedView(application))
        case Some(_) => SeeOther(s"${appConfig.selfExternalUrl + routes.ApplicationRootController.root.url}/create-account")
        case None => Redirect(routes.ApplicationRootController.root)
      }
    }
  }

  private def daysUntilApplicationReviewed(applicationCreationDate: LocalDateTime): Int = {
    val daysUntilAppReviewed =
      LocalDate
        .now(Clock.systemUTC)
        .until(applicationCreationDate.plusDays(appConfig.maintainerApplicationReviewDays), ChronoUnit.DAYS)
        .toInt
    if (daysUntilAppReviewed > 0)
      daysUntilAppReviewed
    else
      0
  }

}
