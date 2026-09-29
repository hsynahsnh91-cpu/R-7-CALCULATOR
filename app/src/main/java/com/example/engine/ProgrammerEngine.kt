package com.example.engine

import java.math.BigInteger

enum class NumBase(val radix: Int, val label: String) {
    HEX(16, "HEX"),
    DEC(10, "DEC"),
    OCT(8, "OCT"),
    BIN(2, "BIN")
}

data class BaseValues(
    val hex: String,
    val dec: String,
    val oct: String,
    val bin: String
)

class ProgrammerEngine {
    private val MASK64 = BigInteger("FFFFFFFFFFFFFFFF", 16)
    var currentValue: BigInteger = BigInteger.ZERO
        private set

    fun setValue(value: BigInteger) {
        currentValue = value.and(MASK64)
    }

    fun parseInput(input: String, base: NumBase): BigInteger {
        val clean = input.replace(" ", "").trim()
        if (clean.isEmpty()) return BigInteger.ZERO
        return try {
            BigInteger(clean, base.radix).and(MASK64)
        } catch (_: Exception) {
            BigInteger.ZERO
        }
    }

    fun getBaseRepresentations(val64: BigInteger = currentValue): BaseValues {
        val uVal = val64.and(MASK64)
        val hex = uVal.toString(16).uppercase()
        val dec = uVal.toString(10)
        val oct = uVal.toString(8)
        val rawBin = uVal.toString(2)
        val bin = formatNibbles(rawBin)

        return BaseValues(hex = hex, dec = dec, oct = oct, bin = bin)
    }

    fun formatNibbles(binStr: String): String {
        val padLen = (4 - (binStr.length % 4)) % 4
        val padded = "0".repeat(padLen) + binStr
        val chunks = mutableListOf<String>()
        for (i in padded.indices step 4) {
            chunks.add(padded.substring(i, minOf(i + 4, padded.length)))
        }
        return chunks.joinToString(" ")
    }

    fun executeOp(a: BigInteger, op: String, b: BigInteger): BigInteger {
        val uA = a.and(MASK64)
        val uB = b.and(MASK64)
        val res = when (op) {
            "AND" -> uA.and(uB)
            "OR" -> uA.or(uB)
            "XOR" -> uA.xor(uB)
            "SHL" -> {
                val shift = uB.toInt().coerceIn(0, 63)
                uA.shiftLeft(shift).and(MASK64)
            }
            "SHR" -> {
                val shift = uB.toInt().coerceIn(0, 63)
                uA.shiftRight(shift).and(MASK64)
            }
            "+" -> uA.add(uB).and(MASK64)
            "-" -> uA.subtract(uB).and(MASK64)
            "×", "*" -> uA.multiply(uB).and(MASK64)
            "÷", "/" -> {
                if (uB == BigInteger.ZERO) throw ArithmeticException("Division by zero")
                uA.divide(uB).and(MASK64)
            }
            else -> uA
        }
        return res
    }

    fun not(a: BigInteger): BigInteger {
        return a.and(MASK64).xor(MASK64)
    }
}
