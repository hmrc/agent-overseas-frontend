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

package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import play.api.http.HttpVerbs.GET
import play.api.mvc.Call
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession._
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus._
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.models.No
import uk.gov.hmrc.agentoverseasfrontend.models.Yes
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait CommonRouting {

  case class StatusRouting(
    proceedTo: Call,
    initialiseAgentSession: Boolean
  )

  val sessionStoreService: SessionCacheService

  val applicationService: ApplicationService

  def lookupNextPage(agentSession: Option[AgentSession]): Call =
    agentSession match {
      case MissingAmlsRequired() => routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired
      case MissingAmlsDetails() => routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm
      case MissingAmlsUploadStatus() => routes.FileUploadController.showAmlsUploadForm
      case MissingContactDetails() => routes.ApplicationController.showContactDetailsForm
      case EmailUnverified() => routes.ApplicationEmailVerificationController.verifyEmail
      case MissingTradingName() => routes.ApplicationController.showTradingNameForm
      case MissingTradingAddress() => routes.TradingAddressController.showMainBusinessAddressForm
      case MissingTradingAddressUploadStatus() => routes.FileUploadController.showTradingAddressUploadForm
      case MissingRegisteredWithHmrc() => routes.ApplicationController.showRegisteredWithHmrcForm
      case IsRegisteredWithHmrc(Yes) => routesFromAgentCodesOnwards(agentSession)
      case IsRegisteredWithHmrc(No) => routesFromUkTaxRegistrationOnwards(agentSession)

      case _ => routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired
    }

  def routesIfExistingApplication(
    subscriptionRootPath: String
  )(implicit
    rh: RequestHeader,
    ec: ExecutionContext
  ): Future[Call] = {
    def routing: Future[StatusRouting] = applicationService.getCurrentApplication.map {
      case Some(application) if application.status == Rejected || application.status == Pending =>
        val initialiseSession = application.status == Rejected
        StatusRouting(routes.ApplicationRootController.applicationStatus, initialiseSession)
      case Some(application)
          if Set(
            Accepted,
            AttemptingRegistration,
            Registered,
            Complete
          )
            .contains(application.status) =>
        StatusRouting(Call(GET, subscriptionRootPath), initialiseAgentSession = false)
      case None => StatusRouting(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired, initialiseAgentSession = true)
    }

    for {
      proceed <- routing
      _ <-
        if (proceed.initialiseAgentSession)
          sessionStoreService.cacheAgentSession(AgentSession.empty)
        else
          Future.successful(())
    } yield proceed.proceedTo
  }

  private def routesFromAgentCodesOnwards(agentSession: Option[AgentSession]): Call =
    agentSession match {
      case MissingAgentCodes() => routes.ApplicationController.showAgentCodesForm
      case HasAnsweredAgentCodes() => routesFromUkTaxRegistrationOnwards(agentSession)
    }

  private def routesFromUkTaxRegistrationOnwards(agentSession: Option[AgentSession]): Call =
    agentSession match {
      case MissingRegisteredForUkTax() => routes.ApplicationController.showUkTaxRegistrationForm
      case IsRegisteredForUkTax(Yes) => showPersonalDetailsOrContinue(agentSession)
      case IsRegisteredForUkTax(No) => collectCompanyRegNoOrContinue(agentSession)
    }

  private def showPersonalDetailsOrContinue(agentSession: Option[AgentSession]): Call =
    agentSession match {
      case MissingPersonalDetails() => routes.ApplicationController.showPersonalDetailsForm
      case _ => collectCompanyRegNoOrContinue(agentSession)
    }

  private def collectCompanyRegNoOrContinue(agentSession: Option[AgentSession]): Call =
    agentSession match {
      case MissingPersonalDetails() => showPersonalDetailsOrContinue(agentSession)
      case MissingCompanyRegistrationNumber() => routes.ApplicationController.showCompanyRegistrationNumberForm
      case _ => collectTaxRegNoOrContinue(agentSession)
    }

  private def collectTaxRegNoOrContinue(agentSession: Option[AgentSession]): Call =
    agentSession match {
      case MissingHasTaxRegistrationNumber() => routes.TaxRegController.showTaxRegistrationNumberForm
      case HasTaxRegistrationNumber() => collectTaxRegFileUploadOrContinue(agentSession)
      case NoTaxRegistrationNumber() => routes.ApplicationController.showCheckYourAnswers
    }

  private def collectTaxRegFileUploadOrContinue(agentSession: Option[AgentSession]): Call =
    agentSession match {
      case MissingHasTaxRegistrationNumber() => collectTaxRegNoOrContinue(agentSession)
      case TaxRegistrationNumbersEmpty() => routes.TaxRegController.showAddTaxRegNoForm
      case MissingTaxRegFile() => routes.FileUploadController.showTrnUploadForm
      case _ => routes.ApplicationController.showCheckYourAnswers

    }

}
