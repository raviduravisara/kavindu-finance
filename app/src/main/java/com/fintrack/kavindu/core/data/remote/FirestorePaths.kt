package com.fintrack.kavindu.core.data.remote

// Firestore collection names, in one place so nobody mistypes them. Everything lives under users/{uid}.
object FirestorePaths {
    const val USERS = "users"
    const val INCOMES = "incomes"
    const val EXPENSES = "expenses"
    const val CATEGORIES = "categories"
    const val RECURRING_ITEMS = "recurringItems"
    const val GOALS = "goals"
    const val FREELANCE_PROJECTS = "freelanceProjects"
    const val MILESTONES = "milestones"
    const val CRYPTO_TRANSFERS = "cryptoTransfers"
    const val BALANCE_CHECKS = "balanceChecks"
}
