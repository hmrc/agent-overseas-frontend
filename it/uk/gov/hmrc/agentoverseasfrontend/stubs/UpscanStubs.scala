package uk.gov.hmrc.agentoverseasfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentoverseasfrontend.support.WireMockSupport

trait UpscanStubs {
  me: WireMockSupport =>

  val request = s"""{
                   |"callbackUrl": "http://localhost:$wireMockPort/agent-overseas-application/upscan-callback",
                   |"minimumFileSize": 1000,
                   |"maximumFileSize": 5000000
                   |}
    """.stripMargin

  def given200UpscanInitiate(): StubMapping =
    stubFor(
      post(urlEqualTo("/upscan/initiate"))
        .withRequestBody(equalToJson(request, true, true))
        .willReturn(aResponse()
          .withBody("""{
                      |    "reference": "11370e18-6e24-453e-b45a-76d3e32ea33d",
                      |    "uploadRequest": {
                      |        "href": "https://bucketName.s3.eu-west-2.amazonaws.com",
                      |        "fields": {
                      |           "x-amz-meta-callback-url": "https://myservice.com/callback",
                      |            "x-amz-date": "yyyyMMddThhmmssZ",
                      |            "x-amz-credential": "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
                      |             "x-amz-algorithm": "AWS4-HMAC-SHA256",
                      |             "key": "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                      |            "acl": "private",
                      |             "x-amz-signature": "xxxx",
                      |            "x-amz-meta-consuming-service": "agent-overseas-application-frontend",
                      |            "policy": "xxxxxxxx=="
                      |        }
                      |    }
                      |}""".stripMargin)))
}
