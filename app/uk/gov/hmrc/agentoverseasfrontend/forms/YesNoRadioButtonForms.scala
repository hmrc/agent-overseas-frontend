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

package uk.gov.hmrc.agentoverseasfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional}
import uk.gov.hmrc.agentoverseasfrontend.models.RadioConfirm
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators.radioInputSelected

object YesNoRadioButtonForms {

  def form(field: String, errorMsg: String): Form[RadioConfirm] = Form(
    mapping(
      field -> optional(boolean)
        .verifying(radioInputSelected(errorMsg))
        .transform(_.getOrElse(false), (Some(_)): Boolean => Option[Boolean])
    )(RadioConfirm.apply)(RadioConfirm.unapply)
  )

  val amlsRequiredForm: Form[RadioConfirm] =
    form("amlsRequired", "error.amls.required.empty")

  val removeTrnForm: Form[RadioConfirm] =
    form("isRemovingTrn", "error.removeTrn.no-radio.selected")

  val registeredForUkTaxForm: Form[RadioConfirm] =
    form("registeredForUkTax", "error.registeredForUkTaxForm.no-radio.selected")

  val registeredWithHmrcForm: Form[RadioConfirm] =
    form("registeredWithHmrc", "error.registeredWithHmrc.no-radio.selected")

  val successfulFileUploadForm: Form[RadioConfirm] =
    form("correctFile", "fileUploadTradingAddress.correctFile.no-radio.selected")
}
