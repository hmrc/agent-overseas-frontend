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

import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.mongo.cache.CacheIdType.SimpleCacheId
import uk.gov.hmrc.mongo.cache.MongoCacheRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.TimestampSupport

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class SessionCacheRepository @Inject() (
  mongo: MongoComponent,
  timestampSupport: TimestampSupport
)(implicit
  ec: ExecutionContext,
  appConfig: AppConfig
)
extends MongoCacheRepository(
  mongoComponent = mongo,
  collectionName = "sessions",
  ttl = appConfig.mongoDbExpireAfterSeconds.seconds,
  timestampSupport = timestampSupport,
  cacheIdType = SimpleCacheId
)
