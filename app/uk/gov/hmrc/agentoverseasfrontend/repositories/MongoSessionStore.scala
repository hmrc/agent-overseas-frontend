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

package uk.gov.hmrc.agentoverseasfrontend.repositories

import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait MongoSessionStore[T]
extends Logging {

  val sessionName: String
  val cacheRepository: SessionCacheRepository

  private def getSessionId(implicit hc: HeaderCarrier): Option[String] = hc.sessionId.map(_.value)

  def get(implicit
    reads: Reads[T],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[String, Option[T]]] =
    getSessionId match {
      case Some(sessionId) =>
        cacheRepository
          .findById(sessionId)
          .map {
            case Some(entity) => Right((entity.data \ sessionName).asOpt[T])
            case _ => Right(None)
          }
          .recover { case e: Exception => Left(e.getMessage) }
      case None => Future successful Right(None)
    }

  def store(
    newSession: T
  )(implicit
    writes: Writes[T],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[String, Unit]] =
    getSessionId match {
      case Some(sessionId) =>
        cacheRepository
          .put(sessionId)(DataKey[T](sessionName), newSession)
          .map(_ => Right(()))
          .recover { case e: Exception => Left(e.getMessage) }
      case None => Future successful Left("Could not store session as no session Id found.")
    }

}
