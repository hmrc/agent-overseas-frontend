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

package uk.gov.hmrc.agentoverseasfrontend.controllers.application

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasfrontend.views.html.application._

import scala.concurrent.ExecutionContext

@Singleton class AccessibilityStatementController @Inject()(
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  controllerComponents: MessagesControllerComponents,
  accessibilityStatementView: accessibility_statement)(implicit appConfig: AppConfig, override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService, controllerComponents)
    with SessionBehaviour with I18nSupport {

  def showAccessibilityStatement: Action[AnyContent] = Action { implicit request =>
    val userAction: String =
      request.headers.get(HeaderNames.REFERER).getOrElse("")
    val accessibilityUrl: String = s"${appConfig.accessibilityUrl}$userAction"
    Ok(accessibilityStatementView(accessibilityUrl))
  }
}
