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

package uk.gov.hmrc.agentoverseasfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasfrontend.models.AgentCodes
import uk.gov.hmrc.agentoverseasfrontend.models.CtAgentCode
import uk.gov.hmrc.agentoverseasfrontend.models.SaAgentCode
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators._

object AgentCodesForm {

  def form: Form[AgentCodes] = Form[AgentCodes](
    mapping(
      "self-assessment-checkbox" -> boolean,
      "self-assessment" -> mandatoryIfTrue("self-assessment-checkbox", saAgentCode),
      "corporation-tax-checkbox" -> boolean,
      "corporation-tax" -> mandatoryIfTrue("corporation-tax-checkbox", ctAgentCode)
    )(
      (
        hasSa,
        sa,
        hasCt,
        ct
      ) =>
        AgentCodes(
          sa.collect { case x if hasSa => SaAgentCode(x) },
          ct.collect { case x if hasCt => CtAgentCode(x) }
        )
    )((codes: AgentCodes) =>
      Some(
        (
          codes.selfAssessment.isDefined,
          codes.selfAssessment.map(_.value),
          codes.corporationTax.isDefined,
          codes.corporationTax.map(_.value)
        )
      )
    )
  )
}
