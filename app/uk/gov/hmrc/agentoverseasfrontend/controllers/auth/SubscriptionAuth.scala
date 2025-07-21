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

package uk.gov.hmrc.agentoverseasfrontend.controllers.auth

import play.api.Configuration
import play.api.Environment
import play.api.Logging
import play.api.mvc.Results.Forbidden
import play.api.mvc.Results.Redirect
import play.api.mvc.Results.SeeOther
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.CommonRouting
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.{routes => applicationRoutes}
import uk.gov.hmrc.agentoverseasfrontend.controllers.subscription
import uk.gov.hmrc.agentoverseasfrontend.controllers.subscription.routes
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus._
import uk.gov.hmrc.agentoverseasfrontend.models.AgencyDetails
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus
import uk.gov.hmrc.agentoverseasfrontend.models.SubscriptionRequest
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.agentoverseasfrontend.services.SubscriptionService
import uk.gov.hmrc.agentoverseasfrontend.utils.RequestSupport._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.AuthProviders
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.authorisedEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.email
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.~

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SubscriptionAuth @Inject() (
  val authConnector: AuthConnector,
  val sessionStoreService: SessionCacheService,
  val applicationService: ApplicationService,
  val subscriptionService: SubscriptionService
)(implicit
  val env: Environment,
  val config: Configuration,
  val appConfig: AppConfig,
  val ec: ExecutionContext
)
extends AuthBase
with CommonRouting
with Logging {

  def getCreds(implicit rh: RequestHeader): Future[Credentials] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent).retrieve(credentials) {
      case Some(creds) => Future.successful(creds)
      case None => throw new IllegalStateException("credentials expected but not found for the logged in user")
    }

  def withBasicAgentAuth(
    block: SubscriptionRequest => Future[Result]
  )(implicit
    request: Request[_]
  ): Future[Result] = authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
    .retrieve(allEnrolments and credentials) { case enrolments ~ creds =>
      creds match {
        case Some(c) => block(SubscriptionRequest(c.providerId, enrolments.enrolments))
        case None =>
          logger.warn("credentials expected but not found for the logged in user")
          Future.successful(Forbidden)
      }
    }
    .recover(handleFailure(request))

  def withHmrcAsAgentAction(
    block: Arn => Future[Result]
  )(implicit
    request: Request[_]
  ): Future[Result] = authorised(Enrolment("HMRC-AS-AGENT") and AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
    .retrieve(authorisedEnrolments) { enrolments =>
      getArn(enrolments) match {
        case Some(arn) => block(arn)
        case None =>
          logger.warn("could not find the ARN from the logger in user to continue")
          Future.successful(Forbidden)
      }
    }
    .recover(handleFailure(request))

  def withSubscribingAgent(
    checkForEmailVerification: Boolean,
    generateNewDetailsIfNoSession: Boolean = false
  )(
    block: AgencyDetails => Future[Result]
  )(implicit
    request: Request[_]
  ): Future[Result] = authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
    .retrieve(allEnrolments and email) { case enrolments ~ maybeAuthEmail =>
      if (hasAgentEnrolment(enrolments)) {
        Future.successful(Redirect(appConfig.asaFrontendUrl))
      }
      else {
        val hasCleanCreds = enrolments.enrolments.isEmpty

        subscriptionService.mostRecentApplication.flatMap {
          case Some(application) if application.status == Pending || application.status == Rejected =>
            Future.successful(SeeOther(s"${appConfig.selfExternalUrl + applicationRoutes.ApplicationRootController.root.url}/application-status"))
          case Some(application) if application.status == ApplicationStatus.Accepted =>
            if (hasCleanCreds) {
              // happy path
              sessionStoreService.fetchAgencyDetails
                .flatMap { maybeAgencyDetails =>
                  // If there are no AgentDetails in session, create a new one derived from the agent's application data and store it in the session
                  if (maybeAgencyDetails.isEmpty && generateNewDetailsIfNoSession) {
                    val agencyDetails = AgencyDetails.fromOverseasApplication(application)
                    sessionStoreService.cacheAgencyDetails(agencyDetails).map(_ => Some(agencyDetails))
                  }
                  else
                    Future.successful(maybeAgencyDetails)
                }
                .flatMap {
                  case Some(agencyDetails) =>
                    // Consider the auth email as verified for email verification purposes (APB-7317)
                    val agencyDetailsFixed = agencyDetails.copy(verifiedEmails = agencyDetails.verifiedEmails ++ maybeAuthEmail.toSet)

                    def maybeUpdateSession(): Future[Unit] = {
                      val sessionNeedsUpdating = agencyDetailsFixed != agencyDetails
                      if (sessionNeedsUpdating)
                        sessionStoreService.cacheAgencyDetails(agencyDetailsFixed)
                      else
                        Future.successful(())
                    }

                    maybeUpdateSession().flatMap { _ =>
                      if (checkForEmailVerification && !agencyDetailsFixed.isEmailVerified) {
                        // email needs verifying
                        Future.successful(Redirect(routes.SubscriptionEmailVerificationController.verifyEmail))
                      }
                      else {
                        // happy path
                        block(agencyDetailsFixed)
                      }
                    }
                  case None =>
                    logger.warn(s"Missing agency details in session, redirecting back to /check-answers")
                    Future.successful(Redirect(routes.BusinessIdentificationController.showCheckAnswers))
                }
            }
            else
              Future.successful(Redirect(subscription.routes.SubscriptionRootController.nextStep))
          case Some(application) if application.status == Registered || application.status == Complete =>
            if (hasCleanCreds)
              subscriptionService.subscribe.flatMap {
                case Right(_) => Future.successful(Redirect(subscription.routes.SubscriptionController.subscriptionComplete))
                case Left(_) => Future.successful(Redirect(subscription.routes.SubscriptionController.alreadySubscribed))
              }
            else
              Future.successful(Redirect(subscription.routes.SubscriptionRootController.nextStep))
          case Some(application) if application.status == AttemptingRegistration =>
            Future.successful(Redirect(subscription.routes.SubscriptionRootController.showApplicationIssue))
          case None => Future.successful(SeeOther(s"${appConfig.selfExternalUrl + applicationRoutes.ApplicationRootController.root.url}"))
          case application => throw new RuntimeException(s"Could not proceed with application status ${application.map(_.status)}")
        }
      }
    }
    .recover(handleFailure(request))

}
