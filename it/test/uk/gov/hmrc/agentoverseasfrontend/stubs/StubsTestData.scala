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

import java.time.LocalDateTime

import uk.gov.hmrc.agentoverseasfrontend.models
import uk.gov.hmrc.agentoverseasfrontend.models._

object StubsTestData {

  val authProviderId = "12345-credId"

  val contactDetails = SubscriptionContactDetails("test@test.com")
  val tradingDetails = SubscriptionTradingDetails(
    "Testing Agency",
    OverseasAddress("addressLine1", "addressLine2", Some("addressLine3"), Some("addressLine4"), "CC")
  )

  val applicationcreationDate = LocalDateTime.parse("2019-02-20T15:11:51.729")

  val application = models.OverseasApplication(
    applicationcreationDate,
    ApplicationStatus.Accepted,
    contactDetails,
    tradingDetails,
    None
  )

  val agencyDetails = AgencyDetails(
    agencyName = "test agency name",
    agencyEmail = "test-agency-email@domain.com",
    agencyAddress = OverseasAddress(
      addressLine1 = "agencyAddressLine1",
      addressLine2 = "agencyAddressLine2",
      addressLine3 = Some("agencyAddressLine3"),
      addressLine4 = Some("agencyAddressLine4"),
      countryCode = "BE"
    ),
    verifiedEmails = Set("test-agency-email@domain.com")
  )

  def pendingApplication(appCreateDate: String): String =
    s"""|[
        |  {
        |    "applicationReference": "25eaab89",
        |    "createdDate": "$appCreateDate",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "TN"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "pending",
        |    "authProviderIds": [
        |      "9865690"
        |    ]
        |  }
        |]
   """.stripMargin

  def acceptedApplication: String =
    s"""|[
        |  {
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-18T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "TN"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "accepted",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID"
        |    }
        |  }
        |]
   """.stripMargin

  def applicationInRedirectStatus(redirectStatus: String): String =
    s"""|[
        |  {
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "TN"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "$redirectStatus",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID"
        |    }
        |  }
        |]
   """.stripMargin

  def allRejected: String =
    s"""|[
        |  {
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "TN"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "rejected",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-21T10:35:21.65",
        |      "reviewerPid": "PID",
        |      "rejectReasons": [
        |        "rejected reason"
        |      ]
        |    }
        |  },
        |  {
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "TN"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "rejected",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID",
        |      "rejectReasons": [
        |        "rejected reason"
        |      ]
        |    }
        |  },
        |  {
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "TN"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "rejected",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID",
        |      "rejectReasons": [
        |        "rejected reason"
        |      ]
        |    }
        |  }
        |]
   """.stripMargin

  def notAllRejected: String =
    s"""[
       | {
       |    "applicationReference": "25eaab89",
       |    "createdDate": "2019-02-20T15:11:51.729",
       |    "amls": {
       |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
       |      "membershipNumber": "0987654321"
       |    },
       |    "contactDetails": {
       |      "firstName": "Testing",
       |      "lastName": "Agent",
       |      "jobTitle": "Tester",
       |      "businessTelephone": "011565438754",
       |      "businessEmail": "test@test.com"
       |    },
       |    "tradingDetails": {
       |      "tradingName": "Testing Agency",
       |      "tradingAddress": {
       |        "addressLine1": "addressLine1",
       |        "addressLine2": "addressLine2",
       |        "addressLine3": "addressLine3",
       |        "addressLine4": "addressLine4",
       |        "countryCode": "CC"
       |      },
       |      "isUkRegisteredTaxOrNino": "yes",
       |      "isHmrcAgentRegistered": "yes",
       |      "saAgentCode": "KOOH67",
       |      "companyRegistrationNumber": "regNumber here",
       |      "taxRegistrationNumbers": [
       |        "anotherTaxRegNumber here",
       |        "taxRegNumber here"
       |      ]
       |    },
       |    "personalDetails": {
       |      "saUtr": "4000000009",
       |      "nino": "AA000000A"
       |    },
       |    "status": "accepted",
       |    "authProviderIds": [
       |      "9865690"
       |    ],
       |    "maintainerDetails": {
       |      "reviewedDate": "2019-02-20T10:35:21.65",
       |      "reviewerPid": "PID"
       |    }
       |  },
       |  {
       |    "applicationReference": "25eaab89",
       |    "createdDate": "2019-02-15T15:11:51.729",
       |    "amls": {
       |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
       |      "membershipNumber": "0987654321"
       |    },
       |    "contactDetails": {
       |      "firstName": "Testing",
       |      "lastName": "Agent",
       |      "jobTitle": "Tester",
       |      "businessTelephone": "011565438754",
       |      "businessEmail": "test@test.com"
       |    },
       |    "tradingDetails": {
       |      "tradingName": "Testing Agency",
       |      "tradingAddress": {
       |        "addressLine1": "addressLine1",
       |        "addressLine2": "addressLine2",
       |        "addressLine3": "addressLine3",
       |        "addressLine4": "addressLine4",
       |        "countryCode": "TN"
       |      },
       |      "isUkRegisteredTaxOrNino": "yes",
       |      "isHmrcAgentRegistered": "yes",
       |      "saAgentCode": "KOOH67",
       |      "companyRegistrationNumber": "regNumber here",
       |      "taxRegistrationNumbers": [
       |        "anotherTaxRegNumber here",
       |        "taxRegNumber here"
       |      ]
       |    },
       |    "personalDetails": {
       |      "saUtr": "4000000009",
       |      "nino": "AA000000A"
       |    },
       |    "status": "rejected",
       |    "authProviderIds": [
       |      "9865690"
       |    ],
       |    "maintainerDetails": {
       |      "reviewedDate": "2019-02-20T10:35:21.65",
       |      "reviewerPid": "PID",
       |      "rejectReasons": [
       |        "rejected reason"
       |      ]
       |    }
       |  },
       |  {
       |    "applicationReference": "25eaab89",
       |    "createdDate": "2019-01-15T15:11:51.729",
       |    "amls": {
       |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
       |      "membershipNumber": "0987654321"
       |    },
       |    "contactDetails": {
       |      "firstName": "Testing",
       |      "lastName": "Agent",
       |      "jobTitle": "Tester",
       |      "businessTelephone": "011565438754",
       |      "businessEmail": "test@test.com"
       |    },
       |    "tradingDetails": {
       |      "tradingName": "Testing Agency",
       |      "tradingAddress": {
       |        "addressLine1": "addressLine1",
       |        "addressLine2": "addressLine2",
       |        "addressLine3": "addressLine3",
       |        "addressLine4": "addressLine4",
       |        "countryCode": "TN"
       |      },
       |      "isUkRegisteredTaxOrNino": "yes",
       |      "isHmrcAgentRegistered": "yes",
       |      "saAgentCode": "KOOH67",
       |      "companyRegistrationNumber": "regNumber here",
       |      "taxRegistrationNumbers": [
       |        "anotherTaxRegNumber here",
       |        "taxRegNumber here"
       |      ]
       |    },
       |    "personalDetails": {
       |      "saUtr": "4000000009",
       |      "nino": "AA000000A"
       |    },
       |    "status": "rejected",
       |    "authProviderIds": [
       |      "9865690"
       |    ],
       |    "maintainerDetails": {
       |      "reviewedDate": "2019-02-20T10:35:21.65",
       |      "reviewerPid": "PID",
       |      "rejectReasons": [
       |        "rejected reason"
       |      ]
       |    }
       |  }
       |]
   """.stripMargin

  def applicationWithStatus(status: String = "accepted"): String =
    s"""|[{
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "CC"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "$status",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID"
        |    }
        |  }]
     """.stripMargin

  def applicationWithCompleteStatus(arn: String): String =
    s"""|[{
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "CC"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "complete",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID"
        |    },
        |    "receivedInDms": "yes",
        |    "agencyDetails": {
        |        "agencyAddress": {
        |            "addressLine1": "17 Roth Terrace",
        |            "addressLine2": "Boloni",
        |            "countryCode": "BE"
        |        },
        |        "agencyEmail": "even@and.com",
        |        "agencyName": "Safez"
        |    },
        |    "safeId": "XX0000646231987",
        |    "arn": "$arn"
        |  }]
     """.stripMargin

  def applicationWithRegisteredStatus: String =
    s"""|[{
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "CC"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "registered",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID"
        |    },
        |    "receivedInDms": "yes",
        |    "agencyDetails": {
        |        "agencyAddress": {
        |            "addressLine1": "17 Roth Terrace",
        |            "addressLine2": "Boloni",
        |            "countryCode": "BE"
        |        },
        |        "agencyEmail": "even@and.com",
        |        "agencyName": "Safez"
        |    },
        |    "safeId": "XX0000646231987"
        |  }]
     """.stripMargin

  def pendingApplication: String =
    s"""|[{
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "CC"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "pending",
        |    "authProviderIds": [
        |      "9865690"
        |    ]
        |  }]
     """.stripMargin

  def rejectedApplication: String =
    s"""|[{
        |    "applicationReference": "25eaab89",
        |    "createdDate": "2019-02-20T15:11:51.729",
        |    "amls": {
        |      "supervisoryBody": "International Association of Bookkeepers (IAB)",
        |      "membershipNumber": "0987654321"
        |    },
        |    "contactDetails": {
        |      "firstName": "Testing",
        |      "lastName": "Agent",
        |      "jobTitle": "Tester",
        |      "businessTelephone": "011565438754",
        |      "businessEmail": "test@test.com"
        |    },
        |    "tradingDetails": {
        |      "tradingName": "Testing Agency",
        |      "tradingAddress": {
        |        "addressLine1": "addressLine1",
        |        "addressLine2": "addressLine2",
        |        "addressLine3": "addressLine3",
        |        "addressLine4": "addressLine4",
        |        "countryCode": "CC"
        |      },
        |      "isUkRegisteredTaxOrNino": "yes",
        |      "isHmrcAgentRegistered": "yes",
        |      "saAgentCode": "KOOH67",
        |      "companyRegistrationNumber": "regNumber here",
        |      "taxRegistrationNumbers": [
        |        "anotherTaxRegNumber here",
        |        "taxRegNumber here"
        |      ]
        |    },
        |    "personalDetails": {
        |      "saUtr": "4000000009",
        |      "nino": "AA000000A"
        |    },
        |    "status": "rejected",
        |    "authProviderIds": [
        |      "9865690"
        |    ],
        |    "maintainerDetails": {
        |      "reviewedDate": "2019-02-20T10:35:21.65",
        |      "reviewerPid": "PID",
        |      "rejectReasons": [
        |        "rejected reason"
        |      ]
        |    }
        |  }]
     """.stripMargin
}
