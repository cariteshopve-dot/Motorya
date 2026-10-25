package agency.lacosta.motorya.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class DomainTest {
    private val d = LocalDate.of(2026, 10, 25)

    private fun fill(km: Long, liters: Double, full: Boolean = true) =
        FuelFill(vehicleId = 1, date = d, odometerKm = km, milliliters = (liters * 1000).toLong(), fullTank = full)

    @Test fun efficiencyNeedsTwoFullTanks() {
        assertNull(Efficiency.kmPerLiter(listOf(fill(1000, 40.0))))
        assertEquals(10.0, Efficiency.kmPerLiter(listOf(fill(1000, 40.0), fill(1400, 40.0)))!!, 0.001)
    }

    @Test fun partialFillsCountInTheNextFullTank() {
        val fills = listOf(fill(1000, 40.0), fill(1200, 10.0, full = false), fill(1500, 40.0))
        assertEquals(10.0, Efficiency.kmPerLiter(fills)!!, 0.001)
    }

    @Test fun partialBeforeFirstFullIsIgnored() {
        val fills = listOf(fill(800, 20.0, full = false), fill(1000, 40.0), fill(1450, 45.0))
        assertEquals(10.0, Efficiency.kmPerLiter(fills)!!, 0.001)
    }

    @Test fun lastSegment() {
        val fills = listOf(fill(1000, 40.0), fill(1400, 40.0), fill(1700, 25.0))
        assertEquals(12.0, Efficiency.lastKmPerLiter(fills)!!, 0.001)
        assertEquals(700.0 / 65.0, Efficiency.kmPerLiter(fills)!!, 0.001)
    }

    @Test fun serviceByKmOrDateWhicheverFirst() {
        val oil = Service(vehicleId = 1, kind = ServiceKind.OIL, lastKm = 80_000, lastDate = d.minusMonths(2))
        assertEquals(85_000L, oil.nextKm)
        assertEquals(ServiceStatus.OK, oil.status(82_000, d))
        assertEquals(ServiceStatus.SOON, oil.status(84_600, d))
        assertEquals(ServiceStatus.DUE, oil.status(85_000, d))
        // Por fecha: 6 meses vencen antes de llegar a los km.
        assertEquals(ServiceStatus.DUE, oil.status(81_000, d.plusMonths(4)))
        assertEquals(ServiceStatus.SOON, oil.status(81_000, d.plusMonths(4).minusDays(10)))
        val renewed = oil.done(85_100, d)
        assertEquals(90_100L, renewed.nextKm)
    }

    @Test fun serviceOnlyByDate() {
        val battery = Service(vehicleId = 1, kind = ServiceKind.BATTERY, lastKm = 0, lastDate = d)
        assertNull(battery.kmLeft(999_999))
        assertEquals(ServiceStatus.OK, battery.status(999_999, d))
    }

    @Test fun urgentFirst() {
        val a = Service(id = 1, vehicleId = 1, kind = ServiceKind.TIRES, lastKm = 80_000, lastDate = d)
        val b = Service(id = 2, vehicleId = 1, kind = ServiceKind.OIL, lastKm = 80_000, lastDate = d)
        val c = Service(id = 3, vehicleId = 1, kind = ServiceKind.BRAKES, lastKm = 60_000, lastDate = d)
        assertEquals(listOf(3L, 2L, 1L), listOf(a, b, c).sortedByUrgency(84_800, d).map { it.id })
    }

    @Test fun paperReminders() {
        val p = Paper(vehicleId = 1, kind = PaperKind.INSURANCE, expires = d.plusDays(7))
        assertTrue(p.remindToday(d))
        assertFalse(p.remindToday(d.plusDays(1)))
        assertTrue(p.remindToday(d.plusDays(7)))
        assertFalse(p.remindToday(d.plusDays(8)))
    }

    @Test fun moneyConversionUsesTheRateOfThatDay() {
        val bs = Money(365_200, Currency.VES, vesPerUsdCents = 3652) // Bs 3.652,00 a 36,52
        assertEquals(10_000L, bs.usdCents())
        val usd = Money(2_000, Currency.USD, vesPerUsdCents = 3652)
        assertEquals(73_040L, usd.vesCents())
        assertNull(Money(100, Currency.VES).usdCents())
    }

    @Test fun monthTotalInBothCurrencies() {
        val rate = 4000L
        val list = listOf(
            Expense(vehicleId = 1, date = d, category = ExpenseCategory.FUEL, amount = Money(2_000, Currency.USD, rate)),
            Expense(vehicleId = 1, date = d, category = ExpenseCategory.WASH, amount = Money(40_000, Currency.VES, rate)),
            Expense(vehicleId = 1, date = d.minusMonths(1), category = ExpenseCategory.FUEL, amount = Money(9_999, Currency.USD, rate)),
            Expense(vehicleId = 1, date = d, category = ExpenseCategory.OTHER, amount = Money(500, Currency.USD)),
        )
        val t = list.monthTotal(YearMonth.of(2026, 10))
        assertEquals(2_000L + 1_000 + 500, t.usdCents)
        assertEquals(80_000L + 40_000, t.vesCents)
        assertEquals(1, t.unconverted)
        assertEquals(2_000L, list.byCategory(YearMonth.of(2026, 10))[ExpenseCategory.FUEL])
    }

    @Test fun freePlanAllowsOneVehicle() {
        assertTrue(Plan.canAddVehicle(0, isPro = false))
        assertFalse(Plan.canAddVehicle(1, isPro = false))
        assertTrue(Plan.canAddVehicle(5, isPro = true))
    }
}

class NumbersTest {
    @Test fun parsesLocalFormats() {
        assertEquals(40_500L, Numbers.parse("40,5", 1000))
        assertEquals(40_500L, Numbers.parse("40.5", 1000))
        assertEquals(84_520L, Numbers.parse("84.520", 1))
        assertEquals(84_520L, Numbers.parse("84520", 1))
        assertEquals(123_450L, Numbers.parse("1.234,50", 100))
        assertEquals(3_652L, Numbers.parse("36,52", 100))
        assertNull(Numbers.parse("", 1))
        assertNull(Numbers.parse("abc", 1))
        assertNull(Numbers.parse("-3", 1))
    }

    @Test fun formatsLocal() {
        assertEquals("84.520", Numbers.km(84_520))
        assertEquals("1.234,50", Numbers.format(123_450, 100, 2))
        assertEquals("11,2", Numbers.format(112, 10, 1))
        assertEquals("0", Numbers.km(0))
    }
}
