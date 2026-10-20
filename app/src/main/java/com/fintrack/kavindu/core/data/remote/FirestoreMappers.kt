package com.fintrack.kavindu.core.data.remote

import android.util.Log
import com.fintrack.kavindu.core.data.local.entity.BalanceCheckEntity
import com.fintrack.kavindu.core.data.local.entity.CategoryEntity
import com.fintrack.kavindu.core.data.local.entity.CryptoTransferEntity
import com.fintrack.kavindu.core.data.local.entity.ExpenseEntity
import com.fintrack.kavindu.core.data.local.entity.FreelanceProjectEntity
import com.fintrack.kavindu.core.data.local.entity.GoalEntity
import com.fintrack.kavindu.core.data.local.entity.IncomeEntity
import com.fintrack.kavindu.core.data.local.entity.MilestoneEntity
import com.fintrack.kavindu.core.data.local.entity.ProfileEntity
import com.fintrack.kavindu.core.data.local.entity.RecurringItemEntity
import com.fintrack.kavindu.core.model.BalanceCheck
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.FreelanceProject
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.RecurringItem
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

// Firestore documents <-> app data. In Firestore, dates are "yyyy-MM-dd" text, fixed choices are
// their names, and money is Long cents. The document ID is the id; it is not stored as a field.

private const val TAG = "FirestoreMappers"

// A broken document is skipped and logged instead of crashing the app.
private inline fun <T> DocumentSnapshot.readOrNull(read: DocumentSnapshot.() -> T): T? =
    try {
        read()
    } catch (e: Exception) {
        Log.w(TAG, "Skipping malformed document ${reference.path}", e)
        null
    }

private fun DocumentSnapshot.text(field: String): String = getString(field) ?: error("$field is missing")
private fun DocumentSnapshot.long(field: String): Long = getLong(field) ?: error("$field is missing")
private fun DocumentSnapshot.int(field: String): Int = long(field).toInt()
private fun DocumentSnapshot.bool(field: String, default: Boolean): Boolean = getBoolean(field) ?: default
private fun DocumentSnapshot.date(field: String): LocalDate = LocalDate.parse(text(field))
private fun DocumentSnapshot.dateOrNull(field: String): LocalDate? = getString(field)?.let(LocalDate::parse)
private inline fun <reified T : Enum<T>> DocumentSnapshot.choice(field: String): T = enumValueOf(text(field))

// ---------- Firestore -> Room (used by the real-time listeners) ----------

fun DocumentSnapshot.toProfileEntity(): ProfileEntity? = readOrNull {
    ProfileEntity(
        id = id,
        email = text("email"),
        cycleStartDay = int("cycleStartDay"),
        baseCurrency = choice("baseCurrency"),
        lastChannel = getString("lastChannel"),
        lastAccount = getString("lastAccount"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toIncomeEntity(): IncomeEntity? = readOrNull {
    IncomeEntity(
        id = id,
        source = choice("source"),
        amountCents = long("amountCents"),
        currency = choice("currency"),
        lkrAmountCents = long("lkrAmountCents"),
        rateCentsPerUnit = long("rateCentsPerUnit"),
        date = date("date"),
        note = getString("note"),
        projectId = getString("projectId"),
        milestoneId = getString("milestoneId"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toExpenseEntity(): ExpenseEntity? = readOrNull {
    ExpenseEntity(
        id = id,
        amountCents = long("amountCents"),
        currency = choice("currency"),
        date = date("date"),
        categoryId = text("categoryId"),
        categoryType = choice("categoryType"),
        channel = text("channel"),
        account = text("account"),
        note = getString("note"),
        origin = choice("origin"),
        recurringItemId = getString("recurringItemId"),
        isConfirmed = bool("isConfirmed", default = true),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toCategoryEntity(): CategoryEntity? = readOrNull {
    CategoryEntity(
        id = id,
        name = text("name"),
        type = choice("type"),
        sortOrder = int("sortOrder"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toRecurringItemEntity(): RecurringItemEntity? = readOrNull {
    RecurringItemEntity(
        id = id,
        name = text("name"),
        amountCents = long("amountCents"),
        categoryId = text("categoryId"),
        dayOfMonth = int("dayOfMonth"),
        isEstimate = bool("isEstimate", default = false),
        isSubscription = bool("isSubscription", default = false),
        isActive = bool("isActive", default = true),
        lastReviewedDate = dateOrNull("lastReviewedDate"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toGoalEntity(): GoalEntity? = readOrNull {
    GoalEntity(
        id = id,
        name = text("name"),
        targetCents = long("targetCents"),
        deadline = date("deadline"),
        startingSavingsCents = long("startingSavingsCents"),
        startDate = date("startDate"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toFreelanceProjectEntity(): FreelanceProjectEntity? = readOrNull {
    FreelanceProjectEntity(
        id = id,
        clientName = text("clientName"),
        projectName = text("projectName"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toMilestoneEntity(): MilestoneEntity? = readOrNull {
    MilestoneEntity(
        id = id,
        projectId = text("projectId"),
        title = text("title"),
        amountCents = long("amountCents"),
        currency = choice("currency"),
        dueDate = date("dueDate"),
        status = choice("status"),
        incomeId = getString("incomeId"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toCryptoTransferEntity(): CryptoTransferEntity? = readOrNull {
    CryptoTransferEntity(
        id = id,
        direction = choice("direction"),
        amountCents = long("amountCents"),
        date = date("date"),
        note = getString("note"),
        createdAt = long("createdAt"),
    )
}

fun DocumentSnapshot.toBalanceCheckEntity(): BalanceCheckEntity? = readOrNull {
    BalanceCheckEntity(
        id = id,
        date = date("date"),
        bankBalanceCents = long("bankBalanceCents"),
        walletCashCents = long("walletCashCents"),
        isStartingPoint = bool("isStartingPoint", default = false),
        expectedCents = getLong("expectedCents"),
        gapCents = getLong("gapCents"),
        resultExpenseId = getString("resultExpenseId"),
        resultIncomeId = getString("resultIncomeId"),
        createdAt = long("createdAt"),
    )
}

// ---------- App -> Firestore (used by the repositories when saving) ----------

fun Income.toFirestoreMap(): Map<String, Any?> = mapOf(
    "source" to source.name,
    "amountCents" to amountCents,
    "currency" to currency.name,
    "lkrAmountCents" to lkrAmountCents,
    "rateCentsPerUnit" to rateCentsPerUnit,
    "date" to date.toString(),
    "note" to note,
    "projectId" to projectId,
    "milestoneId" to milestoneId,
    "createdAt" to createdAt,
)

fun Expense.toFirestoreMap(): Map<String, Any?> = mapOf(
    "amountCents" to amountCents,
    "currency" to currency.name,
    "date" to date.toString(),
    "categoryId" to categoryId,
    "categoryType" to categoryType.name,
    "channel" to channel,
    "account" to account,
    "note" to note,
    "origin" to origin.name,
    "recurringItemId" to recurringItemId,
    "isConfirmed" to isConfirmed,
    "createdAt" to createdAt,
)

fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "type" to type.name,
    "sortOrder" to sortOrder,
    "createdAt" to createdAt,
)

fun RecurringItem.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "amountCents" to amountCents,
    "categoryId" to categoryId,
    "dayOfMonth" to dayOfMonth,
    "isEstimate" to isEstimate,
    "isSubscription" to isSubscription,
    "isActive" to isActive,
    "lastReviewedDate" to lastReviewedDate?.toString(),
    "createdAt" to createdAt,
)

fun Goal.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "targetCents" to targetCents,
    "deadline" to deadline.toString(),
    "startingSavingsCents" to startingSavingsCents,
    "startDate" to startDate.toString(),
    "createdAt" to createdAt,
)

fun FreelanceProject.toFirestoreMap(): Map<String, Any?> = mapOf(
    "clientName" to clientName,
    "projectName" to projectName,
    "createdAt" to createdAt,
)

fun Milestone.toFirestoreMap(): Map<String, Any?> = mapOf(
    "projectId" to projectId,
    "title" to title,
    "amountCents" to amountCents,
    "currency" to currency.name,
    "dueDate" to dueDate.toString(),
    "status" to status.name,
    "incomeId" to incomeId,
    "createdAt" to createdAt,
)

fun CryptoTransfer.toFirestoreMap(): Map<String, Any?> = mapOf(
    "direction" to direction.name,
    "amountCents" to amountCents,
    "date" to date.toString(),
    "note" to note,
    "createdAt" to createdAt,
)

fun BalanceCheck.toFirestoreMap(): Map<String, Any?> = mapOf(
    "date" to date.toString(),
    "bankBalanceCents" to bankBalanceCents,
    "walletCashCents" to walletCashCents,
    "isStartingPoint" to isStartingPoint,
    "expectedCents" to expectedCents,
    "gapCents" to gapCents,
    "resultExpenseId" to resultExpenseId,
    "resultIncomeId" to resultIncomeId,
    "createdAt" to createdAt,
)
