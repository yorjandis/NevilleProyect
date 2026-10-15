package com.ypg.neville.model.db.room

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotaArithmeticCalculationTest {
    @Test
    fun `plain note evaluates every expression with precedence and decimal comma`() {
        val calculation = NotaArithmeticCalculation.calculate(
            "Comida =10+2*3\nTransporte = 4,5/2"
        )

        assertEquals(0, calculation?.value?.compareTo(BigDecimal("18.25")))
        assertEquals(2, calculation?.individual?.size)
    }

    @Test
    fun `inline annotations are inserted updated and removed`() {
        val annotated = NotaArithmeticCalculation.annotateOperations(
            text = "Compra =10+5",
            includeTrailingExpression = true,
            locale = Locale.US
        )

        assertEquals("Compra =10+5->(15)", annotated)
        assertEquals(
            "Compra =10+5->(15)",
            NotaArithmeticCalculation.annotateOperations(
                text = "Compra =10+5->(9)",
                includeTrailingExpression = true,
                locale = Locale.US
            )
        )
        assertEquals("Compra =10+5", NotaArithmeticCalculation.removeOperationAnnotations(annotated))
    }

    @Test
    fun `checklist includes only first operation of tasks and reports pending`() {
        val items = listOf(
            NotaChecklistItem("lead", "Presupuesto =999", false, NotaChecklistItemKind.LEADING_NOTE),
            NotaChecklistItem("one", "Pan =2+3 y extra =100", true),
            NotaChecklistItem("two", "Leche =4", false),
            NotaChecklistItem("tail", "Fin =999", false, NotaChecklistItemKind.TRAILING_NOTE)
        )

        val summary = NotaArithmeticCalculation.checklistSummary(items)

        assertEquals(0, summary?.total?.compareTo(BigDecimal("9")))
        assertEquals(0, summary?.checked?.compareTo(BigDecimal("5")))
        assertEquals(0, summary?.pending?.compareTo(BigDecimal("4")))
    }

    @Test
    fun `invalid expressions and division by zero are ignored`() {
        assertNull(NotaArithmeticCalculation.calculate("Nada = texto"))
        assertNull(NotaArithmeticCalculation.calculate("Error =10/0"))
        assertFalse(NotaArithmeticCalculation.isValidExpression("10/0"))
        assertTrue(NotaArithmeticCalculation.isValidExpression("-10+2"))
    }
}
