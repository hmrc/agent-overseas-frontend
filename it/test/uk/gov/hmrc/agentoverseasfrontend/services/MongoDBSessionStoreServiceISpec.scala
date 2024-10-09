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

package uk.gov.hmrc.agentoverseasfrontend.services

import play.api.libs.json.{JsObject, JsString, JsValue, Json, __}
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionCacheRepository
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global

class MongoDBSessionStoreServiceISpec extends BaseISpec with CleanMongoCollectionSupport {

  private implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(
    "znbxS3YXv6TsIzb8OyeF7DlpXtl95Myvec+Hy8JHzO4="
  )

  private implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val sessionCacheRepository: SessionCacheRepository = new SessionCacheRepository(
    mongo = mongoComponent,
    timestampSupport = new CurrentTimestampSupport()
  )
  private val mongoDBSessionStoreService: MongoDBSessionStoreService = new MongoDBSessionStoreService(
    sessionCache = sessionCacheRepository
  )

  private val id: String = "sessionId123456"
  private val instant: Instant = Instant.now()
  private val contactDetails: ContactDetails = ContactDetails(
    firstName = "first",
    lastName = "last",
    jobTitle = "senior agent",
    businessTelephone = "12345",
    businessEmail = "test@email.com"
  )
  private val amlsDetails = AmlsDetails(
    supervisoryBody = "Keogh Chartered Accountants",
    membershipNumber = Some("123456")
  )
  private val overseasAddress = OverseasAddress(
    addressLine1 = "line 1",
    addressLine2 = "line 2",
    addressLine3 = Some("line 3"),
    addressLine4 = Some("line 4"),
    countryCode = "IE"
  )
  private val personalDetails: PersonalDetailsChoice = PersonalDetailsChoice(
    choice = Some(RadioOption.NinoChoice),
    nino = Some(Nino("AB123456A")),
    saUtr = Some(SaUtr("10000009"))
  )
  private val agentCodes: AgentCodes = AgentCodes(
    selfAssessment = Some(SaAgentCode("SA123456")),
    corporationTax = Some(CtAgentCode("CT123456"))
  )
  private val companyRegistrationNumber: CompanyRegistrationNumber = CompanyRegistrationNumber(
    confirmRegistration = Some(true),
    registrationNumber = Some(Crn("123456"))
  )
  private val fileUploadStatus: FileUploadStatus = FileUploadStatus(
    reference = "ref",
    fileStatus = "success",
    fileName = Some("name")
  )
  private val agentSession = AgentSession(
    amlsRequired = Some(true),
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("Trading name"),
    overseasAddress = Some(overseasAddress),
    registeredWithHmrc = Some(Yes),
    agentCodes = Some(agentCodes),
    registeredForUkTax = Some(Yes),
    personalDetails = Some(personalDetails),
    companyRegistrationNumber = Some(companyRegistrationNumber),
    hasTaxRegNumbers = Some(true),
    taxRegistrationNumbers = Some(SortedSet(Trn("123"), Trn("456"))),
    tradingAddressUploadStatus = Some(fileUploadStatus),
    amlsUploadStatus = Some(fileUploadStatus),
    trnUploadStatus = Some(fileUploadStatus),
    fileType = Some("pdf"),
    verifiedEmails = Set("test1@email.com", "test2@email.com")
  )
  private val agencyDetails: AgencyDetails = AgencyDetails(
    agencyName = "agencyName",
    agencyEmail = "test3@email.com",
    agencyAddress = overseasAddress,
    verifiedEmails = Set("test4@email.com", "test5@email.com", "test6@email.com")
  )

  private val agentSessionJson: JsValue = Json.parse(
    """
      |{
      |    "agentSession": {
      |        "amlsRequired": true,
      |        "amlsDetails": {
      |            "supervisoryBody": "TxKxbnyg1c7avQN7Yz3RIt58RKud+60hJB05N2IrinM=",
      |            "membershipNumber": "km29BO1yO9WEzbdgSBWYsw=="
      |        },
      |        "contactDetails": {
      |            "firstName": "tbnirqBMdx1c7PBHrSnnYw==",
      |            "lastName": "QyVGMXFSVdXqKQs8ar3wNQ==",
      |            "jobTitle": "bukqEgFxzHZElfIZ4wP9fQ==",
      |            "businessTelephone": "W2/Ehdz0dlMr4z1Mfs8z3A==",
      |            "businessEmail": "aPPkkql3ehrgFd+QsfNqIA=="
      |        },
      |        "tradingName": "Z+iOkv9G6WgDSbMa8NJmQw==",
      |        "overseasAddress": {
      |            "addressLine1": "+X/xsyopF91AbFzmXIz/TA==",
      |            "addressLine2": "U7jtSadVHCB28zN6KS2N6w==",
      |            "addressLine3": "OaaEBcOFfqfJMpyXBkS1rg==",
      |            "addressLine4": "IewK2150K11wqlOigYh2wA==",
      |            "countryCode": "gx48DsHk2+NcnNGV+iY11w=="
      |        },
      |        "registeredWithHmrc": "yes",
      |        "agentCodes": {
      |            "selfAssessment": "fSncfJk2IBpahaITdX+G4Q==",
      |            "corporationTax": "ADr1KLo1qOp9Gn3P3UfBCw=="
      |        },
      |        "registeredForUkTax": "yes",
      |        "personalDetails": {
      |            "choice": "rOzP0h41UhJbEXeCLcRwjw==",
      |            "nino": "hSE8j+vbQBPmDxI4DD/E4Q==",
      |            "saUtr": "Yq9Jjug0ybBqLFlYgcmYBQ=="
      |        },
      |        "companyRegistrationNumber": {
      |            "confirmRegistration": true,
      |            "registrationNumber": "km29BO1yO9WEzbdgSBWYsw=="
      |        },
      |        "hasTaxRegNumbers": true,
      |        "taxRegistrationNumbers": [
      |            "+ru2C4N2+TbbgbVv2Dm/fw==",
      |            "jF0SwqBCqjQd5vdqQE87ng=="
      |        ],
      |        "tradingAddressUploadStatus": {
      |            "reference": "ref",
      |            "fileStatus": "success",
      |            "fileName": "name"
      |        },
      |        "amlsUploadStatus": {
      |            "reference": "ref",
      |            "fileStatus": "success",
      |            "fileName": "name"
      |        },
      |        "trnUploadStatus": {
      |            "reference": "ref",
      |            "fileStatus": "success",
      |            "fileName": "name"
      |        },
      |        "fileType": "pdf",
      |        "changingAnswers": false,
      |        "hasTrnsChanged": false,
      |        "verifiedEmails": [
      |            "jTv1fZDMNC8grKJFzM/cKQ==",
      |            "AoPArn7lAeyxnU4FgzuKsg=="
      |        ]
      |    }
      |}
    """.stripMargin
  )
  private val agencyDetailsJson: JsValue = Json.parse(
    """
      |{
      |    "agencyDetails": {
      |        "agencyName": "cM/pRW+JWqdxCrUAtWI9lQ==",
      |        "agencyEmail": "pyQiWrGMLEI1SSgmA6Sl2A==",
      |        "agencyAddress": {
      |            "addressLine1": "+X/xsyopF91AbFzmXIz/TA==",
      |            "addressLine2": "U7jtSadVHCB28zN6KS2N6w==",
      |            "addressLine3": "OaaEBcOFfqfJMpyXBkS1rg==",
      |            "addressLine4": "IewK2150K11wqlOigYh2wA==",
      |            "countryCode": "gx48DsHk2+NcnNGV+iY11w=="
      |        },
      |        "verifiedEmails": [
      |            "KBNMi0j1mAywzsCcTtCeDw==",
      |            "0OI3bNscW/gedAar4zwBhA==",
      |            "OVm22wBmGbDGClzqzgyA7w=="
      |        ]
      |    }
      |}
    """.stripMargin
  )

  "MongoDBSessionStoreService" when {
    "session ID is present" should {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(id)))

      "cache and sanitize agent session" when {
        "registeredWithHmrc and registeredForUkTax are set to Yes" in {
          await(mongoDBSessionStoreService.cacheAgentSession(agentSession))

          val result: Seq[CacheItem] = await(sessionCacheRepository.collection.find().toFuture())

          result.size shouldBe 1

          result.head.data shouldBe agentSessionJson
        }

        "registeredWithHmrc and registeredForUkTax are set to No" in {
          val data: JsObject = Json.toJsObject(
            agentSessionJson
              .transform(
                (__ \ "agentSession" \ "agentCodes").json.prune andThen
                  (__ \ "agentSession" \ "personalDetails").json.prune andThen
                  __.json.update((__ \ "agentSession" \ "registeredWithHmrc").json.put(JsString(No.value))) andThen
                  __.json.update((__ \ "agentSession" \ "registeredForUkTax").json.put(JsString(No.value)))
              )
              .get
          )

          await(
            mongoDBSessionStoreService.cacheAgentSession(
              agentSession.copy(
                registeredWithHmrc = Some(No),
                registeredForUkTax = Some(No)
              )
            )
          )

          val result: Seq[CacheItem] = await(sessionCacheRepository.collection.find().toFuture())

          result.size shouldBe 1

          result.head.data shouldBe data
        }
      }

      "return the cached data when agent session data" which {
        "is encrypted has been stored" in {
          await(mongoDBSessionStoreService.cacheAgentSession(agentSession))

          val result: Seq[CacheItem] = await(sessionCacheRepository.collection.find().toFuture())

          result.size shouldBe 1

          result.head.data shouldBe agentSessionJson

          await(mongoDBSessionStoreService.fetchAgentSession) shouldBe Some(agentSession)
        }
      }

      "return None when no agent session data has been stored" in {
        await(mongoDBSessionStoreService.fetchAgentSession) shouldBe None
      }

      "remove the underlying storage for the current agent session when removeAgentSession is called" in {
        await(mongoDBSessionStoreService.cacheAgentSession(agentSession))

        await(sessionCacheRepository.collection.find().toFuture()).size shouldBe 1

        await(mongoDBSessionStoreService.removeAgentSession)

        await(sessionCacheRepository.collection.find().toFuture()).head.data shouldBe Json.obj()
      }

      "cache agency details" in {
        await(mongoDBSessionStoreService.cacheAgencyDetails(agencyDetails))

        val result: Seq[CacheItem] = await(sessionCacheRepository.collection.find().toFuture())

        result.size shouldBe 1

        result.head.data shouldBe agencyDetailsJson
      }

      "return cached agency details" which {
        "has encrypted agency details data stored with verifiedEmails present" in {
          await(mongoDBSessionStoreService.cacheAgencyDetails(agencyDetails))

          val result: Seq[CacheItem] = await(sessionCacheRepository.collection.find().toFuture())

          result.size shouldBe 1

          result.head.data shouldBe agencyDetailsJson

          await(mongoDBSessionStoreService.fetchAgencyDetails) shouldBe Some(agencyDetails)
        }

        "has encrypted agency details data stored with verifiedEmails absent" in {
          val data: JsObject = Json.toJsObject(
            agencyDetailsJson
              .transform(
                (__ \ "agencyDetails" \ "verifiedEmails").json.prune
              )
              .get
          )

          val cacheItem: CacheItem = CacheItem(id, data, instant, instant)

          await(sessionCacheRepository.collection.insertOne(cacheItem).toFuture())

          await(sessionCacheRepository.collection.find().toFuture()).size shouldBe 1

          await(mongoDBSessionStoreService.fetchAgencyDetails) shouldBe Some(
            agencyDetails.copy(verifiedEmails = Set.empty)
          )
        }
      }

      "return None when no agency details data has been stored" in {
        await(mongoDBSessionStoreService.fetchAgencyDetails) shouldBe None
      }

      "remove the underlying storage for the current agency details when remove is called" in {
        await(mongoDBSessionStoreService.cacheAgencyDetails(agencyDetails))

        await(sessionCacheRepository.collection.find().toFuture()).size shouldBe 1

        await(mongoDBSessionStoreService.remove())

        await(sessionCacheRepository.collection.find().toFuture()).head.data shouldBe Json.obj()
      }
    }

    "session ID is absent" should {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      "return RuntimeException" when {
        "caching agent session" in {
          intercept[RuntimeException] {
            await(mongoDBSessionStoreService.cacheAgentSession(agentSession))
          }.getMessage shouldBe "Could not store session as no session Id found."
        }

        "caching agency details" in {
          intercept[RuntimeException] {
            await(mongoDBSessionStoreService.cacheAgencyDetails(agencyDetails))
          }.getMessage shouldBe "Could not store session as no session Id found."
        }
      }

      "return None" when {
        "fetching agent session" in {
          await(mongoDBSessionStoreService.fetchAgentSession) shouldBe None
        }

        "fetching agency details" in {
          await(mongoDBSessionStoreService.fetchAgencyDetails) shouldBe None
        }
      }

      "return Unit" when {
        "removing agent session" in {
          await(mongoDBSessionStoreService.removeAgentSession) shouldBe (): Unit
        }

        "removing agency details" in {
          await(mongoDBSessionStoreService.remove()) shouldBe (): Unit
        }
      }
    }
  }
}
