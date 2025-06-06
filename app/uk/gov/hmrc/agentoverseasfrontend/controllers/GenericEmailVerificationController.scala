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

package uk.gov.hmrc.agentoverseasfrontend.controllers

import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.Environment
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

abstract class GenericEmailVerificationController[S](
  val env: Environment,
  emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
extends FrontendBaseController
with I18nSupport {

  def emailVerificationEnabled: Boolean

  def emailVerificationFrontendBaseUrl: String
  def accessibilityStatementUrl(implicit request: RequestHeader): String

  // if we are running locally, each service will have a different root URL so we need to use absolute URLs
  // to redirect between calling service and email verification service
  def useAbsoluteUrls: Boolean = emailVerificationFrontendBaseUrl.contains("localhost")

  /*
  State methods
   */

  /** Returns the session state and the credId of the current logged in user.
    */
  def getState(implicit hc: HeaderCarrier): Future[(S, String)]

  /** Extract the email to be verified from the current session state.
    */
  def getEmailToVerify(session: S): String

  /** Check whether the email has already been marked as verified in the current session state.
    */
  def isAlreadyVerified(
    session: S,
    email: String
  ): Boolean

  /** An effectful call to mark the email as being verified in our session. Should return the new session state. This function is expected to be idempotent
    * (marking the same email as verified twice should not lead to unexpected results)
    */
  def markEmailAsVerified(
    session: S,
    email: String
  )(implicit hc: HeaderCarrier): Future[S]

  /*
  Continuation URLs
   */

  /** @return
    *   The Call that will hit the main method in this controller
    */
  def selfRoute: Call

  /** User will be sent here if their email is confirmed verified (or was already verified)
    */
  def redirectUrlIfVerified(session: S): Call

  /** User will be sent here if their email is locked out due to too many failed verifications
    */
  def redirectUrlIfLocked(session: S): Call

  /** User will be sent here in case of unexpected errors during verification
    */
  def redirectUrlIfError(session: S): Call

  /** The URL for the back link displayed on the verification page
    */
  def backLinkUrl(session: S): Option[Call]

  /** User may be sent here if they need to re-enter their email
    */
  def enterEmailUrl(session: S): Call

  def verifyEmail: Action[AnyContent] = Action.async { implicit request =>
    getState.flatMap { case (session, credId) =>
      val emailToVerify = getEmailToVerify(session)
      if (isAlreadyVerified(session, emailToVerify) || !emailVerificationEnabled) {
        markEmailAsVerified(session, emailToVerify).map { updatedSession =>
          Redirect(redirectUrlIfVerified(updatedSession))
        }
      }
      else {
        // Check the status of the email with the email verification service
        emailVerificationService.checkStatus(credId, emailToVerify).flatMap {
          case EmailVerificationStatus.Verified =>
            markEmailAsVerified(session, emailToVerify).map { updatedSession =>
              Redirect(redirectUrlIfVerified(updatedSession))
            }
          // The email is not yet verified. Start the verification journey
          case EmailVerificationStatus.Unverified =>
            emailVerificationService
              .verifyEmail(
                credId,
                Some(
                  Email(
                    address = emailToVerify,
                    enterUrl = urlFor(enterEmailUrl(session))
                  )
                ),
                continueUrl = urlFor(
                  selfRoute
                ), // when the verification journey is done, this same method will be called again to verify that the email is indeed verified
                mBackUrl = backLinkUrl(session).map(urlFor(_)),
                accessibilityStatementUrl = accessibilityStatementUrl
              )
              .map {
                case Some(redirectUri) =>
                  val url =
                    if (useAbsoluteUrls)
                      emailVerificationFrontendBaseUrl + redirectUri
                    else
                      redirectUri
                  Redirect(url)
                case None => throw new RuntimeException("Could not start email verification journey")
              }
          // The email provided was locked out due to too many failed verification attempts
          case EmailVerificationStatus.Locked => Future.successful(Redirect(redirectUrlIfLocked(session)))
          // Any other error
          case EmailVerificationStatus.Error => Future.successful(Redirect(redirectUrlIfError(session)))
        }
      }

    }
  }

  private def urlFor(call: Call)(implicit request: RequestHeader): String =
    if (useAbsoluteUrls)
      call.absoluteURL()
    else
      call.url

}
