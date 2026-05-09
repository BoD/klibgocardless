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
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.jraf.klibgocardless.client.GoCardlessClient
import org.jraf.klibgocardless.client.configuration.ClientConfiguration
import org.jraf.klibgocardless.client.configuration.HttpLoggingLevel
import org.jraf.klibgocardless.internal.json.JsonAmount
import org.jraf.klibgocardless.internal.json.JsonErrorResponse
import org.jraf.klibgocardless.internal.json.JsonTransactionsSuccessResponse
import org.jraf.klibgocardless.model.Transaction
import org.jraf.klibnanolog.logd
import java.math.BigDecimal

internal class GoCardlessClientImpl(private val clientConfiguration: ClientConfiguration) : GoCardlessClient {
  private val service: GoCardlessService by lazy {
    GoCardlessService(
      provideHttpClient(clientConfiguration),
    )
  }

  private fun provideHttpClient(clientConfiguration: ClientConfiguration): HttpClient {
    return HttpClient {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            useAlternativeNames = false
            encodeDefaults = true
          },
        )
      }
      install(Auth) {
        bearer {
          refreshTokens {
            service.newToken(secretId = clientConfiguration.secretId, secretKey = clientConfiguration.secretKey)
          }
        }
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
      }
      engine {
        // Setup a proxy if requested
        clientConfiguration.httpConfiguration.httpProxy?.let { httpProxy ->
          proxy = ProxyBuilder.http(
            URLBuilder().apply {
              host = httpProxy.host
              port = httpProxy.port
            }.build(),
          )
        }
      }
      // Setup logging if requested
      if (clientConfiguration.httpConfiguration.loggingLevel != HttpLoggingLevel.NONE) {
        install(Logging) {
          logger = object : Logger {
            override fun log(message: String) {
              logd("http - $message")
            }
          }
          level = when (clientConfiguration.httpConfiguration.loggingLevel) {
            HttpLoggingLevel.NONE -> LogLevel.NONE
            HttpLoggingLevel.INFO -> LogLevel.INFO
            HttpLoggingLevel.HEADERS -> LogLevel.HEADERS
            HttpLoggingLevel.BODY -> LogLevel.BODY
            HttpLoggingLevel.ALL -> LogLevel.ALL
          }
        }
      }
    }
  }

  override suspend fun getTransactions(accountId: String): Result<List<Transaction>> {
    val transactionsResult = runCatching { service.getTransactions(accountId) }
    if (transactionsResult.isFailure) {
      return Result.failure(transactionsResult.exceptionOrNull()!!)
    }
    return when (val response = transactionsResult.getOrThrow()) {
      is JsonErrorResponse -> Result.failure(GoCardlessClient.GoCardlessServiceException(response))
      is JsonTransactionsSuccessResponse -> Result.success(
        response.transactions.booked.map { jsonTransaction ->
          Transaction(
            internalId = jsonTransaction.internalTransactionId,
            id = jsonTransaction.transactionId,
            date = LocalDate.parse(jsonTransaction.bookingDate),
            amount = jsonTransaction.transactionAmount.toBigDecimal(),
            label = jsonTransaction.remittanceInformationUnstructuredArray
              .sorted()
              .joinToString(" / ")
              .replace("\n", " ")
              .replace(Regex("\\s+"), " "),
          )
        }
          // Newest transactions are first, for Slack messages we want the opposite
          .reversed(),
      )

      else -> error("Unknown response: $response")
    }
  }


  private fun JsonAmount.toBigDecimal() = BigDecimal(amount)

  // {"balances": [{"balanceAmount": {"amount": "9567.73", "currency": "EUR"}, "balanceType": "expected", "referenceDate": "2025-06-10"}]}
  // {"balances": [{"balanceAmount": {"amount": "8016.42", "currency": "EUR"}, "balanceType": "expected", "referenceDate": "2025-06-09"}, {"balanceAmount": {"amount": "8016.42", "currency": "EUR"}, "balanceType": "closingBooked", "referenceDate": "2025-06-09"}]}
  // {"balances": [{"balanceAmount": {"amount": "28537.33", "currency": "EUR"}, "balanceType": "closingBooked"}]}
  override suspend fun getBalance(accountId: String): Result<BigDecimal> {
    return runCatching {
      service.getBalances(accountId).balances.first().balanceAmount.toBigDecimal()
    }
  }

  override suspend fun createEndUserAgreement(institutionId: String): Result<String> {
    return runCatching {
      service.createEndUserAgreement(institutionId).id
    }
  }

  override suspend fun createRequisition(institutionId: String, agreementId: String): Result<String> {
    return runCatching {
      service.createRequisition(institutionId, agreementId).link
    }
  }
}
