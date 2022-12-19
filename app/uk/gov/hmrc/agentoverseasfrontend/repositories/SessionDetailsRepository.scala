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

package uk.gov.hmrc.agentoverseasfrontend.repositories

import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}
import org.mongodb.scala.model.Indexes.ascending

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails.SessionDetailsId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDetailsRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SessionDetails](
      mongoComponent = mongoComponent,
      collectionName = "session-details",
      domainFormat = SessionDetails.format,
      indexes = Seq(
        IndexModel(ascending("id"), IndexOptions().name("idUnique").unique(true)),
        IndexModel(ascending("createdDate"), IndexOptions().name("createDate").unique(false).expireAfter(900, SECONDS))
      )
    ) {

  def findAuthProviderId(id: SessionDetailsId): Future[Option[String]] =
    collection
      .find(Filters.equal("id", id))
      .headOption()
      .map(_.map(_.authProviderId))

  def create(authProviderId: String): Future[SessionDetailsId] = {
    val record = SessionDetails(authProviderId)
    collection
      .insertOne(record)
      .toFuture()
      .map(_ => record.id)
  }

  def delete(id: SessionDetailsId): Future[Unit] =
    collection
      .deleteOne(Filters.equal("id", id))
      .toFuture()
      .map(_ => ())

}
