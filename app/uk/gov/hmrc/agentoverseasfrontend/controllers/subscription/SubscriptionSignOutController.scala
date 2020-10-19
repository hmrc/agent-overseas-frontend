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

package uk.gov.hmrc.agentoverseasfrontend.controllers.subscription

import java.net.URL

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AgentOverseasBaseController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.SubscriptionAuth
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService, SubscriptionService}
import uk.gov.hmrc.agentoverseasfrontend.utils.CallOps
import uk.gov.hmrc.agentoverseasfrontend.views.html.subscription._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionSignOutController @Inject()(
  override val messagesApi: MessagesApi,
  service: SubscriptionService,
  applicationService: ApplicationService,
  mcc: MessagesControllerComponents,
  authAction: SubscriptionAuth,
  sessionStoreService: SessionStoreService,
  timedOutView: timed_out,
  signedOutView: signed_out)(
  implicit val appConfig: AppConfig,
  override val ec: ExecutionContext,
  config: Configuration)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, mcc) with SessionStoreHandler {

  import authAction.withBasicAgentAuth

  def signOutWithContinueUrl: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { implicit subRequest =>
      service.storeSessionDetails(subRequest.authProviderId).map { idRef =>
        val returnContinueUrl =
          s"${appConfig.agentOverseasFrontendUrl}/create-account/return-from-gg-registration?sessionId=$idRef"

        SeeOther(
          CallOps.addParamsToUrl(
            appConfig.ggRegistrationFrontendSosRedirectPath,
            "continue" -> Some(returnContinueUrl))).withNewSession
      }
    }
  }

  def startFeedbackSurvey: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { subRequest =>
      Future.successful(SeeOther(new URL(appConfig.feedbackSurveyUrl).toString).withNewSession)
    }
  }

  def signOut: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { subRequest =>
      Future.successful(SeeOther(routes.SubscriptionRootController.root().url).withNewSession)
    }
  }

  def keepAlive = Action.async {
    Future successful Ok("OK")
  }

  def timedOut: Action[AnyContent] = Action.async { implicit request =>
    Future successful Forbidden(timedOutView())
  }

  def signedOut = Action.async { implicit request =>
    val continueUrl = CallOps.addParamsToUrl(routes.SubscriptionRootController.root().url)
    Future successful Forbidden(signedOutView(continueUrl)).withNewSession
  }
}
