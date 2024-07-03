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
        .willReturn(
          aResponse()
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
                        |            "x-amz-meta-consuming-service": "agent-overseas-frontend",
                        |            "policy": "xxxxxxxx=="
                        |        }
                        |    }
                        |}""".stripMargin)
        )
    )
}
