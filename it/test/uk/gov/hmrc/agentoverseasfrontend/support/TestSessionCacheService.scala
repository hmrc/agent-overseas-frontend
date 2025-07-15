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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentoverseasfrontend.models.AgencyDetails
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.models.ProviderId
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.agentoverseasfrontend.utils.RequestSupport._

import scala.concurrent.Future

//This should probably be removed, we can simply rely on the actual mongo repository in integration tests
class TestSessionCacheService
extends SessionCacheService(null)(null) {

  class Session(
    var agentSession: Option[AgentSession] = None,
    var agencyDetails: Option[AgencyDetails] = None,
    var providerId: Option[ProviderId] = None
  )

  private val sessions = collection.mutable.Map[String, Session]()

  private def sessionKey(implicit rc: RequestHeader): String =
    hc.sessionId match {
      case None => "default"
      case Some(sessionId) => sessionId.toString
    }

  def currentSession(implicit rc: RequestHeader): Session = sessions.getOrElseUpdate(sessionKey, new Session())

  def clear(): Unit = sessions.clear()

  def allSessionsRemoved: Boolean = sessions.isEmpty

  override def fetchAgentSession(implicit
    rc: RequestHeader
  ): Future[Option[AgentSession]] = Future.successful(currentSession.agentSession)

  override def cacheAgentSession(
    agentSession: AgentSession
  )(implicit rc: RequestHeader): Future[Unit] = Future.successful(currentSession.agentSession = Some(agentSession))

  override def removeAgentSession(implicit rc: RequestHeader): Future[Unit] = Future.successful(sessions.clear())

  override def fetchAgencyDetails(implicit rc: RequestHeader): Future[Option[AgencyDetails]] = Future successful currentSession.agencyDetails

  override def cacheAgencyDetails(
    agencyDetails: AgencyDetails
  )(implicit rc: RequestHeader): Future[Unit] = Future.successful(currentSession.agencyDetails = Some(agencyDetails))

  override def removeAgencyDetails(implicit rc: RequestHeader): Future[Unit] = Future.successful {
    sessions.clear()
    ()
  }

  override def fetchOldProviderId(
    oldSessionId: String,
    rh: RequestHeader
  ): Future[Option[ProviderId]] = Future.successful(currentSession(changeHeaderSessionId(oldSessionId, rh)).providerId)

  override def cacheProviderId(providerId: ProviderId)(implicit rh: RequestHeader): Future[Unit] = Future.successful(
    currentSession.providerId = Some(providerId)
  )

  override def removeOldProviderId(
    oldSessionId: String,
    rh: RequestHeader
  ): Future[Unit] = Future.successful(sessions.clear())

}
