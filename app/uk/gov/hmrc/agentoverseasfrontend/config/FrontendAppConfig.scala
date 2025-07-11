/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasfrontend.config

import com.google.inject.ImplementedBy
import com.google.inject.Singleton
import javax.inject.Inject
import play.api.Environment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig {

  val appName: String
  val countryListLocation: String
  val feedbackSurveyUrl: String
  val maintainerApplicationReviewDays: Int
  val companyAuthSignInUrl: String
  val ggRegistrationFrontendSosRedirectPath: String
  val guidancePageApplicationUrl: String
  val authBaseUrl: String
  val agentOverseasFrontendUrl: String
  val agentOverseasApplicationBaseUrl: String
  val upscanBaseUrl: String
  val betaFeedbackUrl: String
  val timeout: Int
  val timeoutCountdown: Int
  val agentGuidancePageFullUrl: String
  val asaFrontendUrl: String
  val agentSubscriptionBaseUrl: String
  val emailVerificationBaseUrl: String
  val emailVerificationFrontendBaseUrl: String
  val mongoDbExpireAfterSeconds: Int
  val disableEmailVerification: Boolean

}

@Singleton
class FrontendAppConfig @Inject() (
  servicesConfig: ServicesConfig,
  environment: Environment
)
extends AppConfig {

  override val appName: String = "agent-overseas-frontend"
  override val countryListLocation: String = servicesConfig.getString("country.list.location")
  override val feedbackSurveyUrl: String = servicesConfig.getString("feedback-survey-url")
  override val maintainerApplicationReviewDays: Int = servicesConfig.getInt("maintainer-application-review-days")
  override val companyAuthSignInUrl: String = servicesConfig.getString("microservice.services.companyAuthSignInUrl")
  override val ggRegistrationFrontendSosRedirectPath: String = servicesConfig.getString(
    "microservice.services.government-gateway-registration-frontend.sosRedirect-path"
  )
  override val guidancePageApplicationUrl: String = servicesConfig.getString("microservice.services.guidancePageApplicationUrl")
  override val authBaseUrl: String = servicesConfig.baseUrl("auth")
  override val agentOverseasApplicationBaseUrl: String = servicesConfig.baseUrl("agent-overseas-application")
  override val agentOverseasFrontendUrl: String = servicesConfig.getString("microservice.services.agent-overseas-frontend.url")
  override val upscanBaseUrl: String = servicesConfig.baseUrl("upscan")
  override val betaFeedbackUrl: String = servicesConfig.getString("betaFeedbackUrl")
  override val timeout: Int = servicesConfig.getInt("timeoutDialog.timeout-seconds")
  override val timeoutCountdown: Int = servicesConfig.getInt("timeoutDialog.timeout-countdown-seconds")
  override val agentGuidancePageFullUrl: String = servicesConfig.getString("agent-guidance-page.full-url")
  override val asaFrontendUrl: String = servicesConfig.getString("microservice.services.agent-services-account-frontend.url")
  override val agentSubscriptionBaseUrl: String = servicesConfig.baseUrl("agent-subscription")
  override val emailVerificationBaseUrl: String = servicesConfig.baseUrl("email-verification")
  override val emailVerificationFrontendBaseUrl: String = servicesConfig.getString("microservice.services.email-verification-frontend.external-url")
  override val mongoDbExpireAfterSeconds: Int = servicesConfig.getInt("mongodb.session.expireAfterSeconds")
  override val disableEmailVerification: Boolean = servicesConfig.getBoolean("disable-email-verification")

}
