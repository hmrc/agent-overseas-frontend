package uk.gov.hmrc.agentoverseasfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.agentoverseasfrontend.support.WireMockSupport
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def authorisedAsValidAgent[A](request: FakeRequest[A], arn: String) = authenticated(request, Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn), isAgent = true)

  protected def authenticatedAs(user: SampleUser): FakeRequest[AnyContentAsEmpty.type] = {
    val sessionKeys = userIsAuthenticated(user)
    FakeRequest().withSession(sessionKeys: _*)
  }

  def authenticated[A](request: FakeRequest[A], enrolment: Enrolment, isAgent: Boolean): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "identifiers":[], "state":"Activated", "enrolment": "${enrolment.serviceName}" },
         |    { "authProviders": ["GovernmentGateway"] }
         |  ],
         |  "retrieve":["authorisedEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"authorisedEnrolments": [
         |  { "key":"${enrolment.serviceName}", "identifiers": [
         |    {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
         |  ]}
         |]}
          """.stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def givenUnauthorisedWith(mdtpDetail: String): StubMapping = {
    stubFor(post(urlEqualTo("/auth/authorise"))
      .willReturn(aResponse()
        .withStatus(401)
        .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")))
  }

  def givenAuthorisedFor(payload: String, responseBody: String): StubMapping = {
    stubFor(post(urlEqualTo("/auth/authorise"))
      .atPriority(1)
      .withRequestBody(equalToJson(payload, true, true))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(responseBody)))

    stubFor(post(urlEqualTo("/auth/authorise")).atPriority(2)
      .willReturn(aResponse()
        .withStatus(401)
        .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))
  }

  def verifyAuthoriseAttempt(): Unit = {
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
  }

  def agentWithAuthorisedEnrolment[A](request: FakeRequest[A], enrolment: Enrolment): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "authProviders": ["GovernmentGateway"] },
         |    {
         |      "affinityGroup": "Agent"
         |    }
         |  ],
         |  "retrieve":["allEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"allEnrolments": [
         |  { "key":"${enrolment.serviceName}", "identifiers": [
         |    {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
         |  ]}
         |],
         |    "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  }}
          """.stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def agentWithNoEnrolmentsOrCreds[A](request: FakeRequest[A]): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "authProviders": ["GovernmentGateway"] },
         |    {
         |      "affinityGroup": "Agent"
         |    }
         |  ],
         |  "retrieve":["allEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{"allEnrolments": []}""".stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def cleanCredsAgent[A](request: FakeRequest[A]): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "authProviders": ["GovernmentGateway"] },
         |    {
         |      "affinityGroup": "Agent"
         |    }
         |  ],
         |  "retrieve":["allEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"allEnrolments": [],
         |    "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  }
         |}
          """.stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def basicRequest[A](request: FakeRequest[A]): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "authProviders": ["GovernmentGateway"] }]
         |}
           """.stripMargin,
      s"""
         |{
         |    "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  }
         |}
          """.stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def basicAgentRequest[A](request: FakeRequest[A]): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "authProviders": ["GovernmentGateway"] },
         |    {"affinityGroup": "Agent"}
         |    ]
         |}
           """.stripMargin,
      s"""
         |{
         |    "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  }
         |}
          """.stripMargin)
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def userIsAuthenticated(user: SampleUser): Seq[(String, String)] = {
    val response =
      s"""{${user.allEnrolments},${user.affinityGroup},"optionalCredentials": {"providerId": "${user.userId}", "providerType": "GovernmentGateway"}}"""
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(response)))
    sessionKeysForMockAuth(user)
  }

  private def sessionKeysForMockAuth(user: SampleUser): Seq[(String, String)] =
    Seq(SessionKeys.userId -> user.userId, "token" -> "fakeToken")
}