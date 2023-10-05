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

package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.services.MongoDBSessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SessionBehaviour extends CommonRouting {
  val sessionStoreService: MongoDBSessionStoreService
  implicit val ec: ExecutionContext

  val showCheckYourAnswersUrl: String =
    routes.ApplicationController.showCheckYourAnswers.url

  def updateSessionAndRedirect(agentSession: AgentSession)(redirectTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService
      .cacheAgentSession(agentSession)
      .map(_ => Redirect(redirectTo))

  def updateSession(agentSession: AgentSession)(redirectTo: String)(implicit hc: HeaderCarrier): Future[Result] =
    if (agentSession.changingAnswers) {
      updateSessionAndRedirect(agentSession.copy(changingAnswers = false))(showCheckYourAnswersUrl)
    } else {
      updateSessionAndRedirect(agentSession)(redirectTo)
    }

}
