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

import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionCacheRepository
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global

class SessionCacheServiceISpec
extends BaseISpec
with CleanMongoCollectionSupport {

  private implicit val crypto: Encrypter
    with Decrypter = SymmetricCryptoFactory.aesCrypto(
    "znbxS3YXv6TsIzb8OyeF7DlpXtl95Myvec+Hy8JHzO4="
  )

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.sessionId -> "testValue")

  private lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val sessionCacheRepository: SessionCacheRepository =
    new SessionCacheRepository(
      mongo = mongoComponent,
      timestampSupport = new CurrentTimestampSupport(),
      appConfig = appConfig
    )

  private val testSessionCacheService: SessionCacheService =
    new SessionCacheService(
      sessionCacheRepository = sessionCacheRepository
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
    verifiedEmails = Set(
      "test4@email.com",
      "test5@email.com",
      "test6@email.com"
    )
  )

  "SessionCacheService" when {
    "session ID is present" should {
      "cache and sanitize agent session" when {
        "registeredWithHmrc and registeredForUkTax are set to Yes" in {
          await(testSessionCacheService.cacheAgentSession(agentSession))

          val result = await(testSessionCacheService.fetchAgentSession)

          result.value shouldBe agentSession
        }

        "registeredWithHmrc and registeredForUkTax are set to No" in {
          await(testSessionCacheService.cacheAgentSession(agentSession.copy(
            registeredWithHmrc = Some(No),
            registeredForUkTax = Some(No)
          )))

          val result = await(testSessionCacheService.fetchAgentSession)

          result.value shouldBe agentSession.copy(
            agentCodes = None,
            personalDetails = None,
            registeredWithHmrc = Some(No),
            registeredForUkTax = Some(No)
          )
        }
      }

      "return None when no agent session data has been stored" in {
        await(testSessionCacheService.fetchAgentSession) shouldBe None
      }

      "remove the underlying storage for the current agent session when removeAgentSession is called" in {
        await(testSessionCacheService.cacheAgentSession(agentSession))

        await(sessionCacheRepository.cacheRepo.collection.find().toFuture()).size shouldBe 1

        await(testSessionCacheService.removeAgentSession)

        await(sessionCacheRepository.cacheRepo.collection.find().toFuture()).head.data shouldBe Json.obj()
      }

      "cache agency details" in {
        await(testSessionCacheService.cacheAgencyDetails(agencyDetails))

        val result = await(testSessionCacheService.fetchAgencyDetails)

        result.value shouldBe agencyDetails
      }

      "return cached agency details" which {
        "has encrypted agency details data stored with verifiedEmails absent" in {
          val data = agencyDetails.copy(
            verifiedEmails = Set()
          )

          await(testSessionCacheService.cacheAgencyDetails(data))

          val result = await(testSessionCacheService.fetchAgencyDetails)

          result.value shouldBe data
        }
      }

      "return None when no agency details data has been stored" in {
        await(testSessionCacheService.fetchAgencyDetails) shouldBe None
      }

      "remove the underlying storage for the current agency details when remove is called" in {
        await(testSessionCacheService.cacheAgencyDetails(agencyDetails))

        await(sessionCacheRepository.cacheRepo.collection.find().toFuture()).size shouldBe 1

        await(testSessionCacheService.removeAgencyDetails)

        await(sessionCacheRepository.cacheRepo.collection.find().toFuture()).head.data shouldBe Json.obj()
      }
    }
  }

}
