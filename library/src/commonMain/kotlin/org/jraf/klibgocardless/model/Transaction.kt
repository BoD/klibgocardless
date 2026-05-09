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

package org.jraf.klibgocardless.model

import kotlinx.datetime.LocalDate
import java.math.BigDecimal

data class Transaction(
  val internalId: String,
  val id: String?,
  val date: LocalDate,
  val amount: BigDecimal,
  val label: String,
) {
  // We get the same transactions multiple times with different internal ids, but same transactionId ¯\_(ツ)_/¯
  // So base equality on that fact.
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Transaction

    if (internalId == other.internalId) return true
    if (id != null && id == other.id) return true
    return false
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: internalId.hashCode()
  }
}
