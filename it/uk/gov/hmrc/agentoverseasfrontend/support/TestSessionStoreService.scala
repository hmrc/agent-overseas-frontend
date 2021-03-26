package uk.gov.hmrc.agentoverseasfrontend.support

import uk.gov.hmrc.agentoverseasfrontend.models.{AgencyDetails, AgentSession}
import uk.gov.hmrc.agentoverseasfrontend.services.MongoDBSessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TestSessionStoreService extends MongoDBSessionStoreService(null) {

  class Session(var agentSession: Option[AgentSession] = None, var agencyDetails: Option[AgencyDetails] = None)

  private val sessions = collection.mutable.Map[String, Session]()

  private def sessionKey(implicit hc: HeaderCarrier): String = hc.sessionId match {
    case None => "default"
    case Some(sessionId) => sessionId.toString
  }

  def currentSession(implicit hc: HeaderCarrier): Session =
    sessions.getOrElseUpdate(sessionKey, new Session())

  def clear(): Unit =
    sessions.clear()

  def allSessionsRemoved: Boolean =
    sessions.isEmpty

  override def fetchAgentSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentSession]] =
    Future.successful(currentSession.agentSession)

  override def cacheAgentSession(agentSession: AgentSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future.successful(currentSession.agentSession = Some(agentSession))

  override def removeAgentSession(implicit ec: ExecutionContext) = {
    Future.successful(sessions.clear())
  }

  override def fetchAgencyDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgencyDetails]] =
    Future successful currentSession.agencyDetails

  override def cacheAgencyDetails(agencyDetails: AgencyDetails)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future.successful(currentSession.agencyDetails = Some(agencyDetails))

  override def remove()(implicit ec: ExecutionContext): Future[Unit] = {
    Future.successful{
      sessions.clear()
      ()
    }
  }
}
