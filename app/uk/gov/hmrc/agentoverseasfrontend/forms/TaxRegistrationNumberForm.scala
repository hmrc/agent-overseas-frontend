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
import play.api.data.Forms.boolean
import play.api.data.Forms.mapping
import play.api.data.Forms.optional
import uk.gov.hmrc.agentoverseasfrontend.models.TaxRegistrationNumber
import uk.gov.hmrc.agentoverseasfrontend.models.Trn
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators.radioInputSelected
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

object TaxRegistrationNumberForm {

  def form: Form[TaxRegistrationNumber] = Form[TaxRegistrationNumber](
    mapping(
      "canProvideTaxRegNo" -> optional(boolean).verifying(radioInputSelected("taxRegNo.form.no-radio.selected")),
      "value" -> mandatoryIfTrue("canProvideTaxRegNo", CommonValidators.taxRegistrationNumber)
    )(
      (
        canProvideTaxRegNo,
        value
      ) => TaxRegistrationNumber(canProvideTaxRegNo, value.map(Trn.apply))
    )(taxRegNo =>
      Some((taxRegNo.canProvideTaxRegNo, taxRegNo.value.map(_.value)))
    )
  )
}
