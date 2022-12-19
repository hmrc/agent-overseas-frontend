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

package uk.gov.hmrc.agentoverseasfrontend.services

import java.time.{LocalDateTime, ZoneOffset}

import cats.data.OptionT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.connectors.{AgentOverseasApplicationConnector, AgentSubscriptionConnector}
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus._
import uk.gov.hmrc.agentoverseasfrontend.models.{FailureToSubscribe, OverseasApplication}
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.{AlreadySubscribed, NoAgencyInSession, NoApplications, WrongApplicationStatus}
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails.SessionDetailsId
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionDetailsRepository
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionService @Inject()(
  applicationConnector: AgentOverseasApplicationConnector,
  subscriptionConnector: AgentSubscriptionConnector,
  repository: SessionDetailsRepository,
  sessionStoreService: MongoDBSessionStoreService)
    extends Logging {

  implicit val orderingLocalDateTime: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

  def subscribe(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[FailureToSubscribe, Arn]] =
    mostRecentApplicationStatus
      .flatMap {
        case Some(Accepted)              => updateAgencyDetailsOnApp()
        case Some(Registered | Complete) => Future.successful(Right(())) // Application already updated previously
        case Some(_)                     => Future.successful(Left(WrongApplicationStatus))
        case None                        => Future.successful(Left(NoApplications))
      }
      .flatMap {
        case Right(_)      => updateOverseasSubscription
        case Left(failure) => Future.successful(Left(failure))
      }

  private def updateOverseasSubscription(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Either[FailureToSubscribe, Arn]] =
    subscriptionConnector.overseasSubscription
      .map(arn => Right(arn))
      .recover {
        case ex: Upstream4xxResponse if ex.upstreamResponseCode == 409 =>
          logger.info("The user is already subscribed", ex)
          Left(AlreadySubscribed)
      }

  private def mostRecentApplicationStatus(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    mostRecentApplication.map(_.map(_.status))

  private def updateAgencyDetailsOnApp()(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Either[FailureToSubscribe, Unit]] =
    sessionStoreService.fetchAgencyDetails.flatMap {
      case Some(agency) => applicationConnector.updateApplicationWithAgencyDetails(agency).map(_ => Right(()))
      case None         => Future.successful(Left(NoAgencyInSession))
    }

  def mostRecentApplication(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OverseasApplication]] =
    applicationConnector.allApplications.map { apps =>
      apps.sortBy(_.createdDate).lastOption
    }

  def authProviderId(detailsId: SessionDetailsId): Future[Option[String]] =
    repository.findAuthProviderId(detailsId)

  def storeSessionDetails(authProviderId: String): Future[SessionDetailsId] =
    repository.create(authProviderId)

  def updateAuthProviderId(
    sessionId: SessionDetailsId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    (for {
      oldAuthId <- OptionT(repository.findAuthProviderId(sessionId))
      _         <- OptionT.liftF(applicationConnector.updateAuthId(oldAuthId))
      _         <- OptionT.liftF(repository.delete(sessionId))
    } yield ()).value.map(_ => ())
}
