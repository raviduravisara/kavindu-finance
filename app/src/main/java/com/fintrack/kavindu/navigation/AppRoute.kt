package com.fintrack.kavindu.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation routes. One @Serializable route per screen (no string routes).
// No arguments -> data object; with arguments -> data class.

// Ravidu: auth, settings, balance check
@Serializable data object Login
@Serializable data object Register
@Serializable data object ForgotPassword
@Serializable data object Settings
@Serializable data object WeeklyBalanceCheck // not "BalanceCheck": that name is the model class

// Member 2: income, goal, freelance, crypto
@Serializable data object IncomeList
@Serializable data class EditIncome(val incomeId: String? = null) // null = new income
@Serializable data object GoalOverview // not "Goal": that name is the model class
@Serializable data class EditGoal(val startNew: Boolean = false) // true = "Set a new goal" after one is reached
@Serializable data object FreelanceProjects
@Serializable data class FreelanceProjectDetail(val projectId: String)
@Serializable data object Crypto

// Member 3: dashboard, expenses, categories, recurring
@Serializable data object Dashboard
@Serializable data object QuickAddExpense
@Serializable data object TransactionHistory
@Serializable data class EditExpense(val expenseId: String)
@Serializable data object Categories
@Serializable data object RecurringItems // also holds subscription review (S6)
