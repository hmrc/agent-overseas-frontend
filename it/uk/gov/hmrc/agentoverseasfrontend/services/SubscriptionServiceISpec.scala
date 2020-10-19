package uk.gov.hmrc.agentoverseasfrontend.services

import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentoverseasfrontend.models.FailureToSubscribe.{AlreadySubscribed, NoAgencyInSession, NoApplications, WrongApplicationStatus}
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails.SessionDetailsId
import uk.gov.hmrc.agentoverseasfrontend.stubs.StubsTestData._
import uk.gov.hmrc.agentoverseasfrontend.stubs.{AgentOverseasApplicationStubs, AgentSubscriptionStubs}
import uk.gov.hmrc.agentoverseasfrontend.support.BaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionServiceISpec extends BaseISpec with AgentOverseasApplicationStubs with AgentSubscriptionStubs {

  implicit val hc = HeaderCarrier()
  val service = app.injector.instanceOf[SubscriptionService]

  "mostRecentApplication" should {
    "return application record for an auth provider id" in {
      givenAcceptedApplicationResponse()
      await(service.mostRecentApplication) shouldBe Some(application)
    }

    "return active application record that will be the most recently made for the auth provider id" in {
      givenApplicationMultiple()
      await(service.mostRecentApplication) shouldBe Some(application)
    }

    "return empty results for an auth provider id" in {
      givenApplicationEmptyResponse()
      await(service.mostRecentApplication) shouldBe None
    }
  }

  "subscribe" should {
    "return Arn on successful subscription" when {
      def testSuccessfulSubscription() = {
        givenApplicationUpdateSuccessResponse()
        givenSubscriptionSuccessfulResponse(Arn("123"))

        await(service.subscribe) shouldBe Right(Arn("123"))
      }

      "most recent application is 'accepted' and the agency details are in the session" in {
        givenAcceptedApplicationResponse()
        sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)
        testSuccessfulSubscription()
      }

      "most recent application is 'registered' and there are no agency details in the session" in {
        givenRegisteredApplicationResponse()
        sessionStoreService.currentSession.agencyDetails = None
        testSuccessfulSubscription()
      }

      "most recent application is 'complete' and there are no agency details in the session" in {
        givenCompleteApplicationResponse()
        sessionStoreService.currentSession.agencyDetails = None
        testSuccessfulSubscription()
      }
    }

    "fail with Left on unsuccessful subscription" when {
      "details are missing from the session store, return Left(NoAgencyInSession)" in {
        givenAcceptedApplicationResponse()
        sessionStoreService.currentSession.agencyDetails = None
        await(service.subscribe) shouldBe Left(NoAgencyInSession)
      }

      "the user has no applications, return Left(NoApplications)" in {
        givenApplicationEmptyResponse()
        await(service.subscribe) shouldBe Left(NoApplications)
      }

      "the user's most recent application is in 'attempting_registration' status, return Left(WrongApplicationStatus)" in {
        givenAttemptingRegistrationApplicationResponse()
        await(service.subscribe) shouldBe Left(WrongApplicationStatus)
      }

      "the user's most recent application is in 'rejected' status, return Left(WrongApplicationStatus)" in {
        givenRejectedApplicationResponse()
        await(service.subscribe) shouldBe Left(WrongApplicationStatus)
      }

      "the user's most recent application is in 'pending' status, return Left(WrongApplicationStatus)" in {
        givenPendingApplicationResponse()
        await(service.subscribe) shouldBe Left(WrongApplicationStatus)
      }

      "upstream agent-subscription returns 409 (i.e. the HMRC-AS-AGENT enrolment with their ARN is already allocated to a group)" in {
        sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)
        givenCompleteApplicationResponse()
        givenApplicationUpdateSuccessResponse()
        givenSubscriptionFailedConflict()
        await(service.subscribe) shouldBe Left(AlreadySubscribed)
      }
    }

    "fail with exception on unsuccessful subscription" when {
      "upstream agent-overseas-application retrieve application fails with 500" in {
        sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)
        givenApplicationServerError()
        givenApplicationUpdateServerError()
        an[UpstreamErrorResponse] shouldBe thrownBy(await(service.subscribe))
      }

      "upstream agent-overseas-application retrieve application succeeds but update fails with 500" in {
        sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)
        givenAcceptedApplicationResponse()
        givenApplicationUpdateServerError()
        an[UpstreamErrorResponse] shouldBe thrownBy(await(service.subscribe))
      }

      "upstream agent-subscription is unavailable" in {
        sessionStoreService.currentSession.agencyDetails = Some(agencyDetails)
        givenAcceptedApplicationResponse()
        givenApplicationUpdateSuccessResponse()
        givenSubscriptionFailedUnavailable()
        an[UpstreamErrorResponse] shouldBe thrownBy(await(service.subscribe))
      }
    }
  }

  "detailsStoreAuthProviderId" should {
    "return produced Id as reference to obtaining stored authProviderId" in {
      val idRef: SessionDetailsId = await(service.storeSessionDetails(authProviderId))
      val findUsingIdRef = await(service.authProviderId(idRef))

      idRef.toString.size shouldBe 32
      findUsingIdRef shouldBe Some(authProviderId)
    }

    "return None when Id not found" in {
      val sampleIdRef = new SessionDetails.SessionDetailsId("d4b872c5819f49f9aebc50f921f5bd2c")
      val findUsingIdRef = await(service.authProviderId(sampleIdRef))

      findUsingIdRef shouldBe None
    }
  }
}
