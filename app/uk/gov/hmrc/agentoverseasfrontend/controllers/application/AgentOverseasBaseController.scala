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

import javax.inject.Inject
import javax.inject.Singleton
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.MongoDBSessionStoreService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class AgentOverseasBaseController @Inject() (
  val sessionStoreService: MongoDBSessionStoreService,
  val applicationService: ApplicationService,
  override val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
extends FrontendController(controllerComponents)
with SessionBehaviour
with I18nSupport {}
