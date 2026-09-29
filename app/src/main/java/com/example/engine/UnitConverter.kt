package com.example.engine

import java.math.BigDecimal
import java.math.RoundingMode

data class UnitItem(val id: String, val nameAr: String, val nameEn: String, val factor: Double)
data class CategoryItem(val id: String, val nameAr: String, val nameEn: String, val units: List<UnitItem>)

class UnitConverter(private val currencyRates: Map<String, Double> = defaultCurrencyRates) {

    companion object {
        val defaultCurrencyRates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.78,
            "SAR" to 3.75,
            "AED" to 3.67,
            "KWD" to 0.31,
            "QAR" to 3.64,
            "EGP" to 48.5,
            "JOD" to 0.709,
            "TRY" to 33.2
        )

        val categories: List<CategoryItem> = listOf(
            CategoryItem("length", "طول", "Length", listOf(
                UnitItem("m", "متر", "Meter", 1.0),
                UnitItem("km", "كيلومتر", "Kilometer", 1000.0),
                UnitItem("cm", "سنتيمتر", "Centimeter", 0.01),
                UnitItem("mm", "ميليمتر", "Millimeter", 0.001),
                UnitItem("mi", "ميل", "Mile", 1609.344),
                UnitItem("yd", "ياردة", "Yard", 0.9144),
                UnitItem("ft", "قدم", "Foot", 0.3048),
                UnitItem("in", "بوصة", "Inch", 0.0254)
            )),
            CategoryItem("mass", "كتلة", "Mass", listOf(
                UnitItem("kg", "كيلوغرام", "Kilogram", 1.0),
                UnitItem("g", "غرام", "Gram", 0.001),
                UnitItem("mg", "ميليغرام", "Milligram", 0.000001),
                UnitItem("t", "طن", "Tonne", 1000.0),
                UnitItem("lb", "رطل", "Pound", 0.45359237),
                UnitItem("oz", "أونصة", "Ounce", 0.028349523125)
            )),
            CategoryItem("temperature", "حرارة", "Temperature", listOf(
                UnitItem("C", "مئوية (°C)", "Celsius", 1.0),
                UnitItem("F", "فهرنهايت (°F)", "Fahrenheit", 1.0),
                UnitItem("K", "كلفن (K)", "Kelvin", 1.0)
            )),
            CategoryItem("area", "مساحة", "Area", listOf(
                UnitItem("m2", "متر مربع", "Square Meter", 1.0),
                UnitItem("km2", "كيلومتر مربع", "Square Kilometer", 1000000.0),
                UnitItem("ha", "هكتار", "Hectare", 10000.0),
                UnitItem("acre", "فدان/إيكر", "Acre", 4046.8564224),
                UnitItem("ft2", "قدم مربع", "Square Foot", 0.09290304)
            )),
            CategoryItem("volume", "حجم", "Volume", listOf(
                UnitItem("l", "لتر", "Liter", 1.0),
                UnitItem("ml", "ميليلتر", "Milliliter", 0.001),
                UnitItem("m3", "متر مكعب", "Cubic Meter", 1000.0),
                UnitItem("gal", "غالون (أمريكي)", "Gallon (US)", 3.785411784),
                UnitItem("cup", "كوب", "Cup", 0.2365882365)
            )),
            CategoryItem("speed", "سرعة", "Speed", listOf(
                UnitItem("kmh", "كم/ساعة", "km/h", 1.0),
                UnitItem("ms", "م/ثانية", "m/s", 3.6),
                UnitItem("mph", "ميل/ساعة", "mph", 1.609344),
                UnitItem("knot", "عقدة", "Knot", 1.852)
            )),
            CategoryItem("time", "زمن", "Time", listOf(
                UnitItem("s", "ثانية", "Second", 1.0),
                UnitItem("min", "دقيقة", "Minute", 60.0),
                UnitItem("h", "ساعة", "Hour", 3600.0),
                UnitItem("d", "يوم", "Day", 86400.0),
                UnitItem("w", "أسبوع", "Week", 604800.0)
            )),
            CategoryItem("data", "بيانات", "Data", listOf(
                UnitItem("B", "بايت", "Byte", 1.0),
                UnitItem("KB", "كيلوبايت", "Kilobyte", 1024.0),
                UnitItem("MB", "ميغابايت", "Megabyte", 1048576.0),
                UnitItem("GB", "غيغابايت", "Gigabyte", 1073741824.0),
                UnitItem("TB", "تيرابايت", "Terabyte", 1099511627776.0)
            )),
            CategoryItem("pressure", "ضغط", "Pressure", listOf(
                UnitItem("Pa", "باسكال", "Pascal", 1.0),
                UnitItem("bar", "بار", "Bar", 100000.0),
                UnitItem("psi", "رطل/بوصة²", "psi", 6894.757293),
                UnitItem("atm", "ضغط جوي", "Atmosphere", 101325.0)
            )),
            CategoryItem("energy", "طاقة", "Energy", listOf(
                UnitItem("J", "جول", "Joule", 1.0),
                UnitItem("kJ", "كيلوجول", "Kilojoule", 1000.0),
                UnitItem("cal", "سعرة حرارية", "Calorie", 4.184),
                UnitItem("kcal", "كيلوسعرة", "Kilocalorie", 4184.0),
                UnitItem("kWh", "كيلوواط.ساعة", "kWh", 3600000.0)
            )),
            CategoryItem("power", "قدرة", "Power", listOf(
                UnitItem("W", "واط", "Watt", 1.0),
                UnitItem("kW", "كيلوواط", "Kilowatt", 1000.0),
                UnitItem("hp", "حصان ميكانيكي", "Horsepower", 745.699872)
            )),
            CategoryItem("angle", "زوايا", "Angle", listOf(
                UnitItem("deg", "درجة (°)", "Degree", 1.0),
                UnitItem("rad", "راديان (rad)", "Radian", 57.295779513),
                UnitItem("grad", "غراد (grad)", "Gradian", 0.9)
            )),
            CategoryItem("currency", "عملات (محلية)", "Currency (Local)", listOf(
                UnitItem("USD", "دولار أمريكي (USD)", "US Dollar", 1.0),
                UnitItem("EUR", "يورو (EUR)", "Euro", 1.0),
                UnitItem("GBP", "جنيه إسترليني (GBP)", "British Pound", 1.0),
                UnitItem("SAR", "ريال سعودي (SAR)", "Saudi Riyal", 1.0),
                UnitItem("AED", "درهم إماراتي (AED)", "UAE Dirham", 1.0),
                UnitItem("KWD", "دينار كويتي (KWD)", "Kuwaiti Dinar", 1.0),
                UnitItem("QAR", "ريال قطري (QAR)", "Qatari Riyal", 1.0),
                UnitItem("EGP", "جنيه مصري (EGP)", "Egyptian Pound", 1.0),
                UnitItem("JOD", "دينار أردني (JOD)", "Jordanian Dinar", 1.0),
                UnitItem("TRY", "ليرة تركية (TRY)", "Turkish Lira", 1.0)
            ))
        )
    }

    fun convert(value: Double, categoryId: String, fromUnitId: String, toUnitId: String, decimals: Int = 1): Double {
        if (categoryId == "temperature") {
            val celsius = when (fromUnitId) {
                "C" -> value
                "F" -> (value - 32.0) * (5.0 / 9.0)
                "K" -> value - 273.15
                else -> value
            }
            val target = when (toUnitId) {
                "C" -> celsius
                "F" -> (celsius * (9.0 / 5.0)) + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
            return BigDecimal(target.toString()).setScale(decimals, RoundingMode.HALF_UP).toDouble()
        }

        if (categoryId == "currency") {
            val fromRate = currencyRates[fromUnitId] ?: defaultCurrencyRates[fromUnitId] ?: 1.0
            val toRate = currencyRates[toUnitId] ?: defaultCurrencyRates[toUnitId] ?: 1.0
            val usd = value / fromRate
            val result = usd * toRate
            return BigDecimal(result.toString()).setScale(decimals, RoundingMode.HALF_UP).toDouble()
        }

        val cat = categories.find { it.id == categoryId } ?: return value
        val fromUnit = cat.units.find { it.id == fromUnitId } ?: return value
        val toUnit = cat.units.find { it.id == toUnitId } ?: return value

        val baseValue = value * fromUnit.factor
        val targetValue = baseValue / toUnit.factor
        return BigDecimal(targetValue.toString()).setScale(decimals, RoundingMode.HALF_UP).toDouble()
    }
}
