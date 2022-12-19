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

package uk.gov.hmrc.agentoverseasfrontend.controllers.auth

import javax.inject.{Inject, Singleton}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.{CommonRouting, routes}
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, MongoDBSessionStoreService}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationAuth @Inject()(
  val authConnector: AuthConnector,
  val sessionStoreService: MongoDBSessionStoreService,
  val applicationService: ApplicationService
)(implicit val env: Environment, val config: Configuration, val appConfig: AppConfig, val ec: ExecutionContext)
    extends AuthBase with CommonRouting {

  def getCredsAndAgentSession(implicit hc: HeaderCarrier): Future[(Credentials, AgentSession)] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and allEnrolments) {
        case Some(credentials) ~ enrolments =>
          sessionStoreService.fetchAgentSession.flatMap {
            case Some(agentSession) =>
              Future.successful((credentials, agentSession))
            case None =>
              throw new IllegalStateException("Agent session not found")
          }
        case None ~ _ => throw UnsupportedCredentialRole("User has no credentials")
      }

  def withCredsAndEnrollingAgent(checkForEmailVerification: Boolean)(
    body: (Credentials, AgentSession) => Future[Result])(
    implicit hc: HeaderCarrier,
    request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and allEnrolments) {
        case Some(credentials) ~ enrolments =>
          if (hasAgentEnrolment(enrolments))
            Future.successful(Redirect(appConfig.asaFrontendUrl))
          else
            sessionStoreService.fetchAgentSession.flatMap {
              case Some(agentSession) if checkForEmailVerification && agentSession.emailNeedsVerifying =>
                Future.successful(Redirect(routes.ApplicationEmailVerificationController.verifyEmail))
              case Some(agentSession) =>
                body(credentials, agentSession)
              case None =>
                routesIfExistingApplication(s"${appConfig.agentOverseasFrontendUrl}/create-account")
                  .map(Redirect)
            }

        case None ~ _ => throw UnsupportedCredentialRole("User has no credentials")
      }
      .recover(handleFailure(request))

  def withEnrollingAgent(
    body: AgentSession => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    withCredsAndEnrollingAgent(checkForEmailVerification = false)((_, session) => body(session))

  def withEnrollingEmailVerifiedAgent(
    body: AgentSession => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    withCredsAndEnrollingAgent(checkForEmailVerification = true)((_, session) => body(session))
}
