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
import play.api.libs.json.Json
import uk.gov.hmrc.agentoverseasfrontend.models.{AgencyDetails, ApplicationStatus, CreateOverseasApplicationRequest}

trait AgentOverseasApplicationStubs {

  private val allStatuses =
    ApplicationStatus.allStatuses.map(status => s"statusIdentifier=${status.key}").mkString("&")

  def givenPostOverseasApplication(status: Int, requestBody: String = defaultRequestBody): StubMapping =
    stubFor(
      post(urlEqualTo(s"/agent-overseas-application/application"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(aResponse()
          .withStatus(status)))

  private val defaultRequestBody =
    s"""|{
        |"amlsRequired": true,
        |  "amls": {
        |    "supervisoryBody": "Association of AccountingTechnicians (AAT)",
        |    "membershipNumber": "12121"
        |  },
        |  "contactDetails": {
        |    "firstName": "Bob",
        |    "lastName": "Anderson",
        |    "jobTitle": "Accountant",
        |    "businessTelephone": "123456789",
        |    "businessEmail": "test@example.com"
        |  },
        |  "tradingDetails": {
        |    "tradingName": "Some business trading Name",
        |    "tradingAddress": {
        |      "addressLine1": "50 SomeStreet",
        |      "addressLine2": "Some town",
        |      "countryCode": "IE"
        |    },
        |    "isUkRegisteredTaxOrNino": "yes",
        |    "isHmrcAgentRegistered": "no",
        |    "saAgentCode": "SA123456",
        |    "ctAgentCode": "CT123456",
        |    "companyRegistrationNumber": "1234",
        |    "taxRegistrationNumbers": [
        |      "1234567"
        |    ]
        |  },
        |  "personalDetails": {
        |    "saUtr": "4000000009",
        |    "nino": "AB123456A"
        |  },
        |  "amlsFileRef": "amlsfile",
        |  "tradingAddressFileRef": "tradingaddressfile",
        |  "taxRegFileRef": "taxregfile"
        |}
     """.stripMargin

  val defaultCreateApplicationRequest: CreateOverseasApplicationRequest =
    Json.parse(defaultRequestBody).as[CreateOverseasApplicationRequest]

  def given200OverseasPendingApplication(
    appCreateDate: Option[String] = Some("2019-02-20T15:11:51.729")): StubMapping = {
    val responseData = StubsTestData.pendingApplication(appCreateDate.getOrElse("2019-02-20T15:11:51.729"))
    stubFor(
      get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
        .willReturn(
          aResponse()
            .withBody(responseData)
            .withStatus(200)))
  }

  def given200OverseasAcceptedApplication(): StubMapping = {
    val responseData = StubsTestData.acceptedApplication
    stubFor(
      get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
        .willReturn(
          aResponse()
            .withBody(responseData)
            .withStatus(200)))
  }

  def given200OverseasRedirectStatusApplication(redirectStatus: String): StubMapping = {
    val responseData = StubsTestData.applicationInRedirectStatus(redirectStatus)
    stubFor(
      get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
        .willReturn(
          aResponse()
            .withBody(responseData)
            .withStatus(200)))
  }

  def given200GetOverseasApplications(allRejected: Boolean): StubMapping = {
    val requestBody = if (allRejected) StubsTestData.allRejected else StubsTestData.notAllRejected
    stubFor(
      get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
        .willReturn(
          aResponse()
            .withBody(requestBody)
            .withStatus(200)))
  }

  def given404OverseasApplications(): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
        .willReturn(aResponse()
          .withStatus(404)))

  def given500GetOverseasApplication(): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
        .willReturn(aResponse()
          .withStatus(500)))

  def given200UpscanPollStatusReady(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/upscan-poll-status/reference"))
        .willReturn(
          aResponse().withBody("""{"reference":"reference","fileStatus":"READY","fileName":"some"}""").withStatus(200)))

  def given200UpscanPollStatusNotReady(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/upscan-poll-status/reference"))
        .willReturn(aResponse().withBody("""{"reference":"reference","fileStatus":"NOT_READY"}""").withStatus(200)))

  def given500UpscanPollStatus(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/upscan-poll-status/reference"))
        .willReturn(aResponse().withStatus(500)))

  def givenAcceptedApplicationResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.applicationWithStatus())
          .withStatus(200)
      ))

  def givenAcceptedApplicationResponseWithUnverifiedEmail(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.applicationWithStatus())
          .withStatus(200)
      ))

  def givenPendingApplicationResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.pendingApplication)
          .withStatus(200)
      ))

  def givenRegisteredApplicationResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.applicationWithRegisteredStatus)
          .withStatus(200)
      ))

  def givenAttemptingRegistrationApplicationResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.applicationWithStatus("attempting_registration"))
          .withStatus(200)
      ))

  def givenRejectedApplicationResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.rejectedApplication)
          .withStatus(200)
      ))

  def givenCompleteApplicationResponse(arn: Option[String] = None): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.applicationWithCompleteStatus(arn.getOrElse("TARN0000001")))
          .withStatus(200)
      ))

  def givenApplicationMultiple(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        okJson(StubsTestData.notAllRejected)
          .withStatus(200)
      ))

  def givenApplicationEmptyResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application")).willReturn(
        aResponse()
          .withStatus(404)
      ))

  def givenApplicationUnavailable(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application"))
        .willReturn(aResponse()
          .withStatus(503)))

  def givenApplicationServerError(): StubMapping =
    stubFor(
      get(urlEqualTo("/agent-overseas-application/application"))
        .willReturn(aResponse()
          .withStatus(500)))

  def givenApplicationUpdateSuccessResponse(): StubMapping =
    stubFor(
      put(urlEqualTo("/agent-overseas-application/application")).willReturn(
        aResponse()
          .withStatus(204)
      ))

  def givenApplicationUpdateNotFoundResponse(): StubMapping =
    stubFor(
      put(urlEqualTo("/agent-overseas-application/application")).willReturn(
        aResponse()
          .withStatus(404)
      ))

  def givenApplicationUpdateServerError(): StubMapping =
    stubFor(
      put(urlEqualTo("/agent-overseas-application/application")).willReturn(
        aResponse()
          .withStatus(500)
      ))

  def givenUpdateAuthIdSuccessResponse(oldAuthId: String): StubMapping =
    stubFor(
      put(urlEqualTo("/agent-overseas-application/application/auth-provider-id"))
        .withRequestBody(equalToJson(s"""{"authId": "$oldAuthId"}"""))
        .willReturn(aResponse()
          .withStatus(204)))

  def givenUpdateAuthIdNotFoundResponse(): StubMapping =
    stubFor(
      put(urlEqualTo("/agent-overseas-application/application/auth-provider-id")).willReturn(
        aResponse()
          .withStatus(404)
      ))

  def givenUpdateAuthIdServerError(): StubMapping =
    stubFor(
      put(urlEqualTo("/agent-overseas-application/application/auth-provider-id")).willReturn(
        aResponse()
          .withStatus(500)
      ))

  def verifyApplicationUpdate(requestBody: AgencyDetails): Unit = {
    import uk.gov.hmrc.agentoverseasfrontend.models.AgencyDetails.formats
    val expectedRequestBody: String = Json.toJson(requestBody).toString

    verify(
      putRequestedFor(urlEqualTo("/agent-overseas-application/application"))
        .withRequestBody(equalToJson(expectedRequestBody)))
  }

  def verifyUpdateAuthIdRequest(count: Int): Unit =
    verify(count, putRequestedFor(urlEqualTo("/agent-overseas-application/application/auth-provider-id")))
}
