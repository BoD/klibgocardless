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

@file:Suppress("PropertyName")

package org.jraf.klibgocardless.internal.json

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
internal data class JsonErrorResponse(
  val summary: String,
  val detail: String,
  val status_code: Int? = null,
) : JsonTransactionsResponse()

@Serializable(with = JsonTransactionsResponseSerializer::class)
internal abstract class JsonTransactionsResponse

@Serializable
internal data class JsonTransactionsSuccessResponse(
  val transactions: JsonTransactionsTransactions,
) : JsonTransactionsResponse()

internal object JsonTransactionsResponseSerializer :
  JsonContentPolymorphicSerializer<JsonTransactionsResponse>(JsonTransactionsResponse::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonTransactionsResponse> {
    return when {
      element.jsonObject.containsKey("summary") -> JsonErrorResponse.serializer()
      element.jsonObject.containsKey("transactions") -> JsonTransactionsSuccessResponse.serializer()
      else -> error("Unknown JSON response: $element")
    }
  }
}

@Serializable
internal data class JsonTransactionsTransactions(
  val booked: List<JsonTransactionsTransaction>,
)

@Serializable
internal data class JsonTransactionsTransaction(
  val internalTransactionId: String,
  val transactionId: String? = null,
  val bookingDate: String,
  val transactionAmount: JsonAmount,
  val remittanceInformationUnstructuredArray: List<String>,
)

