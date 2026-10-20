package com.fintrack.kavindu.core.model

// App defaults, not user data: the starter categories are written into a new user's
// categories once, on first register, and can then be edited. Channels and accounts are fixed lists.
object StarterData {

    val categories: List<Pair<String, CategoryType>> = listOf(
        "Rent" to CategoryType.COMMITTED,
        "Utilities" to CategoryType.COMMITTED,
        "Gym" to CategoryType.COMMITTED,
        "Subscriptions" to CategoryType.COMMITTED,
        "Groceries" to CategoryType.COMMITTED,
        "Coffee & dining" to CategoryType.DISCRETIONARY,
        "Food delivery" to CategoryType.DISCRETIONARY,
        "Transport" to CategoryType.DISCRETIONARY,
        "Shopping" to CategoryType.DISCRETIONARY,
        "Entertainment" to CategoryType.DISCRETIONARY,
        "Other" to CategoryType.DISCRETIONARY,
    )

    val channels: List<String> = listOf("Card", "Cash", "PickMe", "UberEats", "Bank transfer")

    val accounts: List<String> = listOf("Main account", "Secondary account", "Cash wallet")
}
