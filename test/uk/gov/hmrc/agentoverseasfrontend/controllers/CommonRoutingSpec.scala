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

package uk.gov.hmrc.agentoverseasfrontend.controllers

import org.mockito.ArgumentMatchers.any

import java.time.LocalDateTime
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import play.api.mvc.Results
import reactivemongo.api.commands.{LastError, WriteResult}
import uk.gov.hmrc.agentoverseasfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.{Accepted, AttemptingRegistration, Complete, Registered, Rejected}
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.services.{ApplicationService, MongoDBSessionStoreService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.{CommonRouting, routes}
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionCacheRepository
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.mongo.DatabaseUpdate

class CommonRoutingSpec extends AnyWordSpecLike with Matchers with OptionValues with ScalaFutures {
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val amlsUploadStatus = FileUploadStatus("ref", "READY", None)
  private val overseasAddress = OverseasAddress("line1", "line2", None, None, "IE")
  private val tradingAddressUploadStatus = FileUploadStatus("ref", "READY", None)
  private val personalDetails = PersonalDetailsChoice(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  private val companyRegistrationNumber = CompanyRegistrationNumber(Some(true), Some(Crn("123")))

  private val subscriptionRootPath = "/agent-services/apply-from-outside-uk/create-account"

  private val detailsUpToRegisteredWithHmrc =
    AgentSession(
      amlsRequired = Some(true),
      amlsDetails = Some(amlsDetails),
      amlsUploadStatus = Some(amlsUploadStatus),
      contactDetails = Some(contactDetails),
      tradingName = Some("some name"),
      overseasAddress = Some(overseasAddress),
      tradingAddressUploadStatus = Some(tradingAddressUploadStatus)
    )

  private val applicationEntityDetails = ApplicationEntityDetails(
    applicationCreationDate = LocalDateTime.now(),
    status = ApplicationStatus.Pending,
    tradingName = "some name",
    businessEmail = "someemail@example.com",
    maintainerReviewedOn = None
  )

  "lookupNextPage" should {
    "return showAntiMoneyLaunderingRegistration when AmlsRequired is not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(amlsRequired = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.AntiMoneyLaunderingController
        .showMoneyLaunderingRequired()
    }

    "return showAntiMoneyLaunderingForm when AmlsDetails are not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(amlsDetails = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.AntiMoneyLaunderingController
        .showAntiMoneyLaunderingForm()
    }

    "return showAntiMoneyLaunderingRegistration when session not found" in {
      FakeRouting.lookupNextPage(None) shouldBe routes.AntiMoneyLaunderingController
        .showMoneyLaunderingRequired()
    }

    "return showFileUpload(amls) when amlsFileUploadStatus not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(amlsUploadStatus = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.FileUploadController
        .showAmlsUploadForm()
    }

    "return showContactDetailsForm when ContactDetails are not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(contactDetails = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
        .showContactDetailsForm()
    }

    "return showTradingNameForm when Trading Name is not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(tradingName = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
        .showTradingNameForm()
    }

    "return showMainBusinessAddressForm when Business Address is not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(overseasAddress = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.TradingAddressController
        .showMainBusinessAddressForm()
    }

    "return showUploadForm(trading-address) when tradingAddressUploadStatus not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(tradingAddressUploadStatus = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.FileUploadController
        .showTradingAddressUploadForm()
    }

    "return showRegisteredWithHmrc when RegisteredWithHmrc choice is not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
        .showRegisteredWithHmrcForm()
    }

    "return correct branching page after having decided if they are registered with HMRC" when {
      "RegisteredWithHmrc choice is Yes" should {
        "return showSelfAssessmentAgentCodeForm when self assessment details are not in session" in {
          val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = Some(Yes))
          FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

          FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
            .showAgentCodesForm()
        }
      }

      s"RegisteredWithHmrc choice is No" should {
        "return showUkTaxRegistrationForm when uk tax registration details are not in session" in {
          val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = Some(No))
          FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

          FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
            .showUkTaxRegistrationForm()
        }
      }
    }
  }

  "return correct branching page after having decided if they are registered for UK tax" when {
    "RegisteredForUkTax choice is Yes" should {
      "return showPersonalDetailsForm when personal details are not in session" in {
        val agentSession = detailsUpToRegisteredWithHmrc
          .copy(registeredWithHmrc = Some(No), registeredForUkTax = Some(Yes), personalDetails = None)
        FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

        FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
          .showPersonalDetailsForm()
      }

      "return showCompanyRegistrationNumberForm when personal details are in session" in {
        val agentSession = detailsUpToRegisteredWithHmrc
          .copy(registeredWithHmrc = Some(No), registeredForUkTax = Some(Yes), personalDetails = Some(personalDetails))
        FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

        FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
          .showCompanyRegistrationNumberForm()
      }
    }

    s"RegisteredForUkTax choice is No" should {
      "return showCompanyRegistrationNumberForm when company registration number is not in session" in {
        val agentSession = detailsUpToRegisteredWithHmrc
          .copy(registeredWithHmrc = Some(No), registeredForUkTax = Some(No), personalDetails = None)
        FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

        FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
          .showCompanyRegistrationNumberForm()
      }
    }
  }

  "return showTaxRegistrationNumberForm when AgentSession collected prerequisites" when {
    "RegisteredForUkTax choice is Yes and personal details have been submitted" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = Some(Yes),
        personalDetails = Some(personalDetails),
        companyRegistrationNumber = Some(companyRegistrationNumber)
      )
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.TaxRegController
        .showTaxRegistrationNumberForm()
    }
  }

  s"RegisteredForUkTax choice is no and personal details were not submitted" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      personalDetails = None,
      companyRegistrationNumber = Some(companyRegistrationNumber))
    FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.TaxRegController
      .showTaxRegistrationNumberForm()
  }

  "return showYourTaxRegNo when hasTaxRegNumbers equals Some(true)" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      personalDetails = None,
      companyRegistrationNumber = Some(companyRegistrationNumber),
      hasTaxRegNumbers = Some(true)
    )
    FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.TaxRegController.showAddTaxRegNoForm()
  }

  "return showCheckYourAnswers when hasTaxRegNumbers equals Some(false)" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      personalDetails = None,
      companyRegistrationNumber = Some(companyRegistrationNumber),
      hasTaxRegNumbers = Some(false)
    )
    FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
      .showCheckYourAnswers()
  }

  "return showAgentCodesForm when registered with HMRC and no answer for agent codes has yet been given" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = Some(Yes), agentCodes = None)
    FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
      .showAgentCodesForm()
  }

  "return showUkTaxRegistrationForm when one or more agent codes have been given" in {
    val agentSession = detailsUpToRegisteredWithHmrc
      .copy(registeredWithHmrc = Some(Yes), agentCodes = Some(AgentCodes(Some(SaAgentCode("saCode")), None)))
    FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
      .showUkTaxRegistrationForm()
  }

  "return showUkTaxRegistrationForm when no agent codes have been given" in {
    val agentSession = detailsUpToRegisteredWithHmrc
      .copy(registeredWithHmrc = Some(Yes), agentCodes = Some(AgentCodes(None, None)))
    FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
      .showUkTaxRegistrationForm()
  }

  "return correct branching page after having submitted no agent codes" when {
    "return showUkTaxRegistrationForm when they have not yet made a choice for whether they are registered for UK tax" in {
      val agentSession = detailsUpToRegisteredWithHmrc
        .copy(registeredWithHmrc = Some(Yes), agentCodes = Some(AgentCodes(None, None)), registeredForUkTax = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
        .showUkTaxRegistrationForm()
    }

    "return showPersonalDetailsForm when an answer for agent codes was given, but no agent codes were supplied, and they've answered yes to UK Tax registration" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(
        registeredWithHmrc = Some(Yes),
        agentCodes = Some(AgentCodes(None, None)),
        registeredForUkTax = Some(Yes),
        personalDetails = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
        .showPersonalDetailsForm()
    }

    s"return showCompanyRegistrationNumberForm when Uk Tax registered choice was No" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(
        registeredWithHmrc = Some(Yes),
        agentCodes = Some(AgentCodes(None, None)),
        registeredForUkTax = Some(No),
        personalDetails = None,
        companyRegistrationNumber = None)
      FakeRouting.sessionStoreService.cacheAgentSession(agentSession).futureValue

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController
        .showCompanyRegistrationNumberForm()
    }
  }

  "routesForApplicationStatuses" should {
    "return applicationStatus page" when {
      "the application status is pending" in {
        testRoutesForApplicationStatuses(
          List(applicationEntityDetails),
          "/agent-services/apply-from-outside-uk/application-status")
      }

      "the application status is rejected" in {
        testRoutesForApplicationStatuses(
          List(applicationEntityDetails.copy(status = Rejected)),
          "/agent-services/apply-from-outside-uk/application-status")
      }
    }

    "return overseas-subscription-frontend root page" when {
      Set(Accepted, AttemptingRegistration, Registered, Complete).foreach { status =>
        s"the application status is ${status.key}" in {
          testRoutesForApplicationStatuses(List(applicationEntityDetails.copy(status = status)), subscriptionRootPath)
        }
      }
    }

    "return /money-laundering-registration page" in {
      testRoutesForApplicationStatuses(
        List.empty,
        "/agent-services/apply-from-outside-uk/money-laundering-registration")
    }
  }

  def testRoutesForApplicationStatuses(applications: List[ApplicationEntityDetails], responseRoute: String) = {
    when(FakeRouting.connector.getUserApplications).thenReturn(Future.successful(applications))

    FakeRouting.routesIfExistingApplication(subscriptionRootPath).futureValue.url shouldBe responseRoute
  }

}

object FakeRouting extends CommonRouting with Results with MockitoSugar {
  val connector = mock[AgentOverseasApplicationConnector]
  protected val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]
  protected val mockDatabaseUpdate: DatabaseUpdate[Cache] = mock[DatabaseUpdate[Cache]]
  protected val mockLastError: LastError = mock[LastError]
  protected val mockWriteResult: WriteResult = mock[WriteResult]

  when(mockSessionCacheRepository.createOrUpdate(any[Id], any[String], any[JsValue]))
    .thenReturn(Future.successful(mockDatabaseUpdate))
  when(mockDatabaseUpdate.writeResult).thenReturn(mockLastError)
  when(mockLastError.inError).thenReturn(false)
  when(mockWriteResult.writeErrors).thenReturn(Seq.empty)

  override val sessionStoreService = new MongoDBSessionStoreService(mockSessionCacheRepository)
  override val applicationService = new ApplicationService(connector)
}
