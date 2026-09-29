package com.example.gemini

import com.example.engine.R7MathEngine
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object R7LocalReasoningEngine {
    private val MC = MathContext(10, RoundingMode.HALF_UP)

    fun trySolve(query: String, language: DetectedLanguage): String? {
        val trimmed = query.trim()
        val lower = trimmed.lowercase()
        val isArabic = (language != DetectedLanguage.ENGLISH)

        // 1. Parallel Resistors Calculation
        if (lower.contains("توازي") || lower.contains("parallel")) {
            val numbers = Regex("""\b\d+(?:\.\d+)?\b""").findAll(trimmed).mapNotNull {
                try { BigDecimal(it.value) } catch (_: Exception) { null }
            }.filter { it > BigDecimal.ZERO }.toList()

            if (numbers.size >= 2) {
                val r1 = numbers[0]
                val r2 = numbers[1]
                val product = r1.multiply(r2)
                val sum = r1.add(r2)
                val req = product.divide(sum, MC).stripTrailingZeros()
                val reqFormatted = String.format("%.2f", req.toDouble()).trimEnd('0').trimEnd('.')

                return if (isArabic) {
                    """
                    حساب المقاومة المكافئة على التوازي:
                    القانون: Req = (R1 × R2) / (R1 + R2)
                    الخطوات:
                    Req = ($r1 × $r2) / ($r1 + $r2)
                    Req = $product / $sum
                    النتيجة النهائية = $reqFormatted Ω (أوم).
                    """.trimIndent()
                } else {
                    """
                    Parallel Resistance Calculation:
                    Formula: Req = (R1 × R2) / (R1 + R2)
                    Steps:
                    Req = ($r1 × $r2) / ($r1 + $r2)
                    Req = $product / $sum
                    Final Result = $reqFormatted Ω.
                    """.trimIndent()
                }
            }
        }

        // 2. Series Resistors Calculation
        if (lower.contains("توالي") || lower.contains("series")) {
            val numbers = Regex("""\b\d+(?:\.\d+)?\b""").findAll(trimmed).mapNotNull {
                try { BigDecimal(it.value) } catch (_: Exception) { null }
            }.filter { it > BigDecimal.ZERO }.toList()

            if (numbers.size >= 2) {
                val sum = numbers.reduce { acc, b -> acc.add(b) }
                val expr = numbers.joinToString(" + ")
                return if (isArabic) {
                    """
                    حساب المقاومة المكافئة على التوالي:
                    القانون: Req = R1 + R2 + ...
                    Req = $expr
                    النتيجة النهائية = $sum Ω (أوم).
                    """.trimIndent()
                } else {
                    """
                    Series Resistance Calculation:
                    Formula: Req = R1 + R2 + ...
                    Req = $expr
                    Final Result = $sum Ω.
                    """.trimIndent()
                }
            }
        }

        // 3. Polynomial Derivative: e.g. f(x) = x^3 - 5x + 2 or derivative of x^2 + 3x
        if (lower.contains("مشتقة") || lower.contains("derivative") || lower.contains("تفاضل")) {
            val polyMatch = Regex("""(?:f\(x\)\s*=\s*)?([xX0-9\^+\-\*\s]+)""").find(trimmed)
            if (polyMatch != null) {
                val expr = polyMatch.groupValues[1].replace(" ", "")
                val derived = deriveSimplePolynomial(expr)
                if (derived != null) {
                    return if (isArabic) {
                        """
                        حساب التفاضل (المشتقة الأولى):
                        الدالة: f(x) = $expr
                        قاعدة القوة: d/dx(x^n) = n·x^(n-1)
                        المشتقة: f'(x) = $derived
                        """.trimIndent()
                    } else {
                        """
                        Derivative Calculation:
                        Function: f(x) = $expr
                        Power Rule: d/dx(x^n) = n·x^(n-1)
                        Derivative: f'(x) = $derived
                        """.trimIndent()
                    }
                }
            }
        }

        // 4. Percentage Calculation: e.g. "250 + 15%" or "النسبة المئوية لـ 250 + 15%"
        val pctMatch = Regex("""(\d+(?:\.\d+)?)\s*([+\-×*÷/])\s*(\d+(?:\.\d+)?)%""").find(trimmed)
        if (pctMatch != null) {
            val (baseStr, op, rateStr) = pctMatch.destructured
            val base = BigDecimal(baseStr)
            val rate = BigDecimal(rateStr)
            val pctValue = base.multiply(rate).divide(BigDecimal(100), MC)

            val total = when (op) {
                "+" -> base.add(pctValue)
                "-" -> base.subtract(pctValue)
                "*", "×" -> base.multiply(rate).divide(BigDecimal(100), MC)
                else -> null
            }

            if (total != null) {
                val totalFmt = String.format("%.2f", total.toDouble()).trimEnd('0').trimEnd('.')
                val pctFmt = String.format("%.2f", pctValue.toDouble()).trimEnd('0').trimEnd('.')
                return if (isArabic) {
                    """
                    حساب النسبة المئوية:
                    القيمة الأساسية: $base
                    قيمة النسبة ($rate%): $base × $rate% = $pctFmt
                    الناتج الإجمالي ($base $op $pctFmt) = $totalFmt
                    """.trimIndent()
                } else {
                    """
                    Percentage Calculation:
                    Base value: $base
                    Percentage value ($rate%): $base × $rate% = $pctFmt
                    Final Total ($base $op $pctFmt) = $totalFmt
                    """.trimIndent()
                }
            }
        }

        // 5. Temperature Conversion: e.g. "100 فهرنهايت إلى مئوية" or "100 F to C"
        if (lower.contains("فهرنهايت") || lower.contains("fahrenheit") || lower.contains("مئوية") || lower.contains("celsius")) {
            val numMatch = Regex("""\b(\d+(?:\.\d+)?)\b""").find(trimmed)
            if (numMatch != null) {
                val tempVal = numMatch.value.toDoubleOrNull()
                if (tempVal != null) {
                    val toCelsius = lower.contains("مئوية") || lower.contains("celsius") || lower.contains("إلى c")
                    if (toCelsius) {
                        val c = (tempVal - 32.0) * 5.0 / 9.0
                        val cFmt = String.format("%.2f", c)
                        return if (isArabic) {
                            """
                            تحويل درجة الحرارة من فهرنهايت إلى مئوية:
                            القانون: C = (F - 32) × 5/9
                            C = ($tempVal - 32) × 5/9 = $cFmt °C
                            النتيجة = $cFmt درجة مئوية.
                            """.trimIndent()
                        } else {
                            """
                            Temperature Conversion (Fahrenheit to Celsius):
                            Formula: C = (F - 32) × 5/9
                            C = ($tempVal - 32) × 5/9 = $cFmt °C
                            Result = $cFmt °C.
                            """.trimIndent()
                        }
                    } else {
                        val f = (tempVal * 9.0 / 5.0) + 32.0
                        val fFmt = String.format("%.2f", f)
                        return if (isArabic) {
                            """
                            تحويل درجة الحرارة من مئوية إلى فهرنهايت:
                            القانون: F = (C × 9/5) + 32
                            F = ($tempVal × 9/5) + 32 = $fFmt °F
                            النتيجة = $fFmt فهرنهايت.
                            """.trimIndent()
                        } else {
                            """
                            Temperature Conversion (Celsius to Fahrenheit):
                            Formula: F = (C × 9/5) + 32
                            F = ($tempVal × 9/5) + 32 = $fFmt °F
                            Result = $fFmt °F.
                            """.trimIndent()
                        }
                    }
                }
            }
        }

        // 6. Direct arithmetic evaluation via R7MathEngine (e.g. "25 × 25", "100 / 4", "sqrt(144)")
        val cleanMath = trimmed
            .replace("احسب", "")
            .replace("calculate", "")
            .replace("what is", "")
            .replace("ما هو", "")
            .replace("كم يساوي", "")
            .replace("=", "")
            .trim()

        if (cleanMath.isNotBlank() && cleanMath.any { it.isDigit() }) {
            try {
                val eval = R7MathEngine.evaluate(cleanMath)
                if (eval.error == null && eval.formattedResult.isNotBlank() && eval.formattedResult != "0") {
                    return if (isArabic) {
                        "الناتج الحسابي الدقيق:\n$cleanMath = ${eval.formattedResult}"
                    } else {
                        "Exact Calculation Result:\n$cleanMath = ${eval.formattedResult}"
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    private fun deriveSimplePolynomial(expr: String): String? {
        // Simple term parser for expressions like x^3-5x+2 or 2x^2+4x-1
        try {
            val terms = mutableListOf<String>()
            val regex = Regex("""([+-]?\s*\d*(?:\.\d+)?)\s*([xX])(?:\^(\d+))?|([+-]?\s*\d+(?:\.\d+)?)""")
            val matches = regex.findAll(expr).toList()
            if (matches.isEmpty()) return null

            for (m in matches) {
                val full = m.value.replace(" ", "")
                if (full.isEmpty()) continue

                if (full.contains("x") || full.contains("X")) {
                    val coefStr = m.groupValues[1].replace(" ", "")
                    val coef = when {
                        coefStr.isEmpty() || coefStr == "+" -> 1.0
                        coefStr == "-" -> -1.0
                        else -> coefStr.toDoubleOrNull() ?: 1.0
                    }
                    val powerStr = m.groupValues[3]
                    val power = if (powerStr.isNotEmpty()) powerStr.toIntOrNull() ?: 1 else 1

                    val newCoef = coef * power
                    val newPower = power - 1

                    val termFormatted = when {
                        newPower == 0 -> {
                            val c = if (newCoef % 1.0 == 0.0) newCoef.toInt().toString() else newCoef.toString()
                            if (newCoef > 0 && terms.isNotEmpty()) "+$c" else c
                        }
                        newPower == 1 -> {
                            val c = if (newCoef == 1.0) "" else if (newCoef == -1.0) "-" else if (newCoef % 1.0 == 0.0) newCoef.toInt().toString() else newCoef.toString()
                            val prefix = if (newCoef > 0 && terms.isNotEmpty()) "+" else ""
                            "$prefix${c}x"
                        }
                        else -> {
                            val c = if (newCoef == 1.0) "" else if (newCoef == -1.0) "-" else if (newCoef % 1.0 == 0.0) newCoef.toInt().toString() else newCoef.toString()
                            val prefix = if (newCoef > 0 && terms.isNotEmpty()) "+" else ""
                            "$prefix${c}x^$newPower"
                        }
                    }
                    terms.add(termFormatted)
                }
                // Constant terms derive to 0 (omitted)
            }

            return if (terms.isNotEmpty()) terms.joinToString(" ") else "0"
        } catch (_: Exception) {
            return null
        }
    }
}
