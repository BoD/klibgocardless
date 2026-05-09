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

import org.jraf.klibgocardless.client.GoCardlessClient
import org.jraf.klibgocardless.client.configuration.ClientConfiguration
import org.jraf.klibgocardless.client.configuration.HttpConfiguration
import org.jraf.klibgocardless.client.configuration.HttpLoggingLevel
import java.io.File
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
suspend fun main(av: Array<String>) {
  val goCardlessClient = GoCardlessClient.newInstance(
    ClientConfiguration(
      secretId = av[0],
      secretKey = av[1],
      httpConfiguration = HttpConfiguration(
        loggingLevel = HttpLoggingLevel.ALL,
      ),
    ),
  )

  val transactions = goCardlessClient.getTransactions(av[2])
  val outputFile = File("transactions.csv")
  outputFile.printWriter().use { writer ->
    writer.println("Date,Id,Label,Amount")
    for (transaction in transactions.getOrThrow()) {
      writer.print(transaction.date)
      writer.print(",")
      writer.print(transaction.internalId)
      writer.print(",")
      writer.print(""""${transaction.label}"""")
      writer.print(",")
      writer.println(transaction.amount)
    }
  }
}
