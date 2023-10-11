package uk.gov.hmrc.agentoverseasfrontend.stubs

import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier}

case class SampleUser(userId: String, enrolments: Seq[Enrolment], affinity: AffinityGroup) {
  val allEnrolments = s""" "allEnrolments": [${enrolments
    .map(e =>
      s"""{
         |"key": "${e.key}",
         |"identifiers": [${e.identifiers
        .map(i => s"""{"key":"${i.key}","value":"${i.value}"}""")
        .mkString(",")}],
         |"state": "${e.state}"
         |}""".stripMargin)
    .mkString(",")}] """.stripMargin
  val affinityGroup = s""" "affinityGroup": "$affinity" """
}

object SampleUser {

  val subscribingAgentEnrolledForHMRCASAGENT: SampleUser =
    SampleUser(
      "12345-credId",
      Seq(Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "TARN0000001")), "Activated")),
      AffinityGroup.Agent)

  val subscribingAgentEnrolledForNonMTD: SampleUser =
    SampleUser(
      "12345-credId",
      Seq(
        Enrolment("IR-PAYE-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "HZ1234")), "Activated"),
        Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "FOO1234")), "Activated")
      ),
      AffinityGroup.Agent
    )

  val subscribingCleanAgentWithoutEnrolments: SampleUser =
    SampleUser("12345-credId", Seq(), AffinityGroup.Agent)

  val subscribing2ndCleanAgentWithoutEnrolments: SampleUser =
    SampleUser("54321-credId", Seq(), AffinityGroup.Agent)

  val individual: SampleUser =
    SampleUser(
      "individual",
      Seq(Enrolment("FOO", Seq(EnrolmentIdentifier("foo", "AAAAA")), "Activated")),
      AffinityGroup.Individual)
}
