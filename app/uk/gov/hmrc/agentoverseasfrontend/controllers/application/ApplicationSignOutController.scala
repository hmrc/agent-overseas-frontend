/*
 * Copyright 2021 HM Revenue & Customs
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

import java.net.URL

import javax.inject.{Inject, Singleton}
import play.api.Environment
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.ApplicationAuth
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.utils.CallOps
import uk.gov.hmrc.agentoverseasfrontend.views.html.application.timed_out

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationSignOutController @Inject()(
  authAction: ApplicationAuth,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  cc: MessagesControllerComponents,
  timedOutView: timed_out)(implicit ec: ExecutionContext, appConfig: AppConfig, val env: Environment)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with I18nSupport {

  import authAction.withBasicAuth

  def signOut: Action[AnyContent] = Action {
    SeeOther(appConfig.companyAuthSignInUrl).withNewSession
  }

  def signOutWithContinueUrl: Action[AnyContent] = Action {
    val continueUrl = s"${appConfig.agentOverseasFrontendUrl}"
    SeeOther(CallOps.addParamsToUrl(appConfig.ggRegistrationFrontendSosRedirectPath, "continue" -> Some(continueUrl))).withNewSession
  }

  def signOutToStart: Action[AnyContent] = Action {
    Redirect(routes.ApplicationRootController.root()).withNewSession
  }

  def startFeedbackSurvey: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth { _ =>
      Future.successful(SeeOther(new URL(appConfig.feedbackSurveyUrl).toString).withNewSession)
    }
  }

  def keepAlive: Action[AnyContent] = Action.async {
    Future successful Ok("Ok")
  }

  def timedOut: Action[AnyContent] = Action.async { implicit request =>
    Future successful Forbidden(timedOutView())
  }
}
