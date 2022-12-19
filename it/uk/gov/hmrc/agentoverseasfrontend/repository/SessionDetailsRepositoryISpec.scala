package uk.gov.hmrc.agentoverseasfrontend.repository

import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.agentoverseasfrontend.models.SessionDetails
import uk.gov.hmrc.agentoverseasfrontend.repositories.SessionDetailsRepository
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

class SessionDetailsRepositoryISpec extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with ScalaFutures
  with PlayMongoRepositorySupport[SessionDetails]
  with IntegrationPatience {

  override def repository: PlayMongoRepository[SessionDetails] = new SessionDetailsRepository(mongoComponent)

  val repo = repository.asInstanceOf[SessionDetailsRepository]

  private val authProviderId = "12345-credId"

  "SessionDetailsRepository" should {

    "create a SessionDetails record" in {
      val result = repo.create(authProviderId)
      result.futureValue should not be empty

      val mappingArnResult = repo.collection.find(Filters.equal("id", result.futureValue)).toFuture().futureValue.head
      mappingArnResult should have('id (result.futureValue), 'authProviderId (authProviderId))
      mappingArnResult.id.size shouldBe 32
    }

    "find a SessionDetails record by Id" in {
      val record = SessionDetails(authProviderId)
      repo.collection.insertOne(record).toFuture().futureValue

      val result = repo.findAuthProviderId(record.id)

      result.futureValue shouldBe Some(record.authProviderId)
    }

    "delete a SessionDetails record by Id" in {
      val record = SessionDetails(authProviderId)
      repo.collection.insertOne(record).toFuture().futureValue

      repo.delete(record.id).futureValue

      repo.collection.find(Filters.equal("id", record.id)).toFuture().futureValue shouldBe empty
    }

    "not return any SessionDetails record for an invalid Id" in {
      val result = repo.findAuthProviderId("INVALID")

      result.futureValue shouldBe empty
    }
  }
}
