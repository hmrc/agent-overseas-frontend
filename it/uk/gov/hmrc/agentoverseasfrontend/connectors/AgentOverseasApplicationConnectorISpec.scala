package uk.gov.hmrc.agentoverseasfrontend.connectors

import com.kenshoo.play.metrics.Metrics
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.WsScalaTestClient
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasfrontend.models.{AgencyDetails, FileUploadStatus, OverseasAddress}
import uk.gov.hmrc.agentoverseasfrontend.stubs.StubsTestData._
import uk.gov.hmrc.agentoverseasfrontend.stubs.{AgentOverseasApplicationStubs, AuthStubs, DataStreamStubs}
import uk.gov.hmrc.agentoverseasfrontend.support.{BaseISpec, MetricsTestSupport, MongoApp, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class AgentOverseasApplicationConnectorISpec
    extends BaseISpec with AgentOverseasApplicationStubs with WsScalaTestClient
    with ScalaFutures with WireMockSupport with AuthStubs with DataStreamStubs with MetricsTestSupport with MongoApp {

  private lazy val metrics = app.injector.instanceOf[Metrics]
  private lazy val http = app.injector.instanceOf[HttpClient]
  private lazy val appConfig = app.injector.instanceOf[AppConfig]

  private implicit val hc = HeaderCarrier()

  private lazy val connector: AgentOverseasApplicationConnector =
    new AgentOverseasApplicationConnector(appConfig, http, metrics)

  "createOverseasApplication" should {

    "create an application successfully" in {
      givenPostOverseasApplication(201)

      await(connector.createOverseasApplication(defaultCreateApplicationRequest)) shouldBe (())
    }

    "return exception" when {
      "the application already exists" in {
        givenPostOverseasApplication(409)

        an[Exception] should be thrownBy (await(connector.createOverseasApplication(defaultCreateApplicationRequest)))
      }

      "service is unavailable" in {
        givenPostOverseasApplication(503)

        an[Exception] should be thrownBy (await(connector.createOverseasApplication(defaultCreateApplicationRequest)))
      }
    }
  }

  "upscanPollStatus" should {

    "return a FileUploadStatus with READY status when the file was received from AWS/Upscan" in {
      given200UpscanPollStatusReady()

      await(connector.upscanPollStatus("reference")) shouldBe FileUploadStatus("reference", "READY", Some("some"))
    }

    "return a FileUploadStatus with NOT_READY status when the file was NOT received from AWS/Upscan" in {
      given200UpscanPollStatusNotReady()

      await(connector.upscanPollStatus("reference")) shouldBe FileUploadStatus("reference", "NOT_READY", None)
    }

    "service is unavailable" in {
      given500UpscanPollStatus()

      an[Exception] should be thrownBy (await(connector.upscanPollStatus("reference")))
    }

  }

  "allApplications" should {
    "return applications for an authProviderId" in {
      givenAcceptedApplicationResponse

      await(connector.allApplications) shouldBe List(application)
    }

    "return empty result for an authProviderId" in {
      givenApplicationEmptyResponse

      await(connector.allApplications) shouldBe List.empty
    }

    "return exception if the service is unavailable" in {
      givenApplicationUnavailable
      an[Exception] shouldBe thrownBy(await(connector.allApplications))
    }

    "return exception if the service respond with internal server error" in {
      givenApplicationServerError
      an[Exception] shouldBe thrownBy(await(connector.allApplications))
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
      agencyEmail = "agency_email@domain.com"
    )

    "return successfully if application was updated successfully" in {
      givenApplicationUpdateSuccessResponse()

      await(connector.updateApplicationWithAgencyDetails(agencyDetails))

      verifyApplicationUpdate(agencyDetails)
    }

    "return exception if the upstream returns 404" in {
      givenApplicationUpdateNotFoundResponse()

      an[Upstream4xxResponse] shouldBe thrownBy(await(connector.updateApplicationWithAgencyDetails(agencyDetails)))
    }

    "return exception if the upstream responds with 500 internal server error" in {
      givenApplicationUpdateServerError()

      an[Upstream5xxResponse] shouldBe thrownBy(await(connector.updateApplicationWithAgencyDetails(agencyDetails)))
    }
  }

  "updateAuthId" should {
    val oldAuthId = "142d146e"
    "add the new authId to the application" in {
      givenUpdateAuthIdSuccessResponse(oldAuthId)

      await(connector.updateAuthId(oldAuthId)) shouldBe (())
    }

    "return exception if the upstream returns 404" in {
      givenUpdateAuthIdNotFoundResponse()

      an[NotFoundException] shouldBe thrownBy(await(connector.updateAuthId(oldAuthId)))
    }

    "return exception if the upstream responds with 500 internal server error" in {
      givenUpdateAuthIdServerError()

      an[Upstream5xxResponse] shouldBe thrownBy(await(connector.updateAuthId(oldAuthId)))
    }
  }
}
