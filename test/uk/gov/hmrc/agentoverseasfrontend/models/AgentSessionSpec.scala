/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasfrontend.models

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.domain.Nino

import scala.collection.immutable.SortedSet

class AgentSessionSpec extends AnyWordSpecLike with Matchers with OptionValues {

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
    agentCodes = Some(agentCodes),
    registeredForUkTax = Some(No),
    personalDetails = Some(personalDetails),
    companyRegistrationNumber = Some(crn),
    hasTaxRegNumbers = Some(true),
    taxRegistrationNumbers = Some(SortedSet(Trn("123"), Trn("456"))),
    changingAnswers = false
  )

  "cleanAgentSession" should {

    "contain amls details, contact details, trading name, main business address" in {

      val cleanedSession = agentSession.sanitize

      assertCommonDetails(cleanedSession)
    }

    "keep the wanted details and discard the unwanted details" when {
      "registeredWithHmrc is Yes" when {
        "one or more agent codes were provided" in {
          val cleanedSession = agentSession.sanitize

          assertCommonDetails(cleanedSession)

          cleanedSession.agentCodes shouldBe agentSession.agentCodes

          cleanedSession.registeredForUkTax shouldBe Some(No)
          cleanedSession.registeredWithHmrc shouldBe Some(Yes)
          cleanedSession.personalDetails shouldBe None
          cleanedSession.companyRegistrationNumber shouldBe Some(crn)
          cleanedSession.taxRegistrationNumbers shouldBe Some(SortedSet(Trn("123"), Trn("456")))
        }

        "no agents codes were provided" when {
          "UkTaxRegistration is Yes" in {
            val cleanedSession = agentSession
              .copy(registeredWithHmrc = Some(Yes), agentCodes = None, registeredForUkTax = Some(Yes))
              .sanitize

            assertCommonDetails(cleanedSession)

            cleanedSession.agentCodes shouldBe None

            cleanedSession.registeredForUkTax shouldBe Some(Yes)
            cleanedSession.registeredWithHmrc shouldBe Some(Yes)
            cleanedSession.personalDetails shouldBe agentSession.personalDetails
            cleanedSession.companyRegistrationNumber shouldBe agentSession.companyRegistrationNumber
            cleanedSession.taxRegistrationNumbers shouldBe agentSession.taxRegistrationNumbers

          }

          "UkTaxRegistration is No" in {
            val cleanedSession = agentSession
              .copy(registeredWithHmrc = Some(Yes), agentCodes = None, registeredForUkTax = Some(No))
              .sanitize

            assertCommonDetails(cleanedSession)
            cleanedSession.agentCodes shouldBe None

            cleanedSession.registeredForUkTax shouldBe Some(No)
            cleanedSession.registeredWithHmrc shouldBe Some(Yes)
            cleanedSession.personalDetails shouldBe None
            cleanedSession.companyRegistrationNumber shouldBe agentSession.companyRegistrationNumber
            cleanedSession.taxRegistrationNumbers shouldBe agentSession.taxRegistrationNumbers
          }
        }
      }

      "registeredWithHmrc = No" when {
        "registeredForUkTax = Yes" in {
          val cleanedSession = agentSession.copy(registeredWithHmrc = Some(No), registeredForUkTax = Some(Yes)).sanitize

          assertCommonDetails(cleanedSession)

          cleanedSession.agentCodes shouldBe None

          cleanedSession.registeredWithHmrc shouldBe Some(No)
          cleanedSession.registeredForUkTax shouldBe Some(Yes)
          cleanedSession.personalDetails shouldBe agentSession.personalDetails
          cleanedSession.companyRegistrationNumber shouldBe agentSession.companyRegistrationNumber
          cleanedSession.taxRegistrationNumbers shouldBe agentSession.taxRegistrationNumbers
        }

        "registeredForUkTax = No" in {
          val cleanedSession = agentSession.copy(registeredWithHmrc = Some(No), registeredForUkTax = Some(No)).sanitize

          assertCommonDetails(cleanedSession)

          cleanedSession.agentCodes shouldBe None

          cleanedSession.registeredWithHmrc shouldBe Some(No)
          cleanedSession.registeredForUkTax shouldBe Some(No)
          cleanedSession.personalDetails shouldBe None
          cleanedSession.companyRegistrationNumber shouldBe agentSession.companyRegistrationNumber
          cleanedSession.taxRegistrationNumbers shouldBe agentSession.taxRegistrationNumbers
        }
      }
    }

    def assertCommonDetails(cleanedSession: AgentSession) = {
      cleanedSession.amlsDetails shouldBe agentSession.amlsDetails
      cleanedSession.contactDetails shouldBe agentSession.contactDetails
      cleanedSession.tradingName shouldBe agentSession.tradingName
    }
  }
}
