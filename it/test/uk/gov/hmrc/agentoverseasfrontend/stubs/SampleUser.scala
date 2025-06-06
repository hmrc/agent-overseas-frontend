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

package uk.gov.hmrc.agentoverseasfrontend.stubs

import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.EnrolmentIdentifier

case class SampleUser(
  userId: String,
  enrolments: Seq[Enrolment],
  affinity: AffinityGroup
) {

  val allEnrolments =
    s""" "allEnrolments": [${enrolments
        .map(e =>
          s"""{
             |"key": "${e.key}",
             |"identifiers": [${e.identifiers
              .map(i => s"""{"key":"${i.key}","value":"${i.value}"}""")
              .mkString(",")}],
             |"state": "${e.state}"
             |}""".stripMargin
        )
        .mkString(",")}] """.stripMargin
  val affinityGroup = s""" "affinityGroup": "$affinity" """

}

object SampleUser {

  val subscribingAgentEnrolledForHMRCASAGENT: SampleUser = SampleUser(
    "12345-credId",
    Seq(Enrolment(
      "HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", "TARN0000001")),
      "Activated"
    )),
    AffinityGroup.Agent
  )

  val subscribingAgentEnrolledForNonMTD: SampleUser = SampleUser(
    "12345-credId",
    Seq(
      Enrolment(
        "IR-PAYE-AGENT",
        Seq(EnrolmentIdentifier("IRAgentReference", "HZ1234")),
        "Activated"
      ),
      Enrolment(
        "IR-SA-AGENT",
        Seq(EnrolmentIdentifier("IRAgentReference", "FOO1234")),
        "Activated"
      )
    ),
    AffinityGroup.Agent
  )

  val subscribingCleanAgentWithoutEnrolments: SampleUser = SampleUser(
    "12345-credId",
    Seq(),
    AffinityGroup.Agent
  )

  val subscribing2ndCleanAgentWithoutEnrolments: SampleUser = SampleUser(
    "54321-credId",
    Seq(),
    AffinityGroup.Agent
  )

  val individual: SampleUser = SampleUser(
    "individual",
    Seq(Enrolment(
      "FOO",
      Seq(EnrolmentIdentifier("foo", "AAAAA")),
      "Activated"
    )),
    AffinityGroup.Individual
  )

}
