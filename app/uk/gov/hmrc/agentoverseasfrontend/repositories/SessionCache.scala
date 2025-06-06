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
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentoverseasfrontend.utils.toFuture

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait SessionCache[T]
extends MongoSessionStore[T]
with Logging {

  def fetch(implicit
    hc: HeaderCarrier,
    reads: Reads[T],
    ec: ExecutionContext
  ): Future[Option[T]] = get.flatMap {
    case Right(input) => input
    case Left(error) =>
      logger.warn(error)
      Future.failed(new RuntimeException(error))
  }

  def save(input: T)(implicit
    hc: HeaderCarrier,
    writes: Writes[T],
    ec: ExecutionContext
  ): Future[T] = store(input).flatMap {
    case Right(_) => input
    case Left(error) =>
      logger.warn(error)
      Future.failed(new RuntimeException(error))
  }

}
