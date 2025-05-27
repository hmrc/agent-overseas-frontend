/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasfrontend.support

import uk.gov.hmrc.agentoverseasfrontend.models.AgencyDetails
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.services.MongoDBSessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TestSessionStoreService
extends MongoDBSessionStoreService(null)(null) {

  class Session(
    var agentSession: Option[AgentSession] = None,
    var agencyDetails: Option[AgencyDetails] = None
  )

  private val sessions = collection.mutable.Map[String, Session]()

  private def sessionKey(implicit hc: HeaderCarrier): String =
    hc.sessionId match {
      case None => "default"
      case Some(sessionId) => sessionId.toString
    }

  def currentSession(implicit hc: HeaderCarrier): Session = sessions.getOrElseUpdate(sessionKey, new Session())

  def clear(): Unit = sessions.clear()

  def allSessionsRemoved: Boolean = sessions.isEmpty

  override def fetchAgentSession(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[AgentSession]] = Future.successful(currentSession.agentSession)

  override def cacheAgentSession(
    agentSession: AgentSession
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = Future.successful(currentSession.agentSession = Some(agentSession))

  override def removeAgentSession(implicit hc: HeaderCarrier) = Future.successful(sessions.clear())

  override def fetchAgencyDetails(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[AgencyDetails]] = Future successful currentSession.agencyDetails

  override def cacheAgencyDetails(
    agencyDetails: AgencyDetails
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = Future.successful(currentSession.agencyDetails = Some(agencyDetails))

  override def remove()(implicit hc: HeaderCarrier): Future[Unit] = Future.successful {
    sessions.clear()
    ()
  }

}
