package com.fintrack.kavindu.feature.settings.data

import android.content.Context
import android.net.Uri
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.feature.settings.viewmodel.CsvBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/**
 * Exports all income and expenses to a CSV file the user picked with Android's "Save as" screen.
 * The data comes from Room through the existing repositories, so the export works offline.
 */
class CsvExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
) {
    /** Writes the file and returns how many entries it holds. */
    suspend fun exportTo(uri: Uri): Result<Int> = try {
        val incomes = incomeRepository.observeIncomesSince(ALL_TIME).first()
        val expenses = expenseRepository.observeExpensesSince(ALL_TIME).first()
        val categories = categoryRepository.observeCategories().first()
        val csv = CsvBuilder.build(incomes, expenses, categories)

        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("Could not open the file")
            stream.use {
                it.write(UTF8_BOM) // tells Excel the file is UTF-8, so Sinhala or other notes show correctly
                it.write(csv.toByteArray(Charsets.UTF_8))
            }
        }
        Result.success(incomes.size + expenses.size)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        // "Everything": Room compares dates as ISO text, and every entry in the app is dated after 2000.
        val ALL_TIME: LocalDate = LocalDate.of(2000, 1, 1)
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
