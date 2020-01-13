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

package uk.gov.hmrc.agentoverseasfrontend.config.view

import play.api.data.Form
import play.api.data.Forms.{boolean, default, mapping}
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.agentoverseasfrontend.controllers.application.routes
import uk.gov.hmrc.agentoverseasfrontend.models.{AgentSession, No, Yes, YesNo}

case class AnswerBlock(heading: String, answerGroups: Seq[AnswerGroup])

case class AnswerGroup(answerRows: Seq[AnswerRow])

case class AnswerRow(
  id: String,
  question: String,
  answerLines: Seq[String],
  changeLink: Option[Call],
  buttonText: Option[String],
  visuallyHiddenText: Option[String])

object AnswerRow {
  def apply(
    id: String,
    question: String,
    answerLines: Seq[String],
    changeLink: Option[Call] = None,
    visuallyHiddenText: Option[String] = None)(implicit messages: Messages): AnswerRow =
    AnswerRow(id, question, answerLines, changeLink, Some(Messages("checkAnswers.change.button")), visuallyHiddenText)
}

case class CheckYourAnswers(
  amlsDetails: AnswerBlock,
  contactDetails: AnswerBlock,
  businessDetails: AnswerBlock,
  otherBusinessDetails: AnswerBlock,
  backLink: String)

case class CheckYourAnswersConfirmation(confirmed: Boolean)

/**
  * Configuration object to support rendering of check_your_answers template
  * Page contains a small form where the user must a agree by selecting a checkbox
  */
object CheckYourAnswers {

  def form(implicit messages: Messages): Form[CheckYourAnswersConfirmation] =
    Form[CheckYourAnswersConfirmation](
      mapping(
        "confirmed" -> default(boolean, false)
          .verifying(Messages("checkAnswers.confirm.error"), _ == true)
      )(CheckYourAnswersConfirmation.apply)(CheckYourAnswersConfirmation.unapply)
    )

  def apply(agentSession: AgentSession, countryName: String)(implicit messages: Messages): CheckYourAnswers =
    CheckYourAnswers(
      amlsDetails = AnswerBlock(
        heading = Messages("checkAnswers.amlsDetails.title"),
        answerGroups = List(
          makeAmlsRequiredGroup(agentSession),
          makeAmlsDetailsGroup(agentSession),
          makeAmlsFileUploadGroup(agentSession)
        ).flatten
      ),
      contactDetails = AnswerBlock(
        heading = Messages("checkAnswers.contactDetails.title"),
        answerGroups = List(
          makeContactDetailsGroup(agentSession)
        ).flatten
      ),
      businessDetails = AnswerBlock(
        heading = Messages("checkAnswers.BusinessDetails.title"),
        answerGroups = List(
          makeTradingNameGroup(agentSession),
          makeOverseasAddressGroup(agentSession, countryName),
          makeTradingAddressFileUploadGroup(agentSession)
        ).flatten
      ),
      otherBusinessDetails = AnswerBlock(
        heading = Messages("checkAnswers.OtherBusinessDetails.title"),
        answerGroups = List(
          makeRegistrationDataGroup(agentSession),
          makeTaxRegistrationNumbersFileUploadGroup(agentSession)
        ).flatten
      ),
      backLink = if (agentSession.taxRegistrationNumbers.exists(_.nonEmpty)) {
        routes.FileUploadController.showSuccessfulUploadedForm().url
      } else {
        routes.TaxRegController.showTaxRegistrationNumberForm().url
      }
    )

  private def makeAmlsRequiredGroup(session: AgentSession)(implicit messages: Messages): Option[AnswerGroup] = {

    val amlsRequired = session.amlsRequired.fold(false)(identity)
    Some(
      AnswerGroup(
        List(
          AnswerRow(
            id = "amls-details-amls-required",
            question = Messages("checkAnswers.amlsDetails.amlsRequired"),
            answerLines = List(Messages(s"checkAnswers.amlsDetails.amlsRequired.$amlsRequired")),
            changeLink = Some(routes.ChangingAnswersController.changeAmlsRequired())
          )
        )
      )
    )
  }

  private def makeAmlsDetailsGroup(session: AgentSession)(implicit messages: Messages): Option[AnswerGroup] =
    session.amlsDetails.map { details =>
      AnswerGroup(
        List(
          AnswerRow(
            id = "amls-details-supervisory-body",
            question = Messages("checkAnswers.amlsDetails.supervisoryBody"),
            answerLines = List(details.supervisoryBody),
            changeLink = Some(routes.ChangingAnswersController.changeAmlsDetails())
          ),
          AnswerRow(
            id = "amls-details",
            question = Messages("checkAnswers.amlsDetails.membershipNumber"),
            answerLines = List(details.membershipNumber).flatten
          )
        )
      )
    }

  private def formatFileName(fileName: String): String =
    if (fileName.length > 20)
      s"${fileName.take(10)}...${fileName.takeRight(10)}"
    else fileName

  private def makeAmlsFileUploadGroup(session: AgentSession)(implicit messages: Messages): Option[AnswerGroup] =
    session.amlsUploadStatus.flatMap(_.fileName).map { fileName =>
      AnswerGroup(
        List(
          AnswerRow(
            id = "tradingAddressFileName",
            question = Messages("checkAnswers.tradingAddressFile.title"),
            answerLines = List(formatFileName(fileName)),
            changeLink = Some(routes.ChangingAnswersController.changeTradingAddressFile())
          )
        )
      )
    }

  private def makeContactDetailsGroup(session: AgentSession)(implicit messages: Messages): Option[AnswerGroup] =
    session.contactDetails.map { details =>
      AnswerGroup(
        List(
          AnswerRow(
            id = "name",
            question = Messages("checkAnswers.contactDetails.name"),
            answerLines = List(s"${details.firstName} ${details.lastName}"),
            changeLink = Some(routes.ChangingAnswersController.changeContactDetails()),
            visuallyHiddenText = Some(Messages("checkAnswers.contactDetails.visuallyHiddenText"))
          ),
          AnswerRow(
            id = "jobTitle",
            question = Messages("checkAnswers.contactDetails.jobTitle"),
            answerLines = List(details.jobTitle)
          ),
          AnswerRow(
            id = "businessTelephone",
            question = Messages("checkAnswers.contactDetails.businessTelephone"),
            answerLines = List(details.businessTelephone)
          ),
          AnswerRow(
            id = "businessEmail",
            question = Messages("checkAnswers.contactDetails.businessEmail"),
            answerLines = List(details.businessEmail)
          )
        )
      )
    }

  private def makeOverseasAddressGroup(session: AgentSession, countryName: String)(
    implicit messages: Messages): Option[AnswerGroup] =
    session.overseasAddress.map { address =>
      AnswerGroup(
        List(
          AnswerRow(
            id = "mainBusinessAddressTitle",
            question = Messages("checkAnswers.mainBusinessAddress.title"),
            answerLines = List(
              Some(address.addressLine1),
              Some(address.addressLine2),
              address.addressLine3,
              address.addressLine4,
              Some(countryName)).flatten,
            changeLink = Some(routes.ChangingAnswersController.changeTradingAddress())
          )
        )
      )
    }

  private def makeTradingAddressFileUploadGroup(session: AgentSession)(
    implicit messages: Messages): Option[AnswerGroup] =
    session.tradingAddressUploadStatus.flatMap(_.fileName).map { fileName =>
      AnswerGroup(
        List(
          AnswerRow(
            id = "tradingAddressFileName",
            question = Messages("checkAnswers.tradingAddressFile.title"),
            answerLines = List(formatFileName(fileName)),
            Some(routes.ChangingAnswersController.changeTradingAddressFile())
          )
        )
      )
    }

  private def makeTradingNameGroup(session: AgentSession)(implicit messages: Messages): Option[AnswerGroup] =
    session.tradingName.map { name =>
      AnswerGroup(
        List(
          AnswerRow(
            id = "tradingName",
            question = Messages("checkAnswers.tradingName.title"),
            answerLines = List(name),
            changeLink = Some(routes.ChangingAnswersController.changeTradingName())
          )
        )
      )
    }

  def getAgentCodeRows(isRegistered: YesNo, session: AgentSession)(implicit messages: Messages): Seq[AnswerRow] =
    isRegistered match {
      case Yes =>
        if (session.agentCodes.exists(_.hasOneOrMoreCodes)) {
          val maybeSaRow = session.agentCodes.flatMap(_.selfAssessment).map { sa =>
            AnswerRow(
              id = "sa",
              question = Messages("checkAnswers.agentCode.selfAssessment"),
              answerLines = List(sa.value),
              Some(routes.ChangingAnswersController.changeAgentCodes())
            )
          }
          val maybeCtRow = session.agentCodes.flatMap(_.corporationTax).map { ct =>
            AnswerRow(
              id = "ct",
              question = Messages("checkAnswers.agentCode.corporationTax"),
              answerLines = List(ct.value),
              Some(routes.ChangingAnswersController.changeAgentCodes())
            )
          }
          List(maybeSaRow, maybeCtRow).flatten
        } else {
          List(
            AnswerRow(
              id = "agentCodeEmpty",
              question = Messages("checkAnswers.agentCode.title"),
              answerLines = List(Messages("checkAnswers.agentCode.empty")),
              Some(routes.ChangingAnswersController.changeAgentCodes())
            ))
        }
      case No => List.empty
    }

  def getUkRegistrationRows(session: AgentSession)(implicit messages: Messages): Seq[AnswerRow] =
    session.registeredForUkTax.fold(Seq.empty: Seq[AnswerRow]) { isRegisteredForUKTax =>
      val rows = List(
        AnswerRow(
          id = "isRegisteredForTax",
          question = Messages("checkAnswers.registeredForUKTax.title"),
          answerLines = List(isRegisteredForUKTax.value),
          changeLink = Some(routes.ChangingAnswersController.changeRegisteredForUKTax())
        )
      )
      val personalDetails = if (isRegisteredForUKTax == Yes) {
        val maybeNinoRow = session.personalDetails.flatMap(_.nino).map { nino =>
          AnswerRow(
            id = "nino",
            question = Messages("checkAnswers.personalDetails.nino.title"),
            answerLines = List(nino.nino),
            changeLink = Some(routes.ChangingAnswersController.changePersonalDetails())
          )
        }
        val maybeSaUtrRow = session.personalDetails.flatMap(_.saUtr).map { saUtr =>
          AnswerRow(
            id = "saUtr",
            question = Messages("checkAnswers.personalDetails.saUtr.title"),
            answerLines = List(saUtr.utr)
          )
        }
        List(maybeNinoRow, maybeSaUtrRow).flatten
      } else {
        List.empty
      }
      rows ++ personalDetails
    }

  private def makeRegistrationDataGroup(session: AgentSession)(implicit messages: Messages): Option[AnswerGroup] =
    session.registeredWithHmrc.map { isRegistered: YesNo =>
      val agentCodeRows = getAgentCodeRows(isRegistered, session)

      val ukRegistrationRows = getUkRegistrationRows(session)

      val regCompanyNoRows = session.companyRegistrationNumber
        .map(_.registrationNumber)
        .map { crn =>
          AnswerRow(
            id = "companyRegNo",
            question = Messages("checkAnswers.companyRegistrationNumber.title"),
            answerLines = List(crn.fold(Messages("checkAnswers.companyRegistrationNumber.empty"))(_.value)),
            changeLink = Some(
              routes.ChangingAnswersController
                .changeCompanyRegistrationNumber())
          )
        }
        .toList

      val regTaxNoRows = AnswerRow(
        id = "taxRegistrationNumbersTitle",
        question = Messages("checkAnswers.taxRegistrationNumbers.title"),
        answerLines = session.taxRegistrationNumbers match {
          case Some(taxNumbers) if taxNumbers.nonEmpty =>
            taxNumbers.toList.map(_.value)
          case _ => List(Messages("checkAnswers.taxRegistrationNumbers.empty"))
        },
        changeLink = Some(routes.ChangingAnswersController.changeYourTaxRegistrationNumbers())
      )

      AnswerGroup(
        List(
          AnswerRow(
            id = "isRegistered",
            question = Messages("checkAnswers.registeredWithHmrc.title"),
            answerLines = List(isRegistered.value),
            changeLink = Some(routes.ChangingAnswersController.changeRegisteredWithHmrc())
          )
        ) ++ agentCodeRows ++ ukRegistrationRows ++ regCompanyNoRows :+ regTaxNoRows
      )

    }

  private def makeTaxRegistrationNumbersFileUploadGroup(session: AgentSession)(
    implicit messages: Messages): Option[AnswerGroup] =
    if (session.taxRegistrationNumbers.fold(false)(_.nonEmpty)) {

      session.trnUploadStatus.flatMap(_.fileName).map { fileName =>
        AnswerGroup(
          List(
            AnswerRow(
              id = "trnFileName",
              question = Messages("checkAnswers.taxRegistrationNumbersFile.title"),
              answerLines = List(formatFileName(fileName)),
              changeLink = Some(routes.ChangingAnswersController
                .changeYourTaxRegistrationNumbersFile())
            )
          )
        )
      }
    } else {
      None
    }

}
