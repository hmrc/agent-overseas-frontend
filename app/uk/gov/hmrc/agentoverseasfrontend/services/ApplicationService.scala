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
import uk.gov.hmrc.agentoverseasfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationEntityDetails
import uk.gov.hmrc.agentoverseasfrontend.models.CreateOverseasApplicationRequest
import uk.gov.hmrc.agentoverseasfrontend.models.FileUploadStatus

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject() (agentOverseasApplicationConnector: AgentOverseasApplicationConnector)(implicit executionContext: ExecutionContext) {

  implicit val orderingLocalDateTime: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

  def getCurrentApplication(implicit
    rh: RequestHeader
  ): Future[Option[ApplicationEntityDetails]] = agentOverseasApplicationConnector.getUserApplications.map { e =>
    e.sortBy(_.applicationCreationDate).reverse.headOption
  }

  def rejectedApplication(implicit
    rh: RequestHeader
  ): Future[Option[ApplicationEntityDetails]] = agentOverseasApplicationConnector.getUserApplications
    .map { apps =>
      if (apps.forall(_.status == Rejected))
        apps.sortBy(_.maintainerReviewedOn).reverse.headOption
      else
        None
    }

  def createApplication(application: AgentSession)(implicit
    rh: RequestHeader
  ): Future[Unit] = agentOverseasApplicationConnector.createOverseasApplication(CreateOverseasApplicationRequest(application.sanitize))

  def upscanPollStatus(reference: String)(implicit
    rh: RequestHeader
  ): Future[FileUploadStatus] = agentOverseasApplicationConnector.upscanPollStatus(reference)

}
