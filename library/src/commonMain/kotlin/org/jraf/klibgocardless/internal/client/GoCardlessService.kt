/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2026-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 * and contributors (https://github.com/BoD/klibgocardless/graphs/contributors)
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

package org.jraf.klibgocardless.internal.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.jraf.klibgocardless.internal.json.JsonBalancesResponse
import org.jraf.klibgocardless.internal.json.JsonCreateEndUserAgreementRequest
import org.jraf.klibgocardless.internal.json.JsonCreateRequisitionRequest
import org.jraf.klibgocardless.internal.json.JsonEndUserAgreement
import org.jraf.klibgocardless.internal.json.JsonRequisition
import org.jraf.klibgocardless.internal.json.JsonTokenNewRequest
import org.jraf.klibgocardless.internal.json.JsonTokenNewResponse
import org.jraf.klibgocardless.internal.json.JsonTransactionsResponse

internal class GoCardlessService(
  private val httpClient: HttpClient,
) {
  companion object {
    private const val URL_BASE = "https://bankaccountdata.gocardless.com/api/v2"
  }

  suspend fun newToken(secretId: String, secretKey: String): BearerTokens {
    return httpClient.post("$URL_BASE/token/new/") {
      contentType(ContentType.Application.Json)
      setBody(JsonTokenNewRequest(secret_id = secretId, secret_key = secretKey))
    }.body<JsonTokenNewResponse>()
      .toBearerTokens()
  }

  suspend fun getTransactions(accountId: String): JsonTransactionsResponse {
    return httpClient.get("$URL_BASE/accounts/$accountId/transactions/") {
      contentType(ContentType.Application.Json)
      parameter("date_from", "2020-01-01")
    }.body()
  }

  suspend fun getBalances(accountId: String): JsonBalancesResponse {
    return httpClient.get("$URL_BASE/accounts/$accountId/balances/") {
      contentType(ContentType.Application.Json)
    }.body()
  }

  suspend fun createEndUserAgreement(institutionId: String): JsonEndUserAgreement {
    return httpClient.post("$URL_BASE/agreements/enduser/") {
      contentType(ContentType.Application.Json)
      setBody(JsonCreateEndUserAgreementRequest(institution_id = institutionId))
    }.body()
  }

  suspend fun createRequisition(institutionId: String, agreementId: String): JsonRequisition {
    return httpClient.post("$URL_BASE/requisitions/") {
      contentType(ContentType.Application.Json)
      setBody(JsonCreateRequisitionRequest(institution_id = institutionId, agreement = agreementId))
    }.body()
  }
}

private fun JsonTokenNewResponse.toBearerTokens(): BearerTokens {
  return BearerTokens(
    accessToken = access,
    refreshToken = refresh,
  )
}
