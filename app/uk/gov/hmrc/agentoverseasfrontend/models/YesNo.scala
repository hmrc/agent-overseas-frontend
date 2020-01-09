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

package uk.gov.hmrc.agentoverseasfrontend.models

import play.api.libs.json._
import uk.gov.hmrc.http.BadRequestException

case class RadioConfirm(value: Boolean)

object RadioConfirm {
  implicit val format = Json.format[RadioConfirm]
}

sealed trait YesNo {
  val value: String
}

case object Yes extends YesNo { override val value = "yes" }

case object No extends YesNo { override val value = "no" }

object YesNo {
  def apply(str: String): YesNo = str.toLowerCase match {
    case Yes.value => Yes
    case No.value  => No
    case _         => throw new BadRequestException("Strange form input value")
  }

  def unapply(answer: YesNo): Option[String] = answer match {
    case Yes => Some(Yes.value)
    case No  => Some(No.value)
  }

  implicit val reads: Reads[YesNo] = new Reads[YesNo] {
    override def reads(json: JsValue): JsResult[YesNo] =
      json match {
        case JsString(Yes.value) => JsSuccess(Yes)
        case JsString(No.value)  => JsSuccess(No)
        case invalid             => JsError(s"Invalid YesNo value found: $invalid")
      }
  }

  implicit val writes: Writes[YesNo] = new Writes[YesNo] {
    override def writes(o: YesNo): JsValue = JsString(o.value)
  }

  def apply(radioConfirm: RadioConfirm): YesNo =
    if (radioConfirm.value) {
      Yes
    } else {
      No
    }

  def toRadioConfirm(answer: YesNo): RadioConfirm = answer match {
    case Yes => RadioConfirm(true)
    case No  => RadioConfirm(false)
  }
}
