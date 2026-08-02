package com.famex.fixtures

import com.famex.core.model.Budget
import com.famex.core.model.Category
import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember
import com.famex.core.model.MemberRole
import com.famex.core.model.PaymentMode
import com.famex.core.model.Transaction
import com.famex.core.model.TransactionType
import com.famex.core.model.User
import com.famex.core.util.todayLocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

private val alex = User(1, "alex@example.com", "Alex Rivera", "Alex")
private val sam = User(2, "sam@example.com", "Sam Rivera", "Sam")
private val jo = User(3, "jo@example.com", "Jo Rivera", "Jo")

private fun member(user: User, role: MemberRole) = HouseholdMember(user.id, user, role, "Jan 2026")

private fun householdFor(scenario: DummyScenario): Household {
    val members = when (scenario) {
        DummyScenario.SoloBudgeter -> listOf(member(alex, MemberRole.OWNER))
        DummyScenario.FullHouseholdThreeMembers -> listOf(
            member(alex, MemberRole.OWNER),
            member(sam, MemberRole.MEMBER),
            member(jo, MemberRole.MEMBER)
        )
        else -> listOf(member(alex, MemberRole.OWNER), member(sam, MemberRole.MEMBER))
    }
    return Household(
        id = 1,
        name = "Rivera Household",
        currency = "USD",
        language = "en",
        cycleStartDay = 1,
        members = members
    )
}

private fun categoriesFor(scenario: DummyScenario): List<Category> = when (scenario) {
    DummyScenario.NoBudgetSetup -> emptyList()

    DummyScenario.EmptyBudgetNoTransactions -> listOf(
        Category(1, "Groceries", "cart", 800.0, 0.0),
        Category(2, "Dining Out", "restaurant", 400.0, 0.0),
        Category(3, "Utilities", "flash", 350.0, 0.0),
        Category(4, "Transportation", "car", 300.0, 0.0),
    )

    DummyScenario.NearLimitAmber -> listOf(
        Category(1, "Groceries", "cart", 800.0, 620.0),
        Category(2, "Dining Out", "restaurant", 400.0, 380.0),
        Category(3, "Utilities", "flash", 350.0, 300.0),
        Category(4, "Transportation", "car", 300.0, 230.0),
    )

    DummyScenario.OverBudgetCoral -> listOf(
        Category(1, "Groceries", "cart", 800.0, 680.0),
        Category(2, "Dining Out", "restaurant", 400.0, 420.0),
        Category(3, "Utilities", "flash", 350.0, 210.0),
        Category(4, "Transportation", "car", 300.0, 150.0),
    )

    else -> listOf(
        Category(1, "Groceries", "cart", 800.0, 620.0),
        Category(2, "Dining Out", "restaurant", 400.0, 210.0),
        Category(3, "Utilities", "flash", 350.0, 190.0),
        Category(4, "Transportation", "car", 300.0, 150.0),
    )
}

private fun budgetFor(scenario: DummyScenario, categories: List<Category>): Budget? {
    if (scenario == DummyScenario.NoBudgetSetup) return null
    val goal = if (scenario == DummyScenario.OverBudgetCoral) 1400.0 else 1850.0
    return Budget(
        id = 1,
        name = "This Month's Household Budget",
        monthlyGoalAmount = goal,
        spentAmount = categories.sumOf { it.amountSpent },
        month = 7,
        year = 2026
    )
}

private fun baseTransactions(scenario: DummyScenario): List<Transaction> {
    if (scenario == DummyScenario.EmptyBudgetNoTransactions || scenario == DummyScenario.NoBudgetSetup) {
        return emptyList()
    }

    val payers = if (scenario == DummyScenario.SoloBudgeter) listOf(alex) else listOf(alex, sam, jo)

    val template = listOf(
        Triple("Whole Foods Market", 142.50, "Groceries" to 1L),
        Triple("Electricity Bill", 110.00, "Utilities" to 3L),
        Triple("Starbucks", 18.25, "Dining Out" to 2L),
        Triple("Uber", 24.00, "Transportation" to 4L),
        Triple("Trader Joe's", 76.40, "Groceries" to 1L),
    )
    val paymentModes = listOf(PaymentMode.CARD, PaymentMode.BANK_TRANSFER, PaymentMode.CASH, PaymentMode.CARD, PaymentMode.OTHER)

    val count = if (scenario == DummyScenario.LongTransactionHistory) 40 else template.size
    val today = todayLocalDate()

    return (0 until count).map { index ->
        val (merchant, amount, categoryPair) = template[index % template.size]
        val (categoryName, categoryId) = categoryPair
        Transaction(
            id = (index + 1).toLong(),
            merchant = merchant,
            amount = amount,
            type = TransactionType.EXPENSE,
            paymentMode = paymentModes[index % paymentModes.size],
            categoryId = categoryId,
            categoryName = categoryName,
            paidBy = payers[index % payers.size],
            transactionDate = today.minus(index + 1, DateTimeUnit.DAY),
            transactionDateText = "${index + 1}d ago",
            createdAtText = "${index + 1}d ago"
        )
    }
}

fun dummyHousehold(scenario: DummyScenario): Household = householdFor(scenario)

fun dummyCategories(scenario: DummyScenario): List<Category> = categoriesFor(scenario)

fun dummyBudget(scenario: DummyScenario): Budget? = budgetFor(scenario, categoriesFor(scenario))

fun dummyDashboardActivityFeed(scenario: DummyScenario): List<Transaction> = baseTransactions(scenario).take(5)

fun dummyTransactionHistory(scenario: DummyScenario): List<Transaction> = baseTransactions(scenario)

fun dummyCurrentUser(): User = alex
