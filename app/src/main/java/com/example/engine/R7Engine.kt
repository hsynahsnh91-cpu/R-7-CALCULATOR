package com.example.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

enum class AngleMode { DEG, RAD, GRAD }

data class EvalResult(
    val value: BigDecimal? = null,
    val error: String? = null,
    val autoClosedCount: Int = 0,
    val formattedResult: String = ""
)

object R7MathEngine {
    val MC = MathContext(34, RoundingMode.HALF_UP)
    val DISPLAY_MC = MathContext(12, RoundingMode.HALF_UP)

    val PI = BigDecimal("3.14159265358979323846264338327950288")
    val E = BigDecimal("2.71828182845904523536028747135266249")
    val PHI = BigDecimal("1.61803398874989484820458683436563811")

    fun evaluate(expression: String, angleMode: AngleMode = AngleMode.DEG, lastAns: BigDecimal = BigDecimal.ZERO): EvalResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return EvalResult(value = BigDecimal.ZERO, formattedResult = "0")
        }

        // Percentage semantics check for simple pattern: A (+|-|*|/) B%
        val pctMatch = Regex("""^(-?\d+(?:\.\d+)?)\s*([+\-×*÷/])\s*(\d+(?:\.\d+)?)%$""").find(trimmed)
        if (pctMatch != null) {
            val (aStr, op, bStr) = pctMatch.destructured
            val a = BigDecimal(aStr)
            val b = BigDecimal(bStr)
            val res = when (op) {
                "+", "add" -> a.add(a.multiply(b).divide(BigDecimal(100), MC))
                "-", "sub" -> a.subtract(a.multiply(b).divide(BigDecimal(100), MC))
                "×", "*", "mul" -> a.multiply(b).divide(BigDecimal(100), MC)
                "÷", "/", "div" -> {
                    if (b.compareTo(BigDecimal.ZERO) == 0) return EvalResult(error = "قسمة على صفر")
                    a.divide(b.divide(BigDecimal(100), MC), MC)
                }
                else -> null
            }
            if (res != null) {
                return EvalResult(value = res, formattedResult = formatDisplay(res))
            }
        }

        // Standalone percentage: e.g. 50% = 0.5
        val singlePctMatch = Regex("""^(-?\d+(?:\.\d+)?)%$""").find(trimmed)
        if (singlePctMatch != null) {
            val (numStr) = singlePctMatch.destructured
            val res = BigDecimal(numStr).divide(BigDecimal(100), MC)
            return EvalResult(value = res, formattedResult = formatDisplay(res))
        }

        // Auto-close open parentheses
        var openCount = 0
        var closeCount = 0
        for (ch in trimmed) {
            if (ch == '(') openCount++
            else if (ch == ')') closeCount++
        }
        val autoClosed = (openCount - closeCount).coerceAtLeast(0)
        val fullExpr = trimmed + ")".repeat(autoClosed)

        return try {
            val tokens = tokenize(fullExpr)
            val parser = ExprParser(tokens, angleMode, lastAns)
            val value = parser.parse()
            EvalResult(
                value = value,
                autoClosedCount = autoClosed,
                formattedResult = formatDisplay(value)
            )
        } catch (e: ArithmeticException) {
            EvalResult(error = "قسمة على صفر")
        } catch (e: IllegalArgumentException) {
            EvalResult(error = e.message ?: "التعبير غير مكتمل")
        } catch (e: Exception) {
            EvalResult(error = "التعبير غير مكتمل")
        }
    }

    private fun formatDisplay(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val plain = stripped.toPlainString()

        // 1 ÷ 3 × 3 check: if extremely close to an integer within 1e-11, round to integer
        val rounded = stripped.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        if (rounded.scale() <= 0 && (stripped.subtract(rounded).abs() < BigDecimal("0.00000000001"))) {
            return rounded.toPlainString()
        }

        val absVal = value.abs()
        val isZero = absVal.compareTo(BigDecimal.ZERO) == 0
        val isHuge = absVal >= BigDecimal("1000000000000") // >= 10^12
        val isTiny = !isZero && absVal < BigDecimal("0.0000001") // < 10^-7

        if (isHuge || isTiny) {
            val sci = String.format(java.util.Locale.US, "%.10e", value.toDouble())
            val parts = sci.split("e")
            val mantissa = BigDecimal(parts[0]).stripTrailingZeros().toPlainString()
            val exp = parts[1].toInt()
            val supExp = toSuperscript(exp)
            return "$mantissa×10$supExp"
        }

        if (plain.length > 13) {
            val rounded12 = BigDecimal(value.toString(), DISPLAY_MC).stripTrailingZeros()
            return rounded12.toPlainString()
        }

        return plain
    }

    private fun toSuperscript(exp: Int): String {
        val s = exp.toString()
        val map = mapOf(
            '-' to "⁻", '+' to "⁺", '0' to "⁰", '1' to "¹", '2' to "²",
            '3' to "³", '4' to "⁴", '5' to "⁵", '6' to "⁶", '7' to "⁷",
            '8' to "⁸", '9' to "⁹"
        )
        return s.map { map[it] ?: it.toString() }.joinToString("")
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val clean = expr.replace(" ", "")

        while (i < clean.length) {
            val c = clean[i]

            // Numbers
            if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < clean.length && (clean[i].isDigit() || clean[i] == '.')) {
                    sb.append(clean[i])
                    i++
                }
                tokens.add(sb.toString())
                continue
            }

            // Word operators / functions / constants
            if (c.isLetter()) {
                val sb = StringBuilder()
                while (i < clean.length && clean[i].isLetter()) {
                    sb.append(clean[i])
                    i++
                }
                tokens.add(sb.toString())
                continue
            }

            // Operators or symbols
            if (c in "+-×*÷/^%()!|πeφ") {
                tokens.add(c.toString())
                i++
                continue
            }

            i++
        }
        return tokens
    }

    private class ExprParser(
        private val tokens: List<String>,
        private val angleMode: AngleMode,
        private val lastAns: BigDecimal
    ) {
        private var idx = 0

        fun parse(): BigDecimal {
            val res = parseAddSub()
            return res
        }

        private fun peek(): String? = if (idx < tokens.size) tokens[idx] else null
        private fun next(): String = tokens[idx++]

        fun parseFactor(): BigDecimal {
            val tok = peek() ?: throw IllegalArgumentException("التعبير غير مكتمل")

            // Unary minus or plus
            if (tok == "-" || tok == "+") {
                next()
                val f = parseFactor()
                return if (tok == "-") f.negate() else f
            }

            // Parentheses
            if (tok == "(") {
                next()
                val expr = parseAddSub()
                if (peek() == ")") next()
                return expr
            }

            // Absolute value |x|
            if (tok == "|") {
                next()
                val expr = parseAddSub()
                if (peek() == "|") next()
                return expr.abs()
            }

            // Constants
            if (tok == "π" || tok == "pi") { next(); return PI }
            if (tok == "e") { next(); return E }
            if (tok == "φ" || tok == "phi") { next(); return PHI }
            if (tok == "ANS" || tok == "ans") { next(); return lastAns }

            // Functions: sin, cos, tan, asin, acos, atan, ln, log, sqrt
            if (tok in listOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "√")) {
                val fn = next()
                val arg = parseFactor()
                return applyFunction(fn, arg, angleMode)
            }

            // Number
            next()
            return BigDecimal(tok)
        }

        fun parsePostfix(): BigDecimal {
            var node = parseFactor()
            while (peek() == "!" || peek() == "%") {
                val op = next()
                node = if (op == "!") {
                    factorial(node)
                } else {
                    node.divide(BigDecimal(100), MC)
                }
            }
            return node
        }

        fun parsePower(): BigDecimal {
            var left = parsePostfix()
            if (peek() == "^") {
                next()
                val right = parsePower() // right-associative
                val dLeft = left.toDouble()
                val dRight = right.toDouble()
                val res = dLeft.pow(dRight)
                if (res.isInfinite() || res.isNaN()) throw IllegalArgumentException("خارج المدى المسموح")
                left = BigDecimal(res.toString(), MC)
            }
            return left
        }

        fun parseMulDiv(): BigDecimal {
            var left = parsePower()
            while (peek() in listOf("×", "*", "÷", "/")) {
                val op = next()
                val right = parsePower()
                left = when (op) {
                    "×", "*" -> left.multiply(right, MC)
                    "÷", "/" -> {
                        if (right.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                        left.divide(right, MC)
                    }
                    else -> left
                }
            }
            return left
        }

        fun parseAddSub(): BigDecimal {
            var left = parseMulDiv()
            while (peek() in listOf("+", "-")) {
                val op = next()
                val right = parseMulDiv()
                left = when (op) {
                    "+" -> left.add(right, MC)
                    "-" -> left.subtract(right, MC)
                    else -> left
                }
            }
            return left
        }

        private fun factorial(n: BigDecimal): BigDecimal {
            if (n < BigDecimal.ZERO || n.stripTrailingZeros().scale() > 0) {
                throw IllegalArgumentException("خارج المدى المسموح")
            }
            val intVal = n.toInt()
            if (intVal > 170) throw IllegalArgumentException("خارج المدى المسموح")
            var result = BigDecimal.ONE
            for (i in 2..intVal) {
                result = result.multiply(BigDecimal(i))
            }
            return result
        }

        private fun applyFunction(fn: String, arg: BigDecimal, angleMode: AngleMode): BigDecimal {
            val d = arg.toDouble()
            val rad = when (angleMode) {
                AngleMode.DEG -> Math.toRadians(d)
                AngleMode.RAD -> d
                AngleMode.GRAD -> d * Math.PI / 200.0
            }

            val res = when (fn) {
                "sin" -> {
                    if (angleMode == AngleMode.DEG) {
                        val deg = ((d % 360) + 360) % 360
                        when {
                            deg == 0.0 || deg == 180.0 || deg == 360.0 -> 0.0
                            deg == 30.0 || deg == 150.0 -> 0.5
                            deg == 90.0 -> 1.0
                            deg == 210.0 || deg == 330.0 -> -0.5
                            deg == 270.0 -> -1.0
                            else -> sin(rad)
                        }
                    } else sin(rad)
                }
                "cos" -> {
                    if (angleMode == AngleMode.DEG) {
                        val deg = ((d % 360) + 360) % 360
                        when {
                            deg == 90.0 || deg == 270.0 -> 0.0
                            deg == 60.0 || deg == 300.0 -> 0.5
                            deg == 0.0 || deg == 360.0 -> 1.0
                            deg == 120.0 || deg == 240.0 -> -0.5
                            deg == 180.0 -> -1.0
                            else -> cos(rad)
                        }
                    } else cos(rad)
                }
                "tan" -> {
                    if (angleMode == AngleMode.DEG) {
                        val deg = ((d % 360) + 360) % 360
                        when {
                            deg == 45.0 || deg == 225.0 -> 1.0
                            deg == 135.0 || deg == 315.0 -> -1.0
                            deg == 0.0 || deg == 180.0 -> 0.0
                            deg == 90.0 || deg == 270.0 -> throw ArithmeticException("Division by zero")
                            else -> tan(rad)
                        }
                    } else tan(rad)
                }
                "asin" -> {
                    if (d < -1.0 || d > 1.0) throw IllegalArgumentException("خارج المدى المسموح")
                    val r = asin(d)
                    when (angleMode) {
                        AngleMode.DEG -> Math.toDegrees(r)
                        AngleMode.RAD -> r
                        AngleMode.GRAD -> r * 200.0 / Math.PI
                    }
                }
                "acos" -> {
                    if (d < -1.0 || d > 1.0) throw IllegalArgumentException("خارج المدى المسموح")
                    val r = acos(d)
                    when (angleMode) {
                        AngleMode.DEG -> Math.toDegrees(r)
                        AngleMode.RAD -> r
                        AngleMode.GRAD -> r * 200.0 / Math.PI
                    }
                }
                "atan" -> {
                    val r = atan(d)
                    when (angleMode) {
                        AngleMode.DEG -> Math.toDegrees(r)
                        AngleMode.RAD -> r
                        AngleMode.GRAD -> r * 200.0 / Math.PI
                    }
                }
                "ln" -> {
                    if (d <= 0) throw IllegalArgumentException("خارج المدى المسموح")
                    ln(d)
                }
                "log" -> {
                    if (d <= 0) throw IllegalArgumentException("خارج المدى المسموح")
                    log10(d)
                }
                "sqrt", "√" -> {
                    if (d < 0) throw IllegalArgumentException("خارج المدى المسموح")
                    sqrt(d)
                }
                else -> throw IllegalArgumentException("دالة غير مدعومة")
            }

            return BigDecimal(res.toString(), MC)
        }
    }
}
