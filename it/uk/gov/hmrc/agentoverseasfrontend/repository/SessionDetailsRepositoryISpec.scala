package uk.gov.hmrc.agentoverseasfrontend.repository

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionDetailsRepository
import uk.gov.hmrc.agentoverseasfrontend.support.MongoApp
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SessionDetailsRepositoryISpec extends UnitSpec with GuiceOneAppPerSuite with MongoApp {

  protected def builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(mongoConfiguration)

  override implicit lazy val app: Application = builder.build()

  private lazy val repo = app.injector.instanceOf[SessionDetailsRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.ensureIndexes)
    ()
  }

  private val authProviderId = "12345-credId"

  "SessionDetailsRepository" should {

    "create a SessionDetails record" in {
      val result = await(repo.create(authProviderId))
      result should not be empty

      val mappingArnResult = await(repo.find("id" -> result)).head
      mappingArnResult should have('id (result), 'authProviderId (authProviderId))
      mappingArnResult.id.size shouldBe 32
    }

    "find a SessionDetails record by Id" in {
      val record = SessionDetails(authProviderId)
      await(repo.insert(record))

      val result = await(repo.findAuthProviderId(record.id))

      result shouldBe Some(record.authProviderId)
    }

    "delete a SessionDetails record by Id" in {
      val record = SessionDetails(authProviderId)
      await(repo.insert(record))

      await(repo.delete(record.id))

      await(repo.find("id" -> record.id)) shouldBe empty
    }

    "not return any SessionDetails record for an invalid Id" in {
      val result = await(repo.findAuthProviderId("INVALID"))

      result shouldBe empty
    }
  }
}
