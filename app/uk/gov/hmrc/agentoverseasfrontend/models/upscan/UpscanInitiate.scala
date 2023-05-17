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

package uk.gov.hmrc.agentoverseasfrontend.models.upscan

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes, _}

case class UpscanInitiate(reference: String, uploadRequest: UploadRequest)

case class UploadRequest(href: String, fields: Map[String, String])

object UploadRequest {
  implicit val writes = new Writes[UploadRequest] {
    def writes(uploadRequest: UploadRequest): JsObject = Json.obj(
      "href"   -> uploadRequest.href,
      "fields" -> uploadRequest.fields
    )
  }
  implicit val reads: Reads[UploadRequest] = ((__ \ "href").read[String] and
    (__ \ "fields").read[Map[String, String]])(UploadRequest.apply _)
}

object UpscanInitiate {
  implicit val writes = new Writes[UpscanInitiate] {
    def writes(upscan: UpscanInitiate): JsObject = Json.obj(
      "reference"     -> upscan.reference,
      "uploadRequest" -> upscan.uploadRequest
    )
  }
  implicit val reads: Reads[UpscanInitiate] =
    ((__ \ "reference").read[String] and
      (__ \ "uploadRequest").read[UploadRequest])(UpscanInitiate.apply _)
}
