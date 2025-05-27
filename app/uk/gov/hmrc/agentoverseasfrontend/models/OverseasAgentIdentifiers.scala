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

  implicit val reads: SimpleObjectReads[SaAgentCode] = new SimpleObjectReads[SaAgentCode]("value", SaAgentCode.apply)
  implicit val writes: SimpleObjectWrites[SaAgentCode] = new SimpleObjectWrites[SaAgentCode](_.value)

}

case class CtAgentCode(value: String)
extends OverseasAgentIdentifier

object CtAgentCode {

  implicit val reads: SimpleObjectReads[CtAgentCode] = new SimpleObjectReads[CtAgentCode]("value", CtAgentCode.apply)
  implicit val writes: SimpleObjectWrites[CtAgentCode] = new SimpleObjectWrites[CtAgentCode](_.value)

}

case class ApplicationReference(value: String)
extends OverseasAgentIdentifier

object ApplicationReference {

  implicit val reads: SimpleObjectReads[ApplicationReference] = new SimpleObjectReads[ApplicationReference]("value", ApplicationReference.apply)
  implicit val writes: SimpleObjectWrites[ApplicationReference] = new SimpleObjectWrites[ApplicationReference](_.value)

  def create(uuid: UUID = UUID.randomUUID()): ApplicationReference = {
    val formatId = uuid.toString.replace("-", "").take(8)
    ApplicationReference(formatId)
  }

}

case class SafeId(value: String)
extends OverseasAgentIdentifier

object SafeId {

  implicit val reads: SimpleObjectReads[SafeId] = new SimpleObjectReads[SafeId]("value", SafeId.apply)
  implicit val writes: SimpleObjectWrites[SafeId] = new SimpleObjectWrites[SafeId](_.value)

}

case class Crn(value: String)
extends OverseasAgentIdentifier

object Crn {

  implicit val reads: SimpleObjectReads[Crn] = new SimpleObjectReads[Crn]("value", Crn.apply)
  implicit val writes: SimpleObjectWrites[Crn] = new SimpleObjectWrites[Crn](_.value)

}

case class Trn(value: String)
extends OverseasAgentIdentifier

object Trn {

  implicit val ordering: Ordering[Trn] = Ordering.by(unapply)
  implicit val reads: SimpleObjectReads[Trn] = new SimpleObjectReads[Trn]("value", Trn.apply)
  implicit val writes: SimpleObjectWrites[Trn] = new SimpleObjectWrites[Trn](_.value)

}
