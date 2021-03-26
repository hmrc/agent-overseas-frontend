/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.Logger
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.agentoverseasfrontend.models.{AgencyDetails, OverseasApplication}
import uk.gov.hmrc.agentoverseasfrontend.services.MongoDBSessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SessionStoreHandler {
  this: Results =>

  val sessionStoreService: MongoDBSessionStoreService

  def withAgencyDetailsOrWithNewDefaults(defaultsFromThisApplication: OverseasApplication)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[AgencyDetails] =
    sessionStoreService.fetchAgencyDetails
      .flatMap {
        case Some(agencyDetails) => Future.successful(agencyDetails)
        case None =>
          val defaultAgencyDetails = AgencyDetails(defaultsFromThisApplication)
          updateAgencyDetails(defaultAgencyDetails).map(_ => defaultAgencyDetails)
      }

  def withAgencyDetails(
    body: AgencyDetails => Future[Result])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    sessionStoreService.fetchAgencyDetails.flatMap(
      _.map(body)
        .getOrElse(Future.successful(sessionMissingRedirect("AgencyDetails"))))

  def updateAgencyDetails(
    agencyDetails: AgencyDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionStoreService.cacheAgencyDetails(agencyDetails)

  private def sessionMissingRedirect(missingSessionItem: String): Result = {
    Logger(getClass).warn(s"Missing $missingSessionItem in session or keystore, redirecting back to /check-answers")
    Redirect(routes.BusinessIdentificationController.showCheckAnswers())
  }

}
