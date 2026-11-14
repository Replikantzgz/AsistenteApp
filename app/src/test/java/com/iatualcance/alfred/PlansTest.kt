package com.iatualcance.alfred

import com.iatualcance.alfred.billing.Entitlements
import com.iatualcance.alfred.billing.Feature
import com.iatualcance.alfred.billing.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlansTest {
    @Test fun billingOffUnlocksEverything() {
        Feature.entries.forEach { assertTrue(Entitlements.allows(false, null, it)); assertTrue(Entitlements.allows(false, Plan.BASIC, it)) }
        assertEquals(6.0, Entitlements.budgetEur(false, null, 6.0), 0.0)
    }
    @Test fun basicOnlyMailAndCalendar() {
        Feature.entries.forEach { assertFalse(Entitlements.allows(true, Plan.BASIC, it)); assertFalse(Entitlements.allows(true, null, it)) }
        Feature.entries.forEach { assertTrue(Entitlements.allows(true, Plan.PREMIUM, it)) }
    }
    @Test fun budgetsKeepAtLeastHalfMargin() {
        assertEquals(2.5, Entitlements.budgetEur(true, Plan.BASIC, 99.0), 0.0)
        assertEquals(6.0, Entitlements.budgetEur(true, Plan.PREMIUM, 99.0), 0.0)
        assertTrue(Plan.BASIC.marginEur / Plan.BASIC.priceNetEur >= 0.5)
        assertTrue(Plan.PREMIUM.marginEur >= 3.0)
    }
    @Test fun thresholds() {
        assertNull(Entitlements.threshold(0.0, 1.0, 2.5))
        assertEquals(80, Entitlements.threshold(1.9, 2.1, 2.5))
        assertNull(Entitlements.threshold(2.1, 2.2, 2.5))
        assertEquals(100, Entitlements.threshold(2.4, 2.6, 2.5))
        assertNull(Entitlements.threshold(0.0, 5.0, 0.0))
    }
    @Test fun oldLicensesKeepWorkingAsPremium() {
        assertEquals(Plan.PREMIUM, Plan.fromId("premium")); assertEquals(Plan.BASIC, Plan.fromId("basic")); assertEquals(Plan.PREMIUM, Plan.fromId("lo-que-sea"))
    }
}
