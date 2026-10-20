package com.fintrack.kavindu.core.model

// Fixed choices shared by all features. Firestore and Room store each value as its name, e.g. "USD".

enum class Currency { LKR, USD, USDT }

// No crypto cash-out here: crypto profit is calculated from CryptoTransfer entries and never
// saved as income, so it is never counted twice.
enum class IncomeSource { SALARY, FREELANCE, ADSENSE, OTHER }

enum class CategoryType { COMMITTED, DISCRETIONARY }

enum class ExpenseOrigin { MANUAL, RECURRING, BALANCE_CHECK }

enum class MilestoneStatus { PENDING, RECEIVED, CANCELLED }

enum class CryptoDirection { INTO_BINANCE, OUT_OF_BINANCE }
