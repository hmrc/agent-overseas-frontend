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

import uk.gov.hmrc.agentoverseasfrontend.models.OverseasAddress

case class BusinessAddressForm(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  countryCode: String)

object BusinessAddressForm {
  def apply(businessAddress: OverseasAddress): BusinessAddressForm =
    BusinessAddressForm(
      businessAddress.addressLine1,
      businessAddress.addressLine2,
      businessAddress.addressLine3,
      businessAddress.addressLine4,
      businessAddress.countryCode
    )
}
