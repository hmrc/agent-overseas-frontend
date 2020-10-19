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

package uk.gov.hmrc.agentoverseasfrontend.controllers.auth

import javax.inject.{Inject, Singleton}
import play.api.mvc.Results.{Forbidden, Redirect, SeeOther}
import play.api.mvc.{Request, Result}
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.CommonRouting
import uk.gov.hmrc.agentoverseasfrontend.controllers.subscription
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus._
import uk.gov.hmrc.agentoverseasfrontend.models.{OverseasApplication, SubscriptionRequest}
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService, SubscriptionService}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, authorisedEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthProviders, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionAuth @Inject()(
  val authConnector: AuthConnector,
  val sessionStoreService: SessionStoreService,
  val applicationService: ApplicationService,
  val subscriptionService: SubscriptionService
)(implicit val env: Environment, val config: Configuration, val appConfig: AppConfig, val ec: ExecutionContext)
    extends AuthBase with CommonRouting with Logging {

  def withBasicAgentAuth(
    block: SubscriptionRequest => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(allEnrolments and credentials) {
        case enrolments ~ creds =>
          creds match {
            case Some(c) => block(SubscriptionRequest(c.providerId, enrolments.enrolments))
            case None =>
              logger.warn("credentials expected but not found for the logged in user")
              Future.successful(Forbidden)
          }
      }
      .recover(handleFailure(request))

  def withHmrcAsAgentAction(
    block: Arn => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(Enrolment("HMRC-AS-AGENT") and AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
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
    block: OverseasApplication => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(allEnrolments) { enrolments =>
        if (hasAgentEnrolment(enrolments)) {
          Future.successful(Redirect(appConfig.asaFrontendUrl))
        } else {
          val hasCleanCreds = enrolments.enrolments.isEmpty

          subscriptionService.mostRecentApplication.flatMap {
            case Some(application) if application.status == Pending || application.status == Rejected =>
              Future.successful(SeeOther(s"${appConfig.agentOverseasFrontendUrl}/application-status"))
            case Some(application) if application.status == Accepted =>
              if (hasCleanCreds) block(application)
              else Future.successful(Redirect(subscription.routes.SubscriptionRootController.nextStep()))
            case Some(application) if application.status == Registered || application.status == Complete =>
              Future.successful(Redirect(subscription.routes.SubscriptionController.subscribe()))
            case Some(application) if application.status == AttemptingRegistration =>
              Future.successful(Redirect(subscription.routes.SubscriptionRootController.showApplicationIssue()))
            case None =>
              Future.successful(SeeOther(s"${appConfig.agentOverseasFrontendUrl}"))
            case application =>
              throw new RuntimeException(s"Could not proceed with application status ${application.map(_.status)}")
          }
        }
      }
      .recover(handleFailure(request))

}
