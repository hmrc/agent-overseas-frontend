package uk.gov.hmrc.agentoverseasfrontend.support

import uk.gov.hmrc.agentoverseasfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasfrontend.services.SessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TestSessionStoreService extends SessionStoreService(null) {

  class Session(var agentSession: Option[AgentSession] = None)

  private val sessions = collection.mutable.Map[String, Session]()

  private def sessionKey(implicit hc: HeaderCarrier): String = hc.userId match {
    case None => "default"
    case Some(userId) => userId.toString
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

  override def removeAgentSession(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    Future.successful(sessions.remove(sessionKey).fold(())(_ => ()))
  }
}
