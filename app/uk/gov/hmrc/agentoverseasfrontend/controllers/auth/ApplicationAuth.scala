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
import play.api.mvc.Results.Redirect
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.CommonRouting
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.routes
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.agentoverseasfrontend.utils.RequestSupport._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.email
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.~

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationAuth @Inject() (
  val authConnector: AuthConnector,
  val sessionStoreService: SessionCacheService,
  val applicationService: ApplicationService
)(implicit
  val env: Environment,
  val config: Configuration,
  val appConfig: AppConfig,
  val ec: ExecutionContext
)
extends AuthBase
with CommonRouting {

  def getCredsAndAgentSession(implicit rh: RequestHeader): Future[(Credentials, AgentSession)] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and allEnrolments) {
        case Some(credentials) ~ enrolments =>
          sessionStoreService.fetchAgentSession.flatMap {
            case Some(agentSession) => Future.successful((credentials, agentSession))
            case None => throw new IllegalStateException("Agent session not found")
          }
        case None ~ _ => throw UnsupportedCredentialRole("User has no credentials")
      }

  def withCredsAndEnrollingAgent(checkForEmailVerification: Boolean)(
    body: (
      Credentials,
      AgentSession
    ) => Future[Result]
  )(implicit
    request: Request[_]
  ): Future[Result] = authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
    .retrieve(credentials and allEnrolments and email) {
      case Some(credentials) ~ enrolments ~ maybeAuthEmail =>
        if (hasAgentEnrolment(enrolments))
          Future.successful(Redirect(appConfig.asaFrontendUrl))
        else {
          sessionStoreService.fetchAgentSession.flatMap {
            case Some(agentSession) =>
              // Consider the auth email as verified for email verification purposes (APB-7317)
              val agentSessionFixed = agentSession.copy(verifiedEmails = agentSession.verifiedEmails ++ maybeAuthEmail.toSet)

              def maybeUpdateSession(): Future[Unit] = {
                val sessionNeedsUpdating = agentSessionFixed != agentSession
                if (sessionNeedsUpdating)
                  sessionStoreService.cacheAgentSession(agentSessionFixed)
                else
                  Future.successful(())
              }

              maybeUpdateSession().flatMap { _ =>
                if (checkForEmailVerification && agentSessionFixed.emailNeedsVerifying) {
                  // email needs verifying
                  Future.successful(Redirect(routes.ApplicationEmailVerificationController.verifyEmail))
                }
                else {
                  // happy path
                  body(credentials, agentSessionFixed)
                }
              }
            case None =>
              routesIfExistingApplication(s"${appConfig.selfExternalUrl + routes.ApplicationRootController.root.url}/create-account")
                .map(Redirect)
          }
        }

      case None ~ _ ~ _ => throw UnsupportedCredentialRole("User has no credentials")
    }
    .recover(handleFailure(request))

  def withEnrollingAgent(
    body: AgentSession => Future[Result]
  )(implicit
    request: Request[_]
  ): Future[Result] = withCredsAndEnrollingAgent(checkForEmailVerification = false)((_, session) => body(session))

  def withEnrollingEmailVerifiedAgent(
    body: AgentSession => Future[Result]
  )(implicit
    request: Request[_]
  ): Future[Result] = withCredsAndEnrollingAgent(checkForEmailVerification = true)((_, session) => body(session))

}
