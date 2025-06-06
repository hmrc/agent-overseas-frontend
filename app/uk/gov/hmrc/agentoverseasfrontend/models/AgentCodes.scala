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
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.__
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter

case class AgentCodes(
  selfAssessment: Option[SaAgentCode],
  corporationTax: Option[CtAgentCode]
) {

  def hasOneOrMoreCodes: Boolean =
    this match {
      case AgentCodes(None, None) => false
      case _ => true
    }

  def isEmpty: Boolean = !hasOneOrMoreCodes

}

object AgentCodes {

  def agentCodesDatabaseFormat(implicit
    crypto: Encrypter
      with Decrypter
  ): Format[AgentCodes] =
    (
      (__ \ "selfAssessment")
        .formatNullable[String](stringEncrypterDecrypter)
        .bimap[Option[SaAgentCode]](
          _.map(SaAgentCode(_)),
          _.map(_.value)
        ) and
        (__ \ "corporationTax")
          .formatNullable[String](stringEncrypterDecrypter)
          .bimap[Option[CtAgentCode]](
            _.map(CtAgentCode(_)),
            _.map(_.value)
          )
    )(AgentCodes.apply, unlift(AgentCodes.unapply))

  implicit val format: OFormat[AgentCodes] = Json.format[AgentCodes]

}
