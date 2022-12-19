/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import uk.gov.hmrc.agentoverseasfrontend.controllers.FakeRouting.mock
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionCacheRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

import java.time.Instant
import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoDBSessionStoreServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with ScalaFutures {

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val overseasAddress = OverseasAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetailsChoice(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  private val agentCodes = AgentCodes(Some(SaAgentCode("SA123456")), Some(CtAgentCode("CT123456")))

  private val crn = CompanyRegistrationNumber(Some(true), Some(Crn("123456")))

  private val defaultAgentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("Trading name"),
    overseasAddress = Some(overseasAddress),
    registeredWithHmrc = Some(Yes),
    agentCodes = None,
    registeredForUkTax = Some(No),
    personalDetails = None,
    companyRegistrationNumber = Some(crn),
    hasTaxRegNumbers = Some(true),
    taxRegistrationNumbers = Some(SortedSet(Trn("123"), Trn("456")))
  )

  private val sessionId = hc.sessionId.get.value

  private val agencyDetails = AgencyDetails(
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

  "SessionStoreService AgentSession" should {

    "store agent details" in new Setup {
      when(mockSessionCacheRepository.put(sessionId)(DataKey[AgentSession]("agentSession"), defaultAgentSession))
        .thenReturn(Future.successful(mockCacheItem))

      store.cacheAgentSession(defaultAgentSession).futureValue

      verify(mockSessionCacheRepository)
        .put(sessionId)(DataKey[AgentSession]("agentSession"), defaultAgentSession)
    }

    "always sanitise data when stored" in new Setup {

      val agentSession = defaultAgentSession
        .copy(registeredWithHmrc = Some(No), agentCodes = Some(agentCodes), personalDetails = Some(personalDetails))

      when(mockSessionCacheRepository.put(sessionId)(DataKey[AgentSession]("agentSession"), agentSession.sanitize))
        .thenReturn(Future.successful(mockCacheItem))

      store
        .cacheAgentSession(agentSession)
        .futureValue

      verify(mockSessionCacheRepository).put(sessionId)(DataKey[AgentSession]("agentSession"), agentSession.sanitize)
    }

    "return None when no application details have been stored" in new Setup {
      when(mockSessionCacheRepository.findById(any[String]))
        .thenReturn(Future.successful(None))
      store.fetchAgentSession.futureValue shouldBe None
    }

    "storing and retrieving agency details" in new Setup {
      when(mockSessionCacheRepository.put(sessionId)(DataKey[AgencyDetails]("agencyDetails"), agencyDetails))
        .thenReturn(Future.successful(mockCacheItem))

      store.cacheAgencyDetails(agencyDetails).futureValue

      verify(mockSessionCacheRepository).put(sessionId)(DataKey[AgencyDetails]("agencyDetails"), agencyDetails)
    }

    "return None if fetching agency details when they have not been stored" in new Setup {
      when(mockSessionCacheRepository.findById(any[String]))
        .thenReturn(Future.successful(None))
      store.fetchAgencyDetails.futureValue shouldBe None
    }

    "remove the underlying storage for the current session when remove is called" in new Setup {

      when(mockSessionCacheRepository.put(sessionId)(DataKey[AgencyDetails]("agencyDetails"), agencyDetails))
        .thenReturn(Future.successful(mockCacheItem))
      store.cacheAgencyDetails(agencyDetails).futureValue
      when(mockSessionCacheRepository.delete(sessionId)(DataKey[AgencyDetails]("agencyDetails")))
        .thenReturn(Future.successful(()))

      store.remove().futureValue

      verify(mockSessionCacheRepository).delete(sessionId)(DataKey[AgencyDetails]("agencyDetails"))
    }
  }

  trait Setup {
    protected val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]
    val mockCacheItem = CacheItem("id", Json.obj(), Instant.now, Instant.now)
    val store = new MongoDBSessionStoreService(mockSessionCacheRepository)
  }
}
