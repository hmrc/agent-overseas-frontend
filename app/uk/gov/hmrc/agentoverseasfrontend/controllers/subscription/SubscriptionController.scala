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

package uk.gov.hmrc.agentoverseasfrontend.controllers.subscription

import javax.inject.Inject
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AgentOverseasBaseController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.SubscriptionAuth
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.AlreadySubscribed
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.NoAgencyInSession
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.NoApplications
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.WrongApplicationStatus
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.agentoverseasfrontend.services.SubscriptionService
import uk.gov.hmrc.agentoverseasfrontend.views.html.application.cannot_verify_email_locked
import uk.gov.hmrc.agentoverseasfrontend.views.html.application.cannot_verify_email_technical
import uk.gov.hmrc.agentoverseasfrontend.views.html.subscription._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SubscriptionController @Inject() (
  override val messagesApi: MessagesApi,
  authAction: SubscriptionAuth,
  subscriptionService: SubscriptionService,
  applicationService: ApplicationService,
  mcc: MessagesControllerComponents,
  override val sessionStoreService: SessionCacheService,
  subscriptionCompleteView: subscription_complete,
  alreadySubscribedView: already_subscribed,
  emailLockedView: cannot_verify_email_locked,
  emailTechnicalErrorView: cannot_verify_email_technical
)(implicit
  override val ec: ExecutionContext,
  appConfig: AppConfig
)
extends AgentOverseasBaseController(
  sessionStoreService,
  applicationService,
  mcc
)
with Logging {

  import authAction.config
  import authAction.withBasicAgentAuth
  import authAction.withHmrcAsAgentAction

  def subscribe: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { implicit subRequest =>
      if (subRequest.enrolments.isEmpty) {
        sessionStoreService.fetchAgencyDetails.flatMap {
          case Some(agencyDetails) if !agencyDetails.isEmailVerified => Future.successful(Redirect(routes.SubscriptionEmailVerificationController.verifyEmail))
          case _ =>
            subscriptionService.subscribe.map {
              case Right(_) => Redirect(routes.SubscriptionController.subscriptionComplete)
              case Left(NoApplications) =>
                logger.info("User has no known applications, redirecting to application frontend")
                Redirect(s"${appConfig.selfExternalUrl + routes.SubscriptionRootController.root.url}")
              case Left(NoAgencyInSession) =>
                logger.info("No agency details in session, redirecting to /check-answers")
                Redirect(routes.BusinessIdentificationController.showCheckAnswers)
              case Left(AlreadySubscribed) => Redirect(routes.SubscriptionController.alreadySubscribed)
              case Left(WrongApplicationStatus) =>
                throw new IllegalStateException(
                  "Can not proceed with application - can not subscribe with an application in this status"
                )
            }
        }
      }
      else {
        logger.info("User has other enrolments, redirecting to /next-step")
        Future.successful(Redirect(routes.SubscriptionRootController.nextStep))
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
              agencyDetails.agencyEmail
            )
          )
        case None =>
          logger.warn("no agent session found on subscription complete page")
          SeeOther(s"${appConfig.selfExternalUrl + routes.SubscriptionRootController.root.url}")
      }
    }
  }

  def alreadySubscribed: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { subRequest =>
      Future.successful(Ok(alreadySubscribedView()))
    }
  }

  def showEmailLocked: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorised() {
      Future.successful(Ok(emailLockedView(routes.BusinessIdentificationController.showUpdateBusinessEmailForm)))
    }
  }

  def showEmailTechnicalError: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorised() {
      Future.successful(Ok(emailTechnicalErrorView()))
    }
  }

}
