package uk.gov.hmrc.agentoverseasfrontend.stubs

import java.time.LocalDateTime

import uk.gov.hmrc.agentoverseasfrontend.models
import uk.gov.hmrc.agentoverseasfrontend.models._

object StubsTestData {

 val authProviderId = "12345-credId"

 val contactDetails = SubscriptionContactDetails("test@test.com")
 val tradingDetails = SubscriptionTradingDetails("Testing Agency",
  OverseasAddress("addressLine1","addressLine2",Some("addressLine3"),Some("addressLine4"),"CC"))

 val applicationcreationDate = LocalDateTime.parse("2019-02-20T15:11:51.729")

 val application = models.OverseasApplication(applicationcreationDate, ApplicationStatus.Accepted, contactDetails, tradingDetails, None)

 val agencyDetails = AgencyDetails(
  agencyName = "test agency name",
  agencyEmail = "test-agency-email@domain.com",
  agencyAddress = OverseasAddress(
   addressLine1 = "agencyAddressLine1",
   addressLine2 = "agencyAddressLine2",
   addressLine3 = Some("agencyAddressLine3"),
   addressLine4 = Some("agencyAddressLine4"),
   countryCode = "BE"
  )
 )

 def pendingApplication(appCreateDate: String) =
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


 def acceptedApplication =
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

 def applicationInRedirectStatus(redirectStatus: String) =
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


 def allRejected =
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

 def notAllRejected =
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

 def applicationWithStatus(status: String = "accepted") =
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

 def pendingApplication =
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

 def rejectedApplication =
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
