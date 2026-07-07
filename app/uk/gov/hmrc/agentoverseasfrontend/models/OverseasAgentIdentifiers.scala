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

import java.util.UUID

import uk.gov.hmrc.domain.SimpleObjectReads
import uk.gov.hmrc.domain.SimpleObjectWrites

sealed trait OverseasAgentIdentifier { val value: String }

case class SaAgentCode(value: String)
extends OverseasAgentIdentifier

object SaAgentCode {

  given SimpleObjectReads[SaAgentCode] = new SimpleObjectReads[SaAgentCode]("value", SaAgentCode.apply)
  given SimpleObjectWrites[SaAgentCode] = new SimpleObjectWrites[SaAgentCode](_.value)

}

case class CtAgentCode(value: String)
extends OverseasAgentIdentifier

object CtAgentCode {

  given SimpleObjectReads[CtAgentCode] = new SimpleObjectReads[CtAgentCode]("value", CtAgentCode.apply)
  given SimpleObjectWrites[CtAgentCode] = new SimpleObjectWrites[CtAgentCode](_.value)

}

case class ApplicationReference(value: String)
extends OverseasAgentIdentifier

object ApplicationReference {

  given SimpleObjectReads[ApplicationReference] = new SimpleObjectReads[ApplicationReference]("value", ApplicationReference.apply)
  given SimpleObjectWrites[ApplicationReference] = new SimpleObjectWrites[ApplicationReference](_.value)

  def create(uuid: UUID = UUID.randomUUID()): ApplicationReference = {
    val formatId = uuid.toString.replace("-", "").take(8)
    ApplicationReference(formatId)
  }

}

case class SafeId(value: String)
extends OverseasAgentIdentifier

object SafeId {

  given SimpleObjectReads[SafeId] = new SimpleObjectReads[SafeId]("value", SafeId.apply)
  given SimpleObjectWrites[SafeId] = new SimpleObjectWrites[SafeId](_.value)

}

case class Crn(value: String)
extends OverseasAgentIdentifier

object Crn {

  given SimpleObjectReads[Crn] = new SimpleObjectReads[Crn]("value", Crn.apply)
  given SimpleObjectWrites[Crn] = new SimpleObjectWrites[Crn](_.value)

}

case class Trn(value: String)
extends OverseasAgentIdentifier

object Trn {

  given Ordering[Trn] = Ordering.by(_.value)
  given SimpleObjectReads[Trn] = new SimpleObjectReads[Trn]("value", Trn.apply)
  given SimpleObjectWrites[Trn] = new SimpleObjectWrites[Trn](_.value)

}
