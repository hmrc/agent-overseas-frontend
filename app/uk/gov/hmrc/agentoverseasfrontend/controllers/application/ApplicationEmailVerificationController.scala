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

package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import play.api.Environment
import play.api.i18n.I18nSupport
import play.api.mvc.{Call, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.GenericEmailVerificationController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.ApplicationAuth
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, EmailVerificationService, MongoDBSessionStoreService}
import uk.gov.hmrc.hmrcfrontend.config.AccessibilityStatementConfig
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationEmailVerificationController @Inject()(
  env: Environment,
  authAction: ApplicationAuth,
  val sessionStoreService: MongoDBSessionStoreService,
  val applicationService: ApplicationService,
  emailVerificationService: EmailVerificationService,
  val controllerComponents: MessagesControllerComponents,
  accessibilityStatementConfig: AccessibilityStatementConfig
)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends GenericEmailVerificationController[AgentSession](env, emailVerificationService) with SessionBehaviour
    with I18nSupport {

  import authAction.getCredsAndAgentSession

  override def emailVerificationEnabled: Boolean = !appConfig.disableEmailVerification

  override def emailVerificationFrontendBaseUrl: String = appConfig.emailVerificationFrontendBaseUrl
  override def accessibilityStatementUrl(implicit request: RequestHeader): String =
    accessibilityStatementConfig.url.getOrElse("")

  override def getState(implicit hc: HeaderCarrier): Future[(AgentSession, String)] =
    getCredsAndAgentSession.map {
      case (creds, agentSession) =>
        (agentSession, creds.providerId)
    }

  override def getEmailToVerify(session: AgentSession): String = session.contactDetails.map(_.businessEmail).getOrElse {
    throw new IllegalStateException("A verify email call has been made but no email to verify is present.")
  }

  override def isAlreadyVerified(session: AgentSession, email: String): Boolean = session.verifiedEmails.contains(email)

  override def markEmailAsVerified(session: AgentSession, email: String)(
    implicit hc: HeaderCarrier): Future[AgentSession] = {
    val newAgentSession = session.copy(verifiedEmails = session.verifiedEmails + email)
    sessionStoreService.cacheAgentSession(newAgentSession).map(_ => newAgentSession)
  }

  override def selfRoute: Call = routes.ApplicationEmailVerificationController.verifyEmail()
  override def redirectUrlIfVerified(session: AgentSession): Call = lookupNextPage(Some(session))
  override def redirectUrlIfLocked(session: AgentSession): Call = routes.ApplicationController.showCannotVerifyEmail()
  override def redirectUrlIfError(session: AgentSession): Call = routes.ApplicationController.showCannotVerifyEmail()
  override def backLinkUrl(session: AgentSession): Option[Call] = None
  override def enterEmailUrl(session: AgentSession): Call = routes.ApplicationController.showContactDetailsForm()
}
