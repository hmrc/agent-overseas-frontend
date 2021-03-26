/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.JsValue
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.{LastError, WriteResult}
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionCacheRepository
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.mongo.DatabaseUpdate
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MongoDBSessionStoreServiceSpec extends UnitSpec {

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val overseasAddress = OverseasAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetailsChoice(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  private val agentCodes = AgentCodes(Some(SaAgentCode("SA123456")), Some(CtAgentCode("CT123456")))

  private val crn = CompanyRegistrationNumber(Some(true), Some(Crn("123456")))

  private val agentSession = AgentSession(
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

  private val agencyDetails = AgencyDetails(
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

  "SessionStoreService AgentSession" should {

    "store agent details" in new Setup {
      await(store.cacheAgentSession(agentSession))

      verify(mockSessionCacheRepository).createOrUpdate(any[Id], any[String], any[JsValue])
    }

    "always sanitise data when stored" in new Setup {

      await(
        store.cacheAgentSession(agentSession
          .copy(registeredWithHmrc = Some(No), agentCodes = Some(agentCodes), personalDetails = Some(personalDetails))))

      verify(mockSessionCacheRepository).createOrUpdate(any[Id], any[String], any[JsValue])
    }

    "return None when no application details have been stored" in new Setup {
      when(mockSessionCacheRepository.findById(any[Id], any[ReadPreference])(any[ExecutionContext]))
        .thenReturn(Future.successful(None))
      await(store.fetchAgentSession) shouldBe None
    }

    "storing and retrieving agency details" in new Setup {
      await(store.cacheAgencyDetails(agencyDetails))

      verify(mockSessionCacheRepository).createOrUpdate(any[Id], any[String], any[JsValue])
    }

    "return None if fetching agency details when they have not been stored" in new Setup {
      when(mockSessionCacheRepository.findById(any[Id], any[ReadPreference])(any[ExecutionContext]))
        .thenReturn(Future.successful(None))
      await(store.fetchAgencyDetails) shouldBe None
    }

    "remove the underlying storage for the current session when remove is called" in new Setup {
      await(store.cacheAgencyDetails(agencyDetails))

      await(store.remove())

      verify(mockSessionCacheRepository).removeAll(any())(any[ExecutionContext])
    }
  }

  trait Setup {
    protected val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]
    protected val mockDatabaseUpdate: DatabaseUpdate[Cache] = mock[DatabaseUpdate[Cache]]
    protected val mockLastError: LastError = mock[LastError]
    protected val mockWriteResult: WriteResult = mock[WriteResult]

    when(mockSessionCacheRepository.createOrUpdate(any[Id], any[String], any[JsValue]))
      .thenReturn(Future.successful(mockDatabaseUpdate))
    when(mockDatabaseUpdate.writeResult).thenReturn(mockLastError)
    when(mockLastError.inError).thenReturn(false)
    when(mockWriteResult.writeErrors).thenReturn(Seq.empty)
    when(mockSessionCacheRepository.removeAll(any())(any())).thenReturn(mockWriteResult)

    val store = new MongoDBSessionStoreService(mockSessionCacheRepository)
  }
}
