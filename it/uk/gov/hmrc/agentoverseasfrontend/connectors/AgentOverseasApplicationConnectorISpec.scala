package uk.gov.hmrc.agentoverseasfrontend.connectors

import com.kenshoo.play.metrics.Metrics
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.WsScalaTestClient
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models.{AgencyDetails, FileUploadStatus, OverseasAddress}
import uk.gov.hmrc.agentoverseasfrontend.stubs.StubsTestData._
import uk.gov.hmrc.agentoverseasfrontend.stubs.{AgentOverseasApplicationStubs, AuthStubs, DataStreamStubs}
import uk.gov.hmrc.agentoverseasfrontend.support.{BaseISpec, MetricsTestSupport, WireMockSupport}
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class AgentOverseasApplicationConnectorISpec
    extends BaseISpec with AgentOverseasApplicationStubs with WsScalaTestClient
    with ScalaFutures with WireMockSupport with AuthStubs with DataStreamStubs with MetricsTestSupport {

  private lazy val metrics = app.injector.instanceOf[Metrics]
  private lazy val http = app.injector.instanceOf[HttpClient]
  private lazy val appConfig = app.injector.instanceOf[AppConfig]

  private implicit val hc = HeaderCarrier()

  private lazy val connector: AgentOverseasApplicationConnector =
    new AgentOverseasApplicationConnector(appConfig, http, metrics)

  "createOverseasApplication" should {

    "create an application successfully" in {
      givenPostOverseasApplication(201)

      connector.createOverseasApplication(defaultCreateApplicationRequest).futureValue shouldBe (())
    }

    "return exception" when {
      "the application already exists" in {
        givenPostOverseasApplication(409)

        val e = connector.createOverseasApplication(defaultCreateApplicationRequest).failed.futureValue
        e shouldBe a[Exception]
      }

      "service is unavailable" in {
        givenPostOverseasApplication(503)

        val e = connector.createOverseasApplication(defaultCreateApplicationRequest).failed.futureValue
        e shouldBe a[Exception]
      }
    }
  }

  "upscanPollStatus" should {

    "return a FileUploadStatus with READY status when the file was received from AWS/Upscan" in {
      given200UpscanPollStatusReady()

      connector.upscanPollStatus("reference").futureValue shouldBe FileUploadStatus("reference", "READY", Some("some"))
    }

    "return a FileUploadStatus with NOT_READY status when the file was NOT received from AWS/Upscan" in {
      given200UpscanPollStatusNotReady()

      connector.upscanPollStatus("reference").futureValue shouldBe FileUploadStatus("reference", "NOT_READY", None)
    }

    "service is unavailable" in {
      given500UpscanPollStatus()

      val e = connector.upscanPollStatus("reference").failed.futureValue
      e shouldBe a[Exception]
    }

  }

  "allApplications" should {
    "return applications for an authProviderId" in {
      givenAcceptedApplicationResponse()

      connector.allApplications.futureValue shouldBe List(application)
    }

    "return empty result for an authProviderId" in {
      givenApplicationEmptyResponse()

      connector.allApplications.futureValue shouldBe List.empty
    }

    "return exception if the service is unavailable" in {
      givenApplicationUnavailable()
      val e = connector.allApplications.failed.futureValue
      e shouldBe a[Exception]
    }

    "return exception if the service respond with internal server error" in {
      givenApplicationServerError()
      val e = connector.allApplications.failed.futureValue
      e shouldBe a[Exception]
    }
  }

  "updateApplicationWithAgencyDetails" should {
    val agencyDetails = AgencyDetails(
      agencyName = "agencyName",
      agencyAddress = OverseasAddress(
        addressLine1 = "agencyAddressLine1",
        addressLine2 = "agencyAddressLine2",
        addressLine3 = Some("agencyAddressLine3"),
        addressLine4 = Some("agencyAddressLine4"),
        countryCode = "IE"
      ),
      agencyEmail = "agency_email@domain.com",
      verifiedEmails = Set("agency_email@domain.com")
    )

    "return successfully if application was updated successfully" in {
      givenApplicationUpdateSuccessResponse()

      connector.updateApplicationWithAgencyDetails(agencyDetails).futureValue

      verifyApplicationUpdate(agencyDetails)
    }

    "return exception if the upstream returns 404" in {
      givenApplicationUpdateNotFoundResponse()

      val e = connector.updateApplicationWithAgencyDetails(agencyDetails).failed.futureValue
      e shouldBe a[UpstreamErrorResponse]
    }

    "return exception if the upstream responds with 500 internal server error" in {
      givenApplicationUpdateServerError()

      val e = connector.updateApplicationWithAgencyDetails(agencyDetails).failed.futureValue
      e shouldBe a[UpstreamErrorResponse]
    }
  }

  "updateAuthId" should {
    val oldAuthId = "142d146e"
    "add the new authId to the application" in {
      givenUpdateAuthIdSuccessResponse(oldAuthId)

      connector.updateAuthId(oldAuthId).futureValue shouldBe (())
    }

    "return exception if the upstream returns 404" in {
      givenUpdateAuthIdNotFoundResponse()

      val e = connector.updateAuthId(oldAuthId).failed.futureValue
      e shouldBe a[NotFoundException]
    }

    "return exception if the upstream responds with 500 internal server error" in {
      givenUpdateAuthIdServerError()

      val e = connector.updateAuthId(oldAuthId).failed.futureValue
      e shouldBe a[UpstreamErrorResponse]
    }
  }
}
