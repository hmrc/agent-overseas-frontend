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

import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.Environment
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.GenericEmailVerificationController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.SubscriptionAuth
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.services.{EmailVerificationService, MongoDBSessionStoreService, SubscriptionService}
import uk.gov.hmrc.hmrcfrontend.config.AccessibilityStatementConfig
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionEmailVerificationController @Inject()(
  env: Environment,
  authAction: SubscriptionAuth,
  val sessionStoreService: MongoDBSessionStoreService,
  val applicationService: SubscriptionService,
  emailVerificationService: EmailVerificationService,
  val controllerComponents: MessagesControllerComponents,
  accessibilityStatementConfig: AccessibilityStatementConfig
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends GenericEmailVerificationController[AgencyDetails](env, emailVerificationService) with I18nSupport {

  override def emailVerificationEnabled: Boolean = !appConfig.disableEmailVerification

  override def emailVerificationFrontendBaseUrl: String = appConfig.emailVerificationFrontendBaseUrl
  override def accessibilityStatementUrl(implicit request: RequestHeader): String =
    accessibilityStatementConfig.url.getOrElse("")

  override def getState(implicit hc: HeaderCarrier): Future[(AgencyDetails, String)] =
    for {
      mAgencyDetails <- sessionStoreService.fetchAgencyDetails
      agencyDetails = mAgencyDetails.getOrElse(
        throw new IllegalStateException("Email verification: no agency details found in session"))
      creds <- authAction.getCreds
    } yield {
      (agencyDetails, creds.providerId)
    }

  override def getEmailToVerify(session: AgencyDetails): String = session.agencyEmail
  override def isAlreadyVerified(session: AgencyDetails, email: String): Boolean = session.emailVerified
  override def markEmailAsVerified(session: AgencyDetails, email: String)(
    implicit hc: HeaderCarrier): Future[AgencyDetails] = {
    val newAgencyDetails = session.copy(verifiedEmails = session.verifiedEmails + email)
    sessionStoreService.cacheAgencyDetails(newAgencyDetails).map(_ => newAgencyDetails)
  }

  override def selfRoute: Call = routes.SubscriptionEmailVerificationController.verifyEmail
  override def redirectUrlIfVerified(session: AgencyDetails): Call =
    routes.BusinessIdentificationController.showCheckAnswers
  override def redirectUrlIfLocked(session: AgencyDetails): Call = routes.SubscriptionController.showEmailLocked
  override def redirectUrlIfError(session: AgencyDetails): Call =
    routes.SubscriptionController.showEmailTechnicalError
  override def backLinkUrl(session: AgencyDetails): Option[Call] =
    Some(routes.BusinessIdentificationController.showCheckBusinessEmail)
  override def enterEmailUrl(session: AgencyDetails): Call =
    routes.BusinessIdentificationController.showCheckBusinessEmail
}
