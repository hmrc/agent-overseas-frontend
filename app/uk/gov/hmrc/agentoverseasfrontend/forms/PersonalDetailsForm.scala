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
import play.api.data.Forms.{mapping, _}
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators.{nino, radioInputSelected, saUtr}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

object PersonalDetailsForm {
  val form: Form[PersonalDetailsChoice] = Form(
    mapping(
      "personalDetailsChoice" -> optional(text).verifying(
        radioInputSelected("error.personalDetails.no-radio.selected")),
      "nino"  -> mandatoryIfEqual("personalDetailsChoice", "nino", nino),
      "saUtr" -> mandatoryIfEqual("personalDetailsChoice", "saUtr", saUtr)
    )((choice, nino, saUtr) =>
      PersonalDetailsChoice(choice.map(RadioOption.apply), nino.map(Nino.apply), saUtr.map(SaUtr.apply)))(details =>
      Some((details.choice.map(_.value), details.nino.map(_.value), details.saUtr.map(_.value))))
  )
}
