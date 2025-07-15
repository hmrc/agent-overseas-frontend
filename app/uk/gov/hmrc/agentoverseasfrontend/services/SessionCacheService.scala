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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentoverseasfrontend.models.AgencyDetails
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionCacheRepository

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SessionCacheService @Inject() (sessionCacheRepository: SessionCacheRepository)(implicit executionContext: ExecutionContext) {

  def fetchAgentSession(implicit rh: RequestHeader): Future[Option[AgentSession]] = sessionCacheRepository.getFromSession(AgentSession.sessionKey)

  def cacheAgentSession(agentSession: AgentSession)(implicit rh: RequestHeader): Future[Unit] = sessionCacheRepository.putSession(
    AgentSession.sessionKey,
    agentSession.sanitize
  ).map(_ => ())

  def removeAgentSession(implicit rh: RequestHeader): Future[Unit] = sessionCacheRepository.deleteFromSession(AgentSession.sessionKey)

  def fetchAgencyDetails(implicit rh: RequestHeader): Future[Option[AgencyDetails]] = sessionCacheRepository.getFromSession(AgencyDetails.sessionKey)

  def cacheAgencyDetails(agencyDetails: AgencyDetails)(implicit rh: RequestHeader): Future[Unit] = sessionCacheRepository.putSession(
    AgencyDetails.sessionKey,
    agencyDetails
  ).map(_ => ())

  def removeAgencyDetails(implicit rh: RequestHeader): Future[Unit] = sessionCacheRepository.deleteFromSession(AgencyDetails.sessionKey)

}
