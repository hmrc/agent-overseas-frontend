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

package uk.gov.hmrc.agentoverseasfrontend.services

import cats.data.OptionT
import cats.implicits._
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentoverseasfrontend.models.Arn
import uk.gov.hmrc.agentoverseasfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasfrontend.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus._
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.AlreadySubscribed
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.NoAgencyInSession
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.NoApplications
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.WrongApplicationStatus
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe
import uk.gov.hmrc.agentoverseasfrontend.models.OverseasApplication
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject() (
  applicationConnector: AgentOverseasApplicationConnector,
  subscriptionConnector: AgentSubscriptionConnector,
  sessionStoreService: SessionCacheService
)(implicit executionContext: ExecutionContext)
extends Logging {

  implicit val orderingLocalDateTime: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

  def subscribe(implicit
    rh: RequestHeader
  ): Future[Either[FailureToSubscribe, Arn]] = mostRecentApplicationStatus
    .flatMap {
      case Some(Accepted) => updateAgencyDetailsOnApp()
      case Some(Registered | Complete) => Future.successful(Right(())) // Application already updated previously
      case Some(_) => Future.successful(Left(WrongApplicationStatus))
      case None => Future.successful(Left(NoApplications))
    }
    .flatMap {
      case Right(_) => updateOverseasSubscription
      case Left(failure) => Future.successful(Left(failure))
    }

  private def updateOverseasSubscription(implicit
    rh: RequestHeader
  ): Future[Either[FailureToSubscribe, Arn]] = subscriptionConnector.overseasSubscription
    .map(arn => Right(arn))
    .recover {
      case ex: UpstreamErrorResponse if ex.statusCode == 409 =>
        logger.info("The user is already subscribed", ex)
        Left(AlreadySubscribed)
    }

  private def mostRecentApplicationStatus(implicit
    rh: RequestHeader
  ) = mostRecentApplication.map(_.map(_.status))

  private def updateAgencyDetailsOnApp()(implicit
    rh: RequestHeader
  ): Future[Either[FailureToSubscribe, Unit]] = sessionStoreService.fetchAgencyDetails.flatMap {
    case Some(agency) => applicationConnector.updateApplicationWithAgencyDetails(agency).map(_ => Right(()))
    case None => Future.successful(Left(NoAgencyInSession))
  }

  def mostRecentApplication(implicit
    rh: RequestHeader
  ): Future[Option[OverseasApplication]] = applicationConnector.allApplications.map { apps =>
    apps.sortBy(_.createdDate).lastOption
  }

  def updateAuthProviderId(
    sessionId: String
  )(implicit
    rh: RequestHeader
  ): Future[Unit] =
    (for {
      oldAuthId <- OptionT(sessionStoreService.fetchOldProviderId(sessionId, rh))
      _ <- OptionT.liftF(applicationConnector.updateAuthId(oldAuthId))
      _ <- OptionT.liftF(sessionStoreService.removeOldProviderId(sessionId, rh))
    } yield ()).value.map(_ => ())

}
