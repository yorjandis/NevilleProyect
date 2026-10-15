package com.ypg.neville.model.db.room

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Evaluates the inline arithmetic syntax shared with the iOS notes editor.
 * Every expression starts after an '=' and supports +, -, *, / and decimal comma or point.
 */
object NotaArithmeticCalculation {
    data class Individual(val expression: String, val value: BigDecimal)

    data class Calculation(val value: BigDecimal, val individual: List<Individual>) {
        fun formattedValue(locale: Locale = Locale.getDefault()): String = format(value, locale)
    }

    data class ChecklistSummary(val total: BigDecimal, val checked: BigDecimal) {
        val pending: BigDecimal
            get() = total.subtract(checked)

        fun formattedTotal(locale: Locale = Locale.getDefault()): String = format(total, locale)

        fun formattedPending(locale: Locale = Locale.getDefault()): String = format(pending, locale)
    }

    fun calculate(text: String): Calculation? {
        val results = expressionBlocks(text).mapNotNull { block ->
            evaluate(block.expression)?.let { Individual(block.expression, it) }
        }
        return calculation(results)
    }

    /** Only the first arithmetic expression of each checklist task contributes to its total. */
    fun calculate(items: List<NotaChecklistItem>): Calculation? {
        val results = items.asSequence()
            .filter(NotaChecklistItem::isTask)
            .mapNotNull { item ->
                expressionBlocks(item.text, maximumCount = 1).firstOrNull()?.let { block ->
                    evaluate(block.expression)?.let { Individual(block.expression, it) }
                }
            }
            .toList()
        return calculation(results)
    }

    fun checklistSummary(items: List<NotaChecklistItem>): ChecklistSummary? {
        val total = calculate(items)?.value ?: return null
        val checked = calculate(items.filter { it.isTask && it.checked })?.value ?: BigDecimal.ZERO
        return ChecklistSummary(total = total, checked = checked)
    }

    fun annotateOperations(
        text: String,
        includeTrailingExpression: Boolean = false,
        maximumOperationCount: Int? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val replacements = mutableListOf<Replacement>()
        expressionBlocks(text, maximumOperationCount).forEach { block ->
            val annotation = annotationRange(text, block.expressionEnd)
            if (block.expression.startsWith('+') || block.expression.startsWith('-')) {
                if (annotation != null) replacements += Replacement(annotation, "")
            } else {
                val result = evaluate(block.expression)
                if (result != null &&
                    (includeTrailingExpression || block.expressionEnd < text.length || annotation != null)
                ) {
                    replacements += Replacement(
                        block.expressionEnd until (annotation?.last?.plus(1) ?: block.expressionEnd),
                        "->(${format(result, locale)})"
                    )
                }
            }
        }

        var annotated = text
        replacements.asReversed().forEach { replacement ->
            annotated = annotated.replaceRange(replacement.range, replacement.text)
        }
        return annotated
    }

    fun removeOperationAnnotations(text: String): String {
        val ranges = expressionBlocks(text).mapNotNull { block ->
            annotationRange(text, block.expressionEnd)?.takeIf { evaluate(block.expression) != null }
        }
        var clean = text
        ranges.asReversed().forEach { clean = clean.removeRange(it) }
        return clean
    }

    fun isValidExpression(expression: String): Boolean = evaluate(expression) != null

    fun formattedResult(
        expression: String,
        locale: Locale = Locale.getDefault()
    ): String? = evaluate(expression)?.let { format(it, locale) }

    private fun calculation(results: List<Individual>): Calculation? {
        if (results.isEmpty()) return null
        return Calculation(
            value = results.fold(BigDecimal.ZERO) { total, item -> total.add(item.value) },
            individual = results
        )
    }

    private data class ExpressionBlock(
        val expression: String,
        val expressionEnd: Int
    )

    private data class Replacement(val range: IntRange, val text: String)

    private fun expressionBlocks(text: String, maximumCount: Int? = null): List<ExpressionBlock> {
        val blocks = mutableListOf<ExpressionBlock>()
        var searchStart = 0
        while (searchStart < text.length && (maximumCount == null || blocks.size < maximumCount)) {
            val equalsIndex = text.indexOf('=', searchStart)
            if (equalsIndex < 0) break
            val block = arithmeticBlock(text, equalsIndex)
            blocks += block
            searchStart = block.expressionEnd.coerceAtLeast(equalsIndex + 1)
        }
        return blocks
    }

    private fun arithmeticBlock(text: String, equalsIndex: Int): ExpressionBlock {
        var index = equalsIndex + 1
        while (index < text.length && (text[index] == ' ' || text[index] == '\t')) index++
        val expressionStart = index
        var previousWasDigit = false
        var numberHasDecimalSeparator = false

        while (index < text.length) {
            val character = text[index]
            if (character.isAsciiDigit()) {
                previousWasDigit = true
                index++
                continue
            }
            if (character == '-' && index + 1 < text.length && text[index + 1] == '>') break
            if (character in OPERATORS) {
                previousWasDigit = false
                numberHasDecimalSeparator = false
                index++
                continue
            }
            if (character == '.' || character == ',') {
                val hasDigitAfter = index + 1 < text.length && text[index + 1].isAsciiDigit()
                val followsOperator = index > expressionStart && text[index - 1] in OPERATORS
                if (hasDigitAfter && !numberHasDecimalSeparator &&
                    (previousWasDigit || index == expressionStart || followsOperator)
                ) {
                    numberHasDecimalSeparator = true
                    previousWasDigit = false
                    index++
                    continue
                }
            }
            break
        }
        return ExpressionBlock(text.substring(expressionStart, index), index)
    }

    private fun annotationRange(text: String, expressionEnd: Int): IntRange? {
        if (!text.startsWith("->(", expressionEnd)) return null
        val closing = text.indexOf(')', expressionEnd + 3)
        return if (closing >= 0) expressionEnd..closing else null
    }

    private fun evaluate(expression: String): BigDecimal? {
        if (expression.isEmpty()) return null
        return ExpressionParser(expression).parse()
    }

    private class ExpressionParser(expression: String) {
        private val characters = expression.toCharArray()
        private var index = 0

        fun parse(): BigDecimal? {
            val result = parseExpression() ?: return null
            return result.takeIf { index == characters.size }
        }

        private fun parseExpression(): BigDecimal? {
            var result = parseTerm() ?: return null
            while (current == '+' || current == '-') {
                val operation = current
                index++
                val right = parseTerm() ?: return null
                result = if (operation == '+') result.add(right) else result.subtract(right)
            }
            return result
        }

        private fun parseTerm(): BigDecimal? {
            var result = parseSignedNumber() ?: return null
            while (current == '*' || current == '/') {
                val operation = current
                index++
                val right = parseSignedNumber() ?: return null
                result = if (operation == '*') {
                    result.multiply(right)
                } else {
                    if (right.compareTo(BigDecimal.ZERO) == 0) return null
                    runCatching { result.divide(right, DIVISION_SCALE, RoundingMode.HALF_UP) }.getOrNull()
                        ?: return null
                }
            }
            return result
        }

        private fun parseSignedNumber(): BigDecimal? {
            var negative = false
            if (current == '+' || current == '-') {
                negative = current == '-'
                index++
            }
            val value = parseNumber() ?: return null
            return if (negative) value.negate() else value
        }

        private fun parseNumber(): BigDecimal? {
            val integerStart = index
            while (current?.isAsciiDigit() == true) index++
            var normalized = String(characters, integerStart, index - integerStart)
            if (current == '.' || current == ',') {
                index++
                val fractionStart = index
                while (current?.isAsciiDigit() == true) index++
                if (fractionStart == index) return null
                if (normalized.isEmpty()) normalized = "0"
                normalized += "." + String(characters, fractionStart, index - fractionStart)
            }
            return normalized.takeIf(String::isNotEmpty)?.let { runCatching { it.toBigDecimal() }.getOrNull() }
        }

        private val current: Char?
            get() = characters.getOrNull(index)
    }

    private fun format(value: BigDecimal, locale: Locale): String {
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = if (value.stripTrailingZeros().scale() > 0) 2 else 0
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }
        return formatter.format(value)
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

    private val OPERATORS = setOf('+', '-', '*', '/')
    private const val DIVISION_SCALE = 16
}
