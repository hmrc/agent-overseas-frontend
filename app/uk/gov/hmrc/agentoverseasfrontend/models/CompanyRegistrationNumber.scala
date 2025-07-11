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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json._
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter

case class CompanyRegistrationNumber(
  confirmRegistration: Option[Boolean],
  registrationNumber: Option[Crn] = None
)

object CompanyRegistrationNumber {

  def companyRegistrationNumberDatabaseFormat(implicit
    crypto: Encrypter
      with Decrypter
  ): Format[CompanyRegistrationNumber] =
    (
      (__ \ "confirmRegistration").formatNullable[Boolean] and
        (__ \ "registrationNumber")
          .formatNullable[String](stringEncrypterDecrypter)
          .bimap[Option[Crn]](
            _.map(Crn(_)),
            _.map(_.value)
          )
    )(CompanyRegistrationNumber.apply, unlift(CompanyRegistrationNumber.unapply))

  implicit val format: Format[CompanyRegistrationNumber] = Json.format[CompanyRegistrationNumber]

}
