/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasfrontend

import com.google.inject.AbstractModule
import javax.inject.{Inject, Singleton}
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasfrontend.config.AppConfig
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.HttpClient

class FrontendModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    val appName: String = "agent-overseas-frontend"

    val loggerDateFormat: Option[String] =
      configuration.getOptional[String]("logger.json.dateformat")
    Logger(getClass).info(s"Starting microservice : $appName : in mode : ${environment.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))

    bind(classOf[SessionCache]).to(classOf[AgentOverseasSessionCache])
    ()
  }

}

@Singleton
class AgentOverseasSessionCache @Inject()(val http: HttpClient, appConfig: AppConfig) extends SessionCache {
  override lazy val defaultSource = appConfig.appName
  override lazy val baseUri = appConfig.sessionCacheBaseUrl
  override def domain: String = appConfig.sessionCacheDomain
}
