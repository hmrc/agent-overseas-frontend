/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.agentoverseasfrontend.models.OverseasAddress
import uk.gov.hmrc.agentoverseasfrontend.validators.CommonValidators._

object MainBusinessAddressForm {

  def mainBusinessAddressForm(validCountryCodes: Set[String]): Form[OverseasAddress] =
    Form[OverseasAddress](
      mapping(
        "addressLine1" -> addressLine12(lineNumber = 1),
        "addressLine2" -> addressLine12(lineNumber = 2),
        "addressLine3" -> addressLine34(lineNumber = 3),
        "addressLine4" -> addressLine34(lineNumber = 4),
        "countryCode"  -> countryCode(validCountryCodes)
      )(OverseasAddress.apply)(OverseasAddress.unapply))
}
