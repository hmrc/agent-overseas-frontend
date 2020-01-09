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

package uk.gov.hmrc.agentoverseasfrontend.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionStoreService @Inject()(val sessionCache: SessionCache) {

  def fetchAgentSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentSession]] =
    sessionCache.fetchAndGetEntry[AgentSession]("agentSession")

  def cacheAgentSession(agentSession: AgentSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionCache.cache("agentSession", agentSession.sanitize).map(_ => ())

  def removeAgentSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionCache.remove().map(_ => ())
}
