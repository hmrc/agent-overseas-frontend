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

package uk.gov.hmrc.agentoverseasfrontend.controllers

import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AgentOverseasBaseController
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.{routes => applicationRoutes}
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.SubscriptionAuth
import uk.gov.hmrc.agentoverseasfrontend.controllers.subscription.{routes => subscriptionRoutes}
import uk.gov.hmrc.agentoverseasfrontend.models.ProviderId
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.agentoverseasfrontend.services.SubscriptionService
import uk.gov.hmrc.agentoverseasfrontend.views.html.subscription._
import uk.gov.hmrc.http.SessionKeys.sessionId

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class SignOutController @Inject() (
  override val messagesApi: MessagesApi,
  service: SubscriptionService,
  applicationService: ApplicationService,
  mcc: MessagesControllerComponents,
  authAction: SubscriptionAuth,
  sessionStoreService: SessionCacheService,
  timedOutView: timed_out
)(implicit
  val appConfig: AppConfig,
  override val ec: ExecutionContext,
  config: Configuration
)
extends AgentOverseasBaseController(
  sessionStoreService,
  applicationService,
  mcc
) {

  import authAction.withSimpleAgentAuth

  private def signOutWithContinue(continue: String) = {
    val signOutAndRedirectUrl: String = uri"""${appConfig.signOutUrl}?${Map("continue" -> continue)}""".toString
    Redirect(signOutAndRedirectUrl)
  }

  def signOutToGGRegistrationWhenSubscribing: Action[AnyContent] = Action.async { implicit request =>
    withSimpleAgentAuth { implicit subRequest =>
      sessionStoreService.cacheProviderId(ProviderId(subRequest.authProviderId)).map { _ =>
        val postRegistrationContinue =
          uri"${appConfig.selfExternalUrl + subscriptionRoutes.BusinessIdentificationController.returnFromGGRegistration(request.session.apply(sessionId)).url}"
        val params = Seq(
          "accountType" -> "agent",
          "origin" -> "unknown",
          "continue" -> postRegistrationContinue.toString
        )
        val continueUrl = uri"${appConfig.ggRegistrationFrontendSosRedirectPath}?${params}"
        signOutWithContinue(continueUrl.toString)
      }
    }
  }

  def signOutToGGRegistration: Action[AnyContent] = Action {
    val continueFromGG = uri"${appConfig.selfExternalUrl + applicationRoutes.ApplicationRootController.root.url}"
    val params = Seq(
      "accountType" -> "agent",
      "origin" -> "unknown",
      "continue" -> continueFromGG.toString
    )
    val continueUrl = uri"${appConfig.ggRegistrationFrontendSosRedirectPath}?${params}"
    signOutWithContinue(continueUrl.toString)
  }

  def startFeedbackSurvey: Action[AnyContent] = Action {
    val continueUrl = uri"${appConfig.feedbackSurveyUrl}"
    signOutWithContinue(continueUrl.toString)
  }

  def signOut: Action[AnyContent] = Action {
    val continueUrl = uri"${appConfig.selfExternalUrl + applicationRoutes.ApplicationRootController.root.url}"
    signOutWithContinue(continueUrl.toString)
  }

  def timeOut: Action[AnyContent] = Action {
    val continueUrl = uri"${appConfig.selfExternalUrl + routes.SignOutController.timedOut.url}"
    signOutWithContinue(continueUrl.toString)
  }

  def timedOut: Action[AnyContent] = Action { implicit request =>
    Ok(timedOutView())
  }

}
