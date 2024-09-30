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
import play.api.libs.json._
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.utils.StringFormatFallbackSetup.stringFormatFallback
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.BadRequestException

case class PersonalDetailsChoice(choice: Option[RadioOption], nino: Option[Nino], saUtr: Option[SaUtr])

object PersonalDetailsChoice {
  sealed trait RadioOption { val value: String }

  object RadioOption {
    case object NinoChoice extends RadioOption { val value = "nino" }
    case object SaUtrChoice extends RadioOption { val value = "saUtr" }

    def apply(str: String): RadioOption = str.trim match {
      case NinoChoice.value  => NinoChoice
      case SaUtrChoice.value => SaUtrChoice
      case _                 => throw new BadRequestException("Strange form input value")
    }

    def unapply(answer: RadioOption): Option[String] = Some(answer.value)

    implicit val format: Format[RadioOption] = new Format[RadioOption] {

      override def reads(json: JsValue): JsResult[RadioOption] =
        json match {
          case JsString(NinoChoice.value)  => JsSuccess(NinoChoice)
          case JsString(SaUtrChoice.value) => JsSuccess(SaUtrChoice)
          case invalid                     => JsError(s"Invalid RadioOption value found: $invalid")
        }

      override def writes(o: RadioOption): JsValue = JsString(o.value)
    }
  }

  def apply(choice: String, ninoOpt: Option[String], saUtrOpt: Option[String]): PersonalDetailsChoice = {

    val (nino, saUtr) = RadioOption(choice) match {
      case RadioOption.NinoChoice  => (ninoOpt.map(Nino), None)
      case RadioOption.SaUtrChoice => (None, saUtrOpt.map(SaUtr))
    }

    PersonalDetailsChoice(Some(RadioOption(choice)), nino, saUtr)
  }

  def personalDetailsChoiceDatabaseFormat(implicit crypto: Encrypter with Decrypter): Format[PersonalDetailsChoice] =
    (
      (__ \ "choice")
        .formatNullable[String](stringFormatFallback(stringEncrypterDecrypter))
        .bimap[Option[RadioOption]](
          _.map(RadioOption(_)),
          _.map(_.value)
        ) and
        (__ \ "nino")
          .formatNullable[String](stringFormatFallback(stringEncrypterDecrypter))
          .bimap[Option[Nino]](
            _.map(Nino(_)),
            _.map(_.value)
          ) and
        (__ \ "saUtr")
          .formatNullable[String](stringFormatFallback(stringEncrypterDecrypter))
          .bimap[Option[SaUtr]](
            _.map(SaUtr(_)),
            _.map(_.value)
          )
    )(PersonalDetailsChoice.apply, unlift(PersonalDetailsChoice.unapply))

  implicit val personalDetailsFormat: OFormat[PersonalDetailsChoice] = Json.format[PersonalDetailsChoice]
}
