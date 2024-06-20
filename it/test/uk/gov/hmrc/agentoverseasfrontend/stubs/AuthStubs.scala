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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import uk.gov.hmrc.agentoverseasfrontend.support.WireMockSupport
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def authorisedAsValidAgent[A](request: FakeRequest[A], arn: String) =
    authenticated(request, Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn), isAgent = true)

  protected def authenticatedAs(user: SampleUser, method: String = GET): FakeRequest[AnyContentAsEmpty.type] = {
    userIsAuthenticated(user)
    FakeRequest(method, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")
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
          """.stripMargin
    )
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def givenUnauthorisedWith(mdtpDetail: String): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")
        )
    )

  def givenAuthorisedFor(payload: String, responseBody: String): StubMapping = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
        )
    )
  }

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

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
         |  "allEnrolments": [
         |    { "key":"${enrolment.serviceName}", "identifiers": [
         |      {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
         |    ]}
         |  ],
         |  "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  },
         |  "email":"authemail@email.com"
         |}
          """.stripMargin
    )
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
         |{"allEnrolments": [], "email":"authemail@email.com"}""".stripMargin
    )
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
         |  "allEnrolments": [],
         |  "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  },
         |  "email":"authemail@email.com"
         |}
          """.stripMargin
    )
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
         |  "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  },
         |  "email":"authemail@email.com"
         |}
          """.stripMargin
    )
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
         |  "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  },
         |  "email":"authemail@email.com"
         |}
          """.stripMargin
    )
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def userIsAuthenticated(user: SampleUser) = {
    val response =
      s"""{${user.allEnrolments},${user.affinityGroup},"optionalCredentials": {"providerId": "${user.userId}", "providerType": "GovernmentGateway"}}"""
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )
  }
}
