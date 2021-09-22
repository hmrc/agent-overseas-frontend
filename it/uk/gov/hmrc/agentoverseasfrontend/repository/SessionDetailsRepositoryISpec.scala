package uk.gov.hmrc.agentoverseasfrontend.repository

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionDetailsRepository
import uk.gov.hmrc.agentoverseasfrontend.support.MongoApp

import scala.concurrent.ExecutionContext.Implicits.global

class SessionDetailsRepositoryISpec extends AnyWordSpecLike with Matchers with OptionValues with ScalaFutures with GuiceOneAppPerSuite with MongoApp {

  protected def builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(mongoConfiguration)

  override implicit lazy val app: Application = builder.build()

  private lazy val repo = app.injector.instanceOf[SessionDetailsRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.ensureIndexes.futureValue
    ()
  }

  private val authProviderId = "12345-credId"

  "SessionDetailsRepository" should {

    "create a SessionDetails record" in {
      val result = repo.create(authProviderId)
      result.futureValue should not be empty

      val mappingArnResult = repo.find("id" -> result.futureValue).futureValue.head
      mappingArnResult should have('id (result.futureValue), 'authProviderId (authProviderId))
      mappingArnResult.id.size shouldBe 32
    }

    "find a SessionDetails record by Id" in {
      val record = SessionDetails(authProviderId)
      repo.insert(record).futureValue

      val result = repo.findAuthProviderId(record.id)

      result.futureValue shouldBe Some(record.authProviderId)
    }

    "delete a SessionDetails record by Id" in {
      val record = SessionDetails(authProviderId)
      repo.insert(record).futureValue

      repo.delete(record.id).futureValue

      repo.find("id" -> record.id).futureValue shouldBe empty
    }

    "not return any SessionDetails record for an invalid Id" in {
      val result = repo.findAuthProviderId("INVALID")

      result.futureValue shouldBe empty
    }
  }
}
