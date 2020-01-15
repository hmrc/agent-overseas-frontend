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

import javax.inject.{Inject, Named}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Logger}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AgentOverseasBaseController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.{AuthBase, SubscriptionAuth}
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.{AlreadySubscribed, NoAgencyInSession, NoApplications, WrongApplicationStatus}
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService, SubscriptionService}
import uk.gov.hmrc.agentoverseasfrontend.views.html.subscription._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject()(
  override val messagesApi: MessagesApi,
  authAction: SubscriptionAuth,
  subscriptionService: SubscriptionService,
  applicationService: ApplicationService,
  mcc: MessagesControllerComponents,
  override val sessionStoreService: SessionStoreService,
  subscriptionCompleteView: subscription_complete,
  alreadySubscribedView: already_subscribed,
  accessibilityStatementView: accessibility_statement)(implicit override val ec: ExecutionContext, appConfig: AppConfig)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, mcc) with SessionStoreHandler {

  import authAction.{config, withBasicAgentAuth, withHmrcAsAgentAction}

  def subscribe: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { implicit subRequest =>
      if (subRequest.enrolments.isEmpty) {
        subscriptionService.subscribe.map {
          case Right(_) =>
            Redirect(routes.SubscriptionController.subscriptionComplete())
          case Left(NoApplications) =>
            Logger.info("User has no known applications, redirecting to application frontend")
            Redirect(s"${appConfig.agentOverseasFrontendUrl}/create-account")
          case Left(NoAgencyInSession) =>
            Logger.info("No agency details in session, redirecting to /check-answers")
            Redirect(routes.BusinessIdentificationController.showCheckAnswers())
          case Left(AlreadySubscribed) =>
            Redirect(routes.SubscriptionController.alreadySubscribed())
          case Left(WrongApplicationStatus) =>
            throw new IllegalStateException(
              "Can not proceed with application - can not subscribe with an application in this status")
        }
      } else {
        Logger.info("User has other enrolments, redirecting to /next-step")
        Future.successful(Redirect(routes.SubscriptionRootController.nextStep()))
      }
    }
  }

  def subscriptionComplete: Action[AnyContent] = Action.async { implicit request =>
    withHmrcAsAgentAction { implicit arn =>
      sessionStoreService.fetchAgencyDetails.map {
        case Some(agencyDetails) =>
          Ok(
            subscriptionCompleteView(
              appConfig.agentGuidancePageFullUrl,
              appConfig.asaFrontendUrl,
              arn.value,
              agencyDetails.agencyName,
              agencyDetails.agencyEmail))
        case None =>
          Logger.warn("no agent session found on subscription complete page")
          SeeOther(s"${appConfig.agentOverseasFrontendUrl}/create-account")
      }
    }
  }

  def alreadySubscribed: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { implicit subRequest =>
      Future.successful(Ok(alreadySubscribedView()))
    }
  }

  def showAccessibilityStatement: Action[AnyContent] = Action { implicit request =>
    val userAction: String = request.headers.get(HeaderNames.REFERER).getOrElse("")
    val accessibilityUrl: String = s"${appConfig.accessibilityUrl}$userAction"
    Ok(accessibilityStatementView(accessibilityUrl))
  }
}
