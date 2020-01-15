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

package uk.gov.hmrc.agentoverseasfrontend.repositories

import javax.inject.{Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails.SessionDetailsId
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDetailsRepository @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[SessionDetails, BSONObjectID](
      "session-details",
      mongoComponent.mongoConnector.db,
      SessionDetails.format,
      ReactiveMongoFormats.objectIdFormats) {

  override def indexes: Seq[Index] =
    Seq(
      Index(key = Seq("id" -> IndexType.Ascending), name = Some("idUnique"), unique = true),
      Index(
        key = Seq("createdDate" -> IndexType.Ascending),
        name = Some("createDate"),
        unique = false,
        options = BSONDocument("expireAfterSeconds" -> 900)
      )
    )

  def findAuthProviderId(id: SessionDetailsId)(implicit ec: ExecutionContext): Future[Option[String]] =
    find("id" -> id).map(_.headOption.map(_.authProviderId))

  def create(authProviderId: String)(implicit ec: ExecutionContext): Future[SessionDetailsId] = {
    val record = SessionDetails(authProviderId)
    insert(record).map(_ => record.id)
  }

  def delete(id: SessionDetailsId)(implicit ec: ExecutionContext): Future[Unit] =
    remove("id" -> id).map(_ => ())
}
