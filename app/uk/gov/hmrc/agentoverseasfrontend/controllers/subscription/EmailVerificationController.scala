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

package uk.gov.hmrc.agentoverseasfrontend.controllers.subscription

import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Environment, Mode}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AgentOverseasBaseController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.SubscriptionAuth
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, EmailVerificationService, MongoDBSessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.utils.toFuture

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationController @Inject()(
  val env: Environment,
  authAction: SubscriptionAuth,
  sessionStoreService: MongoDBSessionStoreService,
  applicationService: ApplicationService,
  emailVerificationService: EmailVerificationService,
  cc: MessagesControllerComponents,
)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, cc) with SessionStoreHandler
    with I18nSupport {

  import authAction.withBasicAgentAuth

  def verifyEmail: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { subscriptionRequest =>
      withAgencyDetails { agencyDetails =>
        val emailToVerify = agencyDetails.agencyEmail
        agencyDetails.emailVerified match {
          // If the user has already successfully verified this email before...
          case true =>
            Redirect(routes.BusinessIdentificationController.showCheckAnswers())
          // Otherwise check the status of the email with the email verification service
          case false =>
            emailVerificationService.checkStatus(subscriptionRequest.authProviderId, emailToVerify).flatMap {
              case EmailVerificationStatus.Verified =>
                val newAgencyDetails = agencyDetails.copy(emailVerified = true)
                updateAgencyDetails(newAgencyDetails).map(_ =>
                  Redirect(routes.BusinessIdentificationController.showCheckAnswers()))
              // The email is not yet verified. Start the verification journey
              case EmailVerificationStatus.Unverified =>
                emailVerificationService
                  .verifyEmail(
                    subscriptionRequest.authProviderId,
                    Some(
                      Email(
                        address = emailToVerify,
                        enterUrl = urlFor(routes.BusinessIdentificationController.showUpdateBusinessEmailForm())
                      ),
                    ),
                    continueUrl = urlFor(routes.BusinessIdentificationController.showCheckAnswers()),
                    mBackUrl = Some(urlFor(routes.BusinessIdentificationController.showUpdateBusinessEmailForm()))
                  )
                  .map {
                    case Some(redirectUri) =>
                      val url = env.mode match {
                        case Mode.Dev => appConfig.emailVerificationFrontendBaseUrl + redirectUri
                        case _        => redirectUri // we need an absolute uri if in Dev, relative otherwise
                      }
                      Redirect(url)
                    case None => throw new RuntimeException("Could not start email verification journey")
                  }
              case EmailVerificationStatus.Locked =>
                Future.successful(Redirect(routes.SubscriptionController.cannotVerifyEmail()))
              case EmailVerificationStatus.Error =>
                Future.successful(Redirect(routes.SubscriptionController.cannotVerifyEmail()))
            }
        }
      }
    }
  }

  private def urlFor(call: Call)(implicit request: RequestHeader): String = env.mode match {
    case Mode.Dev =>
      call.absoluteURL()
    case _ =>
      call.url
  }
}
