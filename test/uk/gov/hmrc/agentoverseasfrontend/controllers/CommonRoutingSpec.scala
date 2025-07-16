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

package uk.gov.hmrc.agentoverseasfrontend.controllers

import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.agentoverseasfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.CommonRouting
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.routes
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Accepted
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.AttemptingRegistration
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Complete
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Registered
import uk.gov.hmrc.agentoverseasfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasfrontend.models._
import uk.gov.hmrc.agentoverseasfrontend.services.ApplicationService
import uk.gov.hmrc.agentoverseasfrontend.services.SessionCacheService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommonRoutingSpec
extends AnyWordSpecLike
with Matchers
with OptionValues
with ScalaFutures {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.sessionId -> "sessionId123456")

  private val contactDetails = ContactDetails(
    "test",
    "last",
    "senior agent",
    "12345",
    "test@email.com"
  )
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val amlsUploadStatus = FileUploadStatus(
    "ref",
    "READY",
    None
  )
  private val overseasAddress = OverseasAddress(
    "line1",
    "line2",
    None,
    None,
    "IE"
  )
  private val tradingAddressUploadStatus = FileUploadStatus(
    "ref",
    "READY",
    None
  )
  private val personalDetails = PersonalDetailsChoice(
    Some(RadioOption.NinoChoice),
    Some(Nino("AB123456A")),
    None
  )
  private val companyRegistrationNumber = CompanyRegistrationNumber(Some(true), Some(Crn("123")))

  private val subscriptionRootPath = "/agent-services/apply-from-outside-uk/create-account"

  private val detailsUpToRegisteredWithHmrc = AgentSession(
    amlsRequired = Some(true),
    amlsDetails = Some(amlsDetails),
    amlsUploadStatus = Some(amlsUploadStatus),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    overseasAddress = Some(overseasAddress),
    tradingAddressUploadStatus = Some(tradingAddressUploadStatus),
    verifiedEmails = Set(contactDetails.businessEmail)
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

      FakeRouting.lookupNextPage(
        Some(agentSession)
      ) shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired
    }

    "return showAntiMoneyLaunderingForm when AmlsDetails are not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(amlsDetails = None)

      FakeRouting.lookupNextPage(
        Some(agentSession)
      ) shouldBe routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm
    }

    "return showAntiMoneyLaunderingRegistration when session not found" in {
      FakeRouting.lookupNextPage(None) shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired
    }

    "return showFileUpload(amls) when amlsFileUploadStatus not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(amlsUploadStatus = None)

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.FileUploadController.showAmlsUploadForm
    }

    "return showContactDetailsForm when ContactDetails are not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(contactDetails = None)

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showContactDetailsForm
    }

    "return showTradingNameForm when Trading Name is not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(tradingName = None)

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showTradingNameForm
    }

    "return showMainBusinessAddressForm when Business Address is not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(overseasAddress = None)

      FakeRouting.lookupNextPage(
        Some(agentSession)
      ) shouldBe routes.TradingAddressController.showMainBusinessAddressForm
    }

    "return showUploadForm(trading-address) when tradingAddressUploadStatus not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(tradingAddressUploadStatus = None)

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.FileUploadController.showTradingAddressUploadForm
    }

    "return showRegisteredWithHmrc when RegisteredWithHmrc choice is not found in session" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = None)

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showRegisteredWithHmrcForm
    }

    "return correct branching page after having decided if they are registered with HMRC" when {
      "RegisteredWithHmrc choice is Yes" should {
        "return showSelfAssessmentAgentCodeForm when self assessment details are not in session" in {
          val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = Some(Yes))

          FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showAgentCodesForm
        }
      }

      s"RegisteredWithHmrc choice is No" should {
        "return showUkTaxRegistrationForm when uk tax registration details are not in session" in {
          val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = Some(No))

          FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showUkTaxRegistrationForm
        }
      }
    }
  }

  "return correct branching page after having decided if they are registered for UK tax" when {
    "RegisteredForUkTax choice is Yes" should {
      "return showPersonalDetailsForm when personal details are not in session" in {
        val agentSession = detailsUpToRegisteredWithHmrc
          .copy(
            registeredWithHmrc = Some(No),
            registeredForUkTax = Some(Yes),
            personalDetails = None
          )

        FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showPersonalDetailsForm
      }

      "return showCompanyRegistrationNumberForm when personal details are in session" in {
        val agentSession = detailsUpToRegisteredWithHmrc
          .copy(
            registeredWithHmrc = Some(No),
            registeredForUkTax = Some(Yes),
            personalDetails = Some(personalDetails)
          )

        FakeRouting.lookupNextPage(
          Some(agentSession)
        ) shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm
      }
    }

    s"RegisteredForUkTax choice is No" should {
      "return showCompanyRegistrationNumberForm when company registration number is not in session" in {
        val agentSession = detailsUpToRegisteredWithHmrc
          .copy(
            registeredWithHmrc = Some(No),
            registeredForUkTax = Some(No),
            personalDetails = None
          )

        FakeRouting.lookupNextPage(
          Some(agentSession)
        ) shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm
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

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.TaxRegController.showTaxRegistrationNumberForm
    }
  }

  s"RegisteredForUkTax choice is no and personal details were not submitted" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      personalDetails = None,
      companyRegistrationNumber = Some(companyRegistrationNumber)
    )

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.TaxRegController.showTaxRegistrationNumberForm
  }

  "return showYourTaxRegNo when hasTaxRegNumbers equals Some(true)" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      personalDetails = None,
      companyRegistrationNumber = Some(companyRegistrationNumber),
      hasTaxRegNumbers = Some(true)
    )

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.TaxRegController.showAddTaxRegNoForm
  }

  "return showCheckYourAnswers when hasTaxRegNumbers equals Some(false)" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      personalDetails = None,
      companyRegistrationNumber = Some(companyRegistrationNumber),
      hasTaxRegNumbers = Some(false)
    )

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showCheckYourAnswers
  }

  "return showAgentCodesForm when registered with HMRC and no answer for agent codes has yet been given" in {
    val agentSession = detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = Some(Yes), agentCodes = None)

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showAgentCodesForm
  }

  "return showUkTaxRegistrationForm when one or more agent codes have been given" in {
    val agentSession = detailsUpToRegisteredWithHmrc
      .copy(registeredWithHmrc = Some(Yes), agentCodes = Some(AgentCodes(Some(SaAgentCode("saCode")), None)))

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showUkTaxRegistrationForm
  }

  "return showUkTaxRegistrationForm when no agent codes have been given" in {
    val agentSession = detailsUpToRegisteredWithHmrc
      .copy(registeredWithHmrc = Some(Yes), agentCodes = Some(AgentCodes(None, None)))

    FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showUkTaxRegistrationForm
  }

  "return correct branching page after having submitted no agent codes" when {
    "return showUkTaxRegistrationForm when they have not yet made a choice for whether they are registered for UK tax" in {
      val agentSession = detailsUpToRegisteredWithHmrc
        .copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = Some(AgentCodes(None, None)),
          registeredForUkTax = None
        )

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showUkTaxRegistrationForm
    }

    "return showPersonalDetailsForm when an answer for agent codes was given, but no agent codes were supplied, and they've answered yes to UK Tax registration" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(
        registeredWithHmrc = Some(Yes),
        agentCodes = Some(AgentCodes(None, None)),
        registeredForUkTax = Some(Yes),
        personalDetails = None
      )

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationController.showPersonalDetailsForm
    }

    s"return showCompanyRegistrationNumberForm when Uk Tax registered choice was No" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(
        registeredWithHmrc = Some(Yes),
        agentCodes = Some(AgentCodes(None, None)),
        registeredForUkTax = Some(No),
        personalDetails = None,
        companyRegistrationNumber = None
      )

      FakeRouting.lookupNextPage(
        Some(agentSession)
      ) shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm
    }

    "direct to the verify email stage when the email is not verified" in {
      val agentSession = detailsUpToRegisteredWithHmrc.copy(verifiedEmails = Set.empty)

      FakeRouting.lookupNextPage(Some(agentSession)) shouldBe routes.ApplicationEmailVerificationController.verifyEmail
    }
  }

  "routesForApplicationStatuses" should {
    "return applicationStatus page" when {
      "the application status is pending" in {
        testRoutesForApplicationStatuses(
          List(applicationEntityDetails),
          "/agent-services/apply-from-outside-uk/application-status"
        )
      }

      "the application status is rejected" in {
        testRoutesForApplicationStatuses(
          List(applicationEntityDetails.copy(status = Rejected)),
          "/agent-services/apply-from-outside-uk/application-status"
        )
      }
    }

    "return overseas-subscription-frontend root page" when {
      Set(
        Accepted,
        AttemptingRegistration,
        Registered,
        Complete
      ).foreach { status =>
        s"the application status is ${status.key}" in {
          testRoutesForApplicationStatuses(List(applicationEntityDetails.copy(status = status)), subscriptionRootPath)
        }
      }
    }

    "return /money-laundering-registration page" in {
      testRoutesForApplicationStatuses(
        List.empty,
        "/agent-services/apply-from-outside-uk/money-laundering-registration"
      )
    }
  }

  def testRoutesForApplicationStatuses(
    applications: List[ApplicationEntityDetails],
    responseRoute: String
  ): Assertion = {
    when(FakeRouting.connector.getUserApplications).thenReturn(Future.successful(applications))

    FakeRouting.routesIfExistingApplication(subscriptionRootPath).futureValue.url shouldBe responseRoute
  }

}

object FakeRouting
extends CommonRouting
with MockitoSugar {

  val connector: AgentOverseasApplicationConnector = mock[AgentOverseasApplicationConnector]

  private val app: Application = new GuiceApplicationBuilder().build()

  override val sessionStoreService: SessionCacheService = app.injector.instanceOf[SessionCacheService]
  override val applicationService: ApplicationService = new ApplicationService(connector)

}
