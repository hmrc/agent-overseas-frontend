package uk.gov.hmrc.agentoverseasfrontend.stubs

object StubsTestData {

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
     |    "status": "pending",
     |    "authProviderIds": [
     |      "9865690"
     |    ]
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


}
