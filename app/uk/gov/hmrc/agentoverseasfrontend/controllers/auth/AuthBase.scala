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

import play.api.mvc.Results.{Forbidden, Redirect}
import play.api.mvc.{Request, Result}
import play.api.{Configuration, Environment, Logger, Mode}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

trait AuthBase extends AuthRedirects with AuthorisedFunctions {
  val authConnector: AuthConnector
  val env: Environment
  val config: Configuration
  val appConfig: AppConfig
  implicit val ec: ExecutionContext

  lazy val isDevEnv: Boolean =
    if (env.mode.equals(Mode.Test)) false
    else config.getOptional[String]("run.mode").forall(Mode.Dev.toString.equals)

  def withBasicAuth(
    block: Request[_] => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway)) {
      block(request)
    }.recover(handleFailure(request))

  def withBasicAuthAndAgentAffinity(
    block: Request[_] => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent) {
      block(request)
    }.recover(handleFailure(request))

  protected def hasAgentEnrolment(enrolments: Enrolments): Boolean =
    enrolments.enrolments
      .find(_.key equals "HMRC-AS-AGENT")
      .exists(_.isActivated)

  protected def getArn(enrolments: Enrolments): Option[Arn] =
    for {
      enrolment  <- enrolments.getEnrolment("HMRC-AS-AGENT")
      identifier <- enrolment.getIdentifier("AgentReferenceNumber")
    } yield Arn(identifier.value)

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
      Redirect(application.routes.ApplicationRootController.showNotAgent())
  }

}