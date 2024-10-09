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

package uk.gov.hmrc.agentoverseasfrontend.models

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, Json, __}
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

case class OverseasAddress(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  countryCode: String
)

object OverseasAddress {
  def overseasAddressDatabaseFormat(implicit crypto: Encrypter with Decrypter): Format[OverseasAddress] =
    (
      (__ \ "addressLine1").format[String](stringEncrypterDecrypter) and
        (__ \ "addressLine2").format[String](stringEncrypterDecrypter) and
        (__ \ "addressLine3").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "addressLine4").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "countryCode").format[String](stringEncrypterDecrypter)
    )(OverseasAddress.apply, unlift(OverseasAddress.unapply))

  implicit val format: Format[OverseasAddress] = Json.format[OverseasAddress]
}
