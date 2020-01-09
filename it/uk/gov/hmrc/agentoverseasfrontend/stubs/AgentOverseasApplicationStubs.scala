package uk.gov.hmrc.agentoverseasfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentoverseasfrontend.models.{ApplicationStatus, CreateOverseasApplicationRequest}

trait AgentOverseasApplicationStubs {

  private val allStatuses =
    ApplicationStatus.allStatuses.map(status => s"statusIdentifier=${status.key}").mkString("&")

  def givenPostOverseasApplication(status: Int, requestBody: String = defaultRequestBody): StubMapping = {
    stubFor(post(urlEqualTo(s"/agent-overseas-application/application"))
      .withRequestBody(equalToJson(requestBody))
      .willReturn(aResponse()
        .withStatus(status)))
  }

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

  val defaultCreateApplicationRequest: CreateOverseasApplicationRequest = Json.parse(defaultRequestBody).as[CreateOverseasApplicationRequest]

  def given200OverseasPendingApplication(appCreateDate: Option[String] = Some("2019-02-20T15:11:51.729")): StubMapping = {
    val responseData = StubsTestData.pendingApplication(appCreateDate.getOrElse("2019-02-20T15:11:51.729"))
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withBody(responseData)
        .withStatus(200))
    )
  }

  def given200OverseasAcceptedApplication(): StubMapping = {
    val responseData = StubsTestData.acceptedApplication
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withBody(responseData)
        .withStatus(200))
    )
  }

  def given200OverseasRedirectStatusApplication(redirectStatus: String): StubMapping = {
    val responseData = StubsTestData.applicationInRedirectStatus(redirectStatus)
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
    .willReturn(aResponse()
    .withBody(responseData)
    .withStatus(200)))
  }

  def given200GetOverseasApplications(allRejected: Boolean): StubMapping = {
    val requestBody = if (allRejected) StubsTestData.allRejected else StubsTestData.notAllRejected
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withBody(requestBody)
        .withStatus(200))
    )
  }

  def given404OverseasApplications(): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withStatus(404)))
  }

  def given500GetOverseasApplication(): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withStatus(500)))
  }

  def given200UpscanPollStatusReady():  StubMapping = {
    stubFor(get(urlEqualTo("/agent-overseas-application/upscan-poll-status/reference"))
    .willReturn(aResponse().withBody("""{"reference":"reference","fileStatus":"READY","fileName":"some"}""").withStatus(200)))
  }

  def given200UpscanPollStatusNotReady():  StubMapping = {
    stubFor(get(urlEqualTo("/agent-overseas-application/upscan-poll-status/reference"))
      .willReturn(aResponse().withBody("""{"reference":"reference","fileStatus":"NOT_READY"}""").withStatus(200)))
  }

  def given500UpscanPollStatus():  StubMapping = {
    stubFor(get(urlEqualTo("/agent-overseas-application/upscan-poll-status/reference"))
      .willReturn(aResponse().withStatus(500)))
  }
}
