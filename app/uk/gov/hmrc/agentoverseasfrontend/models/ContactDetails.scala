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

case class ContactDetails(
  firstName: String,
  lastName: String,
  jobTitle: String,
  businessTelephone: String,
  businessEmail: String
)

object ContactDetails {
  def contactDetailsDatabaseFormat(implicit crypto: Encrypter with Decrypter): Format[ContactDetails] =
    (
      (__ \ "firstName").format[String](stringEncrypterDecrypter) and
        (__ \ "lastName").format[String](stringEncrypterDecrypter) and
        (__ \ "jobTitle").format[String](stringEncrypterDecrypter) and
        (__ \ "businessTelephone").format[String](stringEncrypterDecrypter) and
        (__ \ "businessEmail").format[String](stringEncrypterDecrypter)
    )(ContactDetails.apply, unlift(ContactDetails.unapply))

  implicit val contactDetailsFormat: Format[ContactDetails] =
    Json.format[ContactDetails]
}
