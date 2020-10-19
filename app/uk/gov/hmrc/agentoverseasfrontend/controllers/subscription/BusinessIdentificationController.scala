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
import uk.gov.hmrc.agentoverseasfrontend.models.OverseasAddress
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService, SubscriptionService}
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators._
import uk.gov.hmrc.agentoverseasfrontend.views.html.subscription._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessIdentificationController @Inject()(
  authAction: SubscriptionAuth,
  countryNamesLoader: CountryNamesLoader,
  subscriptionService: SubscriptionService,
  mcc: MessagesControllerComponents,
  applicationService: ApplicationService,
  override val sessionStoreService: SessionStoreService,
  checkAnswersView: check_answers,
  updateBusinessAddressView: update_business_address,
  updateBusinessNameView: update_business_name,
  updateBusinessEmailView: update_business_email
)(implicit override val ec: ExecutionContext, appConfig: AppConfig)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, mcc) with SessionStoreHandler {

  private lazy val countries = countryNamesLoader.load
  private lazy val validCountryCodes = countries.keys.toSet

  import authAction.{config, withBasicAgentAuth, withSubscribingAgent}

  def showCheckAnswers: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { overseasApplication =>
      withAgencyDetailsOrWithNewDefaults(overseasApplication).map { agencyDetails =>
        val countryCode = agencyDetails.agencyAddress.countryCode
        val countryName = countries.getOrElse(
          countryCode,
          throw new RuntimeException(s"The application's stored countryCode: `$countryCode` is unknown"))

        Ok(checkAnswersView(agencyDetails, countryName))
      }
    }
  }

  def showUpdateBusinessAddressForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { overseasApplication =>
      withAgencyDetails { agencyDetails =>
        Future.successful(
          Ok(
            updateBusinessAddressView(
              updateBusinessAddressForm(validCountryCodes).fill(BusinessAddressForm(agencyDetails.agencyAddress)),
              countries)))
      }
    }
  }

  def submitUpdateBusinessAddressForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { overseasApplication =>
      withAgencyDetails { agencyDetails =>
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

              updateAgencyDetails(agencyWithUpdatedAddress).map(_ =>
                Redirect(routes.BusinessIdentificationController.showCheckAnswers()))
            }
          )
      }
    }
  }

  def showUpdateBusinessEmailForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { overseasApplication =>
      withAgencyDetails { agencyDetails =>
        Future.successful(
          Ok(updateBusinessEmailView(updateBusinessEmailForm.fill(BusinessEmailForm(agencyDetails.agencyEmail)))))
      }
    }
  }

  def submitUpdateBusinessEmailForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { overseasApplication =>
      withAgencyDetails { agencyDetails =>
        updateBusinessEmailForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(Ok(updateBusinessEmailView(formWithErrors))),
            validForm => {
              val agencyWithUpdatedEmail = agencyDetails.copy(agencyEmail = validForm.email)

              updateAgencyDetails(agencyWithUpdatedEmail).map(_ =>
                Redirect(routes.BusinessIdentificationController.showCheckAnswers()))
            }
          )
      }
    }
  }

  def showUpdateBusinessNameForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { overseasApplication =>
      withAgencyDetails { agencyDetails =>
        Future.successful(
          Ok(updateBusinessNameView(updateBusinessNameForm.fill(BusinessNameForm(agencyDetails.agencyName)))))
      }
    }
  }

  def submitUpdateBusinessNameForm: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { overseasApplication =>
      withAgencyDetails { agencyDetails =>
        updateBusinessNameForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(Ok(updateBusinessNameView(formWithErrors))),
            validForm => {
              val agencyWithUpdatedName = agencyDetails.copy(agencyName = validForm.name)

              updateAgencyDetails(agencyWithUpdatedName).map(_ =>
                Redirect(routes.BusinessIdentificationController.showCheckAnswers()))
            }
          )
      }
    }
  }

  def returnFromGGRegistration(sessionId: String): Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { _ =>
      subscriptionService
        .updateAuthProviderId(sessionId)
        .map(_ => Redirect(routes.BusinessIdentificationController.showCheckAnswers()))
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
      )(BusinessAddressForm.apply)(BusinessAddressForm.unapply))

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
