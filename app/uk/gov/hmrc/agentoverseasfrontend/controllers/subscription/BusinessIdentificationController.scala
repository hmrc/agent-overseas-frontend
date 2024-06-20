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

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentoverseasfrontend.config.{AppConfig, CountryNamesLoader}
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AgentOverseasBaseController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.SubscriptionAuth
import uk.gov.hmrc.agentoverseasfrontend.controllers.subscription.BusinessIdentificationController._
import uk.gov.hmrc.agentoverseasfrontend.forms.{BusinessAddressForm, BusinessEmailForm, BusinessNameForm}
import uk.gov.hmrc.agentoverseasfrontend.forms.YesNoRadioButtonForms._
import uk.gov.hmrc.agentoverseasfrontend.models.OverseasAddress
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, MongoDBSessionStoreService, SubscriptionService}
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators._
import uk.gov.hmrc.agentoverseasfrontend.views.html.subscription._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessIdentificationController @Inject() (
  authAction: SubscriptionAuth,
  countryNamesLoader: CountryNamesLoader,
  subscriptionService: SubscriptionService,
  mcc: MessagesControllerComponents,
  applicationService: ApplicationService,
  override val sessionStoreService: MongoDBSessionStoreService,
  checkAnswersView: check_answers,
  checkBusinessAddressView: check_business_address,
  updateBusinessAddressView: update_business_address,
  checkBusinessNameView: check_business_name,
  updateBusinessNameView: update_business_name,
  checkBusinessEmailView: check_business_email,
  updateBusinessEmailView: update_business_email
)(implicit override val ec: ExecutionContext, appConfig: AppConfig)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, mcc) {

  private lazy val countries = countryNamesLoader.load
  private lazy val validCountryCodes = countries.keys.toSet

  import authAction.{config, withBasicAgentAuth, withSubscribingAgent}

  def showCheckAnswers: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true, generateNewDetailsIfNoSession = true) { agencyDetails =>
      val countryCode = agencyDetails.agencyAddress.countryCode
      val countryName = countries.getOrElse(
        countryCode,
        throw new RuntimeException(s"The application's stored countryCode: `$countryCode` is unknown")
      )

      Future.successful(Ok(checkAnswersView(agencyDetails, countryName)))
    }
  }

  def showCheckBusinessAddress: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      val countryCode = agencyDetails.agencyAddress.countryCode
      val countryName = countries.getOrElse(
        countryCode,
        throw new RuntimeException(s"The application's stored countryCode: `$countryCode` is unknown")
      )
      Future.successful(
        Ok(checkBusinessAddressView(businessAddressCheckForm, agencyDetails.agencyAddress, countryName))
      )
    }
  }

  def submitCheckBusinessAddress: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      businessAddressCheckForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val countryCode = agencyDetails.agencyAddress.countryCode
            val countryName = countries.getOrElse(
              countryCode,
              throw new RuntimeException(s"The application's stored countryCode: `$countryCode` is unknown")
            )
            Future.successful(Ok(checkBusinessAddressView(formWithErrors, agencyDetails.agencyAddress, countryName)))
          },
          validForm => {
            val useCurrentAddress = validForm.value
            if (useCurrentAddress)
              Future.successful(Redirect(routes.BusinessIdentificationController.showCheckAnswers))
            else
              Future.successful(Redirect(routes.BusinessIdentificationController.showUpdateBusinessAddressForm))
          }
        )
    }
  }

  def showUpdateBusinessAddressForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      Future.successful(
        Ok(
          updateBusinessAddressView(
            updateBusinessAddressForm(validCountryCodes).fill(BusinessAddressForm(agencyDetails.agencyAddress)),
            countries
          )
        )
      )
    }
  }

  def submitUpdateBusinessAddressForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      updateBusinessAddressForm(validCountryCodes)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(updateBusinessAddressView(formWithErrors, countries))),
          validForm => {
            val updatedAddress = OverseasAddress(
              addressLine1 = validForm.addressLine1,
              addressLine2 = validForm.addressLine2,
              addressLine3 = validForm.addressLine3,
              addressLine4 = validForm.addressLine4,
              countryCode = validForm.countryCode
            )
            val agencyWithUpdatedAddress = agencyDetails.copy(agencyAddress = updatedAddress)

            sessionStoreService
              .cacheAgencyDetails(agencyWithUpdatedAddress)
              .map(_ => Redirect(routes.BusinessIdentificationController.showCheckAnswers))
          }
        )
    }
  }

  def showCheckBusinessEmail: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = false) { agencyDetails =>
      Future.successful(Ok(checkBusinessEmailView(businessEmailCheckForm, agencyDetails.agencyEmail)))
    }
  }

  def submitCheckBusinessEmail: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = false) { agencyDetails =>
      businessEmailCheckForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(checkBusinessEmailView(formWithErrors, agencyDetails.agencyEmail))),
          validForm => {
            val useCurrentEmail = validForm.value
            if (useCurrentEmail)
              Future.successful(Redirect(routes.BusinessIdentificationController.showCheckAnswers))
            else
              Future.successful(Redirect(routes.BusinessIdentificationController.showUpdateBusinessEmailForm))
          }
        )
    }
  }

  def showUpdateBusinessEmailForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = false) { agencyDetails =>
      Future.successful(
        Ok(updateBusinessEmailView(updateBusinessEmailForm.fill(BusinessEmailForm(agencyDetails.agencyEmail))))
      )
    }
  }

  def submitUpdateBusinessEmailForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = false) { agencyDetails =>
      updateBusinessEmailForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(updateBusinessEmailView(formWithErrors))),
          validForm => {
            val agencyWithUpdatedEmail = agencyDetails.copy(agencyEmail = validForm.email)

            sessionStoreService
              .cacheAgencyDetails(agencyWithUpdatedEmail)
              .map(_ => Redirect(routes.BusinessIdentificationController.showCheckAnswers))
          }
        )
    }
  }

  def showCheckBusinessName: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      Future.successful(Ok(checkBusinessNameView(businessNameCheckForm, agencyDetails.agencyName)))
    }
  }

  def submitCheckBusinessName: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      businessNameCheckForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(checkBusinessNameView(formWithErrors, agencyDetails.agencyName))),
          validForm => {
            val useCurrentName = validForm.value
            if (useCurrentName)
              Future.successful(Redirect(routes.BusinessIdentificationController.showCheckAnswers))
            else
              Future.successful(Redirect(routes.BusinessIdentificationController.showUpdateBusinessNameForm))
          }
        )
    }
  }

  def showUpdateBusinessNameForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      Future.successful(
        Ok(updateBusinessNameView(updateBusinessNameForm.fill(BusinessNameForm(agencyDetails.agencyName))))
      )
    }
  }

  def submitUpdateBusinessNameForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(checkForEmailVerification = true) { agencyDetails =>
      updateBusinessNameForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(updateBusinessNameView(formWithErrors))),
          validForm => {
            val agencyWithUpdatedName = agencyDetails.copy(agencyName = validForm.name)

            sessionStoreService
              .cacheAgencyDetails(agencyWithUpdatedName)
              .map(_ => Redirect(routes.BusinessIdentificationController.showCheckAnswers))
          }
        )
    }
  }

  def returnFromGGRegistration(sessionId: String): Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { _ =>
      subscriptionService
        .updateAuthProviderId(sessionId)
        .map(_ => Redirect(routes.BusinessIdentificationController.showCheckAnswers))
    }
  }
}

object BusinessIdentificationController {
  def updateBusinessAddressForm(validCountryCodes: Set[String]): Form[BusinessAddressForm] =
    Form[BusinessAddressForm](
      mapping(
        "addressLine1" -> addressLine12(lineNumber = 1),
        "addressLine2" -> addressLine12(lineNumber = 2),
        "addressLine3" -> addressLine34(lineNumber = 3),
        "addressLine4" -> addressLine34(lineNumber = 4),
        "countryCode"  -> countryCode(validCountryCodes)
      )(BusinessAddressForm.apply)(BusinessAddressForm.unapply)
    )

  val updateBusinessEmailForm: Form[BusinessEmailForm] = Form[BusinessEmailForm](
    mapping(
      "email" -> emailAddress
    )(BusinessEmailForm.apply)(BusinessEmailForm.unapply)
  )

  val updateBusinessNameForm: Form[BusinessNameForm] = Form[BusinessNameForm](
    mapping(
      "name" -> businessName
    )(BusinessNameForm.apply)(BusinessNameForm.unapply)
  )
}
