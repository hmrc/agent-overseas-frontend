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

package uk.gov.hmrc.agentoverseasfrontend.controllers.subscription

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.AgentOverseasBaseController
import uk.gov.hmrc.agentoverseasfrontend.controllers.auth.SubscriptionAuth
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, MongoDBSessionStoreService, SubscriptionService}
import uk.gov.hmrc.agentoverseasfrontend.views.html.subscription._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionRootController @Inject()(
  override val messagesApi: MessagesApi,
  service: SubscriptionService,
  authAction: SubscriptionAuth,
  sessionStoreService: MongoDBSessionStoreService,
  applicationService: ApplicationService,
  mcc: MessagesControllerComponents,
  createNewAccountView: create_new_account,
  cannotCheckStatusView: cannot_check_status)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext,
  configuration: Configuration)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, mcc) {

  import authAction.withBasicAgentAuth

  def root: Action[AnyContent] = Action {
    Redirect(routes.BusinessIdentificationController.showCheckAnswers)
  }

  def nextStep: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { subRequest =>
      Future.successful(Ok(createNewAccountView()))
    }
  }

  def showApplicationIssue: Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth { subRequest =>
      Future.successful(Ok(cannotCheckStatusView()))
    }
  }
}
