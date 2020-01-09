/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.support.TestSessionCache
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global

class SessionStoreServiceSpec extends UnitSpec {

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

  "SessionStoreService AgentSession" should {

    "store agent details" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.cacheAgentSession(agentSession))

      await(store.fetchAgentSession) shouldBe Some(agentSession)
    }

    "always sanitise data when stored" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(
        store.cacheAgentSession(agentSession
          .copy(registeredWithHmrc = Some(No), agentCodes = Some(agentCodes), personalDetails = Some(personalDetails))))

      await(store.fetchAgentSession).get.agentCodes shouldBe None
      await(store.fetchAgentSession).get.personalDetails shouldBe None
    }

    "return None when no application details have been stored" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.fetchAgentSession) shouldBe None
    }
  }
}
