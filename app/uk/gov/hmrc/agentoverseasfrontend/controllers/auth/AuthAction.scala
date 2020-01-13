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
import play.api.mvc.Results.{Forbidden, Redirect}
import play.api.mvc.{Request, Result}
import play.api.{Configuration, Environment, Logger, Mode}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.{CommonRouting, routes}
import uk.gov.hmrc.agentoverseasfrontend.models.CredentialRequest
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthAction @Inject()(
  val authConnector: AuthConnector,
  val sessionStoreService: SessionStoreService,
  val applicationService: ApplicationService
)(implicit val env: Environment, val config: Configuration, appConfig: AppConfig, ec: ExecutionContext)
    extends AuthRedirects with AuthorisedFunctions with CommonRouting {

  lazy val isDevEnv: Boolean =
    if (env.mode.equals(Mode.Test)) false
    else config.getOptional[String]("run.mode").forall(Mode.Dev.toString.equals)

  def withBasicAuth(
    block: Request[_] => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway)) {
      block(request)
    }.recover(handleFailure(request))

  def withBasicAgentAuth(
    block: Request[_] => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent) {
      block(request)
    }.recover(handleFailure(request))

  def withEnrollingAgent(
    block: CredentialRequest => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and allEnrolments) {
        case credentialsOpt ~ enrolments =>
          if (isEnrolledForHmrcAsAgent(enrolments))
            Future.successful(Redirect(appConfig.agentServicesAccountPath))
          else
            sessionStoreService.fetchAgentSession.flatMap {
              case Some(agentSession) =>
                credentialsOpt.fold(throw UnsupportedCredentialRole("User has no credentials"))(credentials =>
                  block(CredentialRequest(credentials.providerId, request, agentSession)))
              case None =>
                routesIfExistingApplication(appConfig.agentOverseasSubscriptionFrontendRootPath).map(Redirect)
            }
      }
      .recover(handleFailure(request))

  private def isEnrolledForHmrcAsAgent(enrolments: Enrolments): Boolean =
    enrolments.enrolments
      .find(_.key equals "HMRC-AS-AGENT")
      .exists(_.isActivated)

  protected def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession ⇒
      toGGLogin(
        if (isDevEnv) s"http://${request.host}${request.uri}"
        else s"${request.uri}")

    case _: InsufficientEnrolments ⇒
      Logger.warn(s"Logged in user does not have required enrolments")
      Forbidden

    case _: UnsupportedAuthProvider ⇒
      Logger.warn(s"user logged in with unsupported auth provider")
      Forbidden

    case _: UnsupportedAffinityGroup =>
      Logger.warn(s"user logged in with unsupported affinity group")
      Redirect(routes.StartController.showNotAgent())
  }

}
