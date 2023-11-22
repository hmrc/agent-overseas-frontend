package uk.gov.hmrc.agentoverseasfrontend.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Suite}
import play.api.Application

import scala.jdk.CollectionConverters._

trait MetricsTestSupport {
  self: Suite with Matchers =>

  def app: Application

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- registry.getMetrics.keySet().iterator().asScala) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def verifyTimerExistsAndBeenUpdated(metric: String): Assertion = {
    val timers = metricsRegistry.getTimers
    val metrics = timers.get(s"Timer-$metric")
    if (metrics == null) throw new Exception(s"Metric [$metric] not found, try one of ${timers.keySet()}")
    metrics.getCount should be >= 1L
  }

}
