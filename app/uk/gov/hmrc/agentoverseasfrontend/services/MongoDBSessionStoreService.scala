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

import play.api.libs.json.Format
import uk.gov.hmrc.agentoverseasfrontend.models.{AgencyDetails, AgentSession}
import uk.gov.hmrc.agentoverseasfrontend.repositories.{SessionCache, SessionCacheRepository}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MongoDBSessionStoreService @Inject() (val sessionCache: SessionCacheRepository)(implicit
  @Named("aes") crypto: Encrypter with Decrypter
) {

  final val agentSessionCache = new SessionCache[AgentSession] {
    override val sessionName: String = "agentSession"
    override val cacheRepository: SessionCacheRepository = sessionCache
  }

  final val agencyDetailsCache = new SessionCache[AgencyDetails] {
    override val sessionName: String = "agencyDetails"
    override val cacheRepository: SessionCacheRepository = sessionCache
  }

  implicit val agencyDetailsFormat: Format[AgencyDetails] = AgencyDetails.agencyDetailsDatabaseFormat
  implicit val agentSessionFormat: Format[AgentSession] = AgentSession.agentSessionDatabaseFormat

  def fetchAgentSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentSession]] =
    agentSessionCache.fetch

  def cacheAgentSession(agentSession: AgentSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    agentSessionCache.save(agentSession.sanitize).map(_ => ())

  def removeAgentSession(implicit hc: HeaderCarrier): Future[Unit] =
    hc.sessionId
      .map(_.value)
      .map(id => sessionCache.delete(id)(DataKey[AgentSession]("agentSession")))
      .getOrElse(Future.successful(()))

  def fetchAgencyDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgencyDetails]] =
    agencyDetailsCache.fetch

  def cacheAgencyDetails(agencyDetails: AgencyDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    agencyDetailsCache.save(agencyDetails).map(_ => ())

  def remove()(implicit hc: HeaderCarrier): Future[Unit] =
    hc.sessionId
      .map(_.value)
      .map(id => sessionCache.delete(id)(DataKey[AgencyDetails]("agencyDetails")))
      .getOrElse(Future.successful(()))
}
