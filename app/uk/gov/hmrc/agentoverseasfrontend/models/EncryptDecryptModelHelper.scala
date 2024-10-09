/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.JsString
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.json.JsonEncryption.{stringDecrypter, stringEncrypter}

object EncryptDecryptModelHelper {
  def decryptString(value: String)(implicit crypto: Encrypter with Decrypter): String =
    stringDecrypter.reads(JsString(value)).getOrElse(value)

  def encryptString(value: String)(implicit crypto: Encrypter with Decrypter): String =
    stringEncrypter.writes(value).as[String]
}
