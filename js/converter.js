/**
 * R-7 Unit Converter Module
 * High-precision conversion across 13 engineering and scientific categories:
 * Length, Mass, Temperature, Area, Volume, Speed, Time, Data, Pressure, Energy, Power, Angles, Currencies.
 * Currency rates are stored locally in localStorage (zero network calls).
 */

export const CONVERSION_CATEGORIES = {
  temp: {
    name_ar: "درجة الحرارة",
    name_en: "Temperature",
    units: {
      C: { name_ar: "مئوية (°C)", name_en: "Celsius (°C)", symbol: "°C" },
      F: { name_ar: "فهرنهايت (°F)", name_en: "Fahrenheit (°F)", symbol: "°F" },
      K: { name_ar: "كلفن (K)", name_en: "Kelvin (K)", symbol: "K" }
    },
    convert: (val, from, to) => {
      if (from === to) return val;
      // Convert to Celsius first
      let c = val;
      if (from === "F") c = (val - 32) * (5 / 9);
      else if (from === "K") c = val - 273.15;

      // Convert Celsius to target
      if (to === "C") return c;
      if (to === "F") return c * (9 / 5) + 32;
      if (to === "K") return c + 273.15;
      return c;
    }
  },

  length: {
    name_ar: "طول",
    name_en: "Length",
    base: "m",
    units: {
      m: { name_ar: "متر (m)", name_en: "Meter (m)", factor: 1 },
      km: { name_ar: "كيلومتر (km)", name_en: "Kilometer (km)", factor: 1000 },
      cm: { name_ar: "سنتيمتر (cm)", name_en: "Centimeter (cm)", factor: 0.01 },
      mm: { name_ar: "ميليمتر (mm)", name_en: "Millimeter (mm)", factor: 0.001 },
      in: { name_ar: "بوصة (in)", name_en: "Inch (in)", factor: 0.0254 },
      ft: { name_ar: "قدم (ft)", name_en: "Foot (ft)", factor: 0.3048 },
      yd: { name_ar: "ياردة (yd)", name_en: "Yard (yd)", factor: 0.9144 },
      mi: { name_ar: "ميل (mi)", name_en: "Mile (mi)", factor: 1609.344 },
      nm: { name_ar: "نانومتر (nm)", name_en: "Nanometer (nm)", factor: 1e-9 },
      um: { name_ar: "ميكرومتر (µm)", name_en: "Micrometer (µm)", factor: 1e-6 }
    }
  },

  mass: {
    name_ar: "كتلة",
    name_en: "Mass",
    base: "kg",
    units: {
      kg: { name_ar: "كيلوغرام (kg)", name_en: "Kilogram (kg)", factor: 1 },
      g: { name_ar: "غرام (g)", name_en: "Gram (g)", factor: 0.001 },
      mg: { name_ar: "ميليغرام (mg)", name_en: "Milligram (mg)", factor: 1e-6 },
      lb: { name_ar: "رطل (lb)", name_en: "Pound (lb)", factor: 0.45359237 },
      oz: { name_ar: "أونصة (oz)", name_en: "Ounce (oz)", factor: 0.028349523125 },
      t: { name_ar: "طن متري (t)", name_en: "Metric Ton (t)", factor: 1000 },
      ct: { name_ar: "قيراط (ct)", name_en: "Carat (ct)", factor: 0.0002 }
    }
  },

  area: {
    name_ar: "مساحة",
    name_en: "Area",
    base: "m2",
    units: {
      m2: { name_ar: "متر مربع (m²)", name_en: "Square Meter (m²)", factor: 1 },
      km2: { name_ar: "كيلومتر مربع (km²)", name_en: "Square Km (km²)", factor: 1e6 },
      cm2: { name_ar: "سنتيمتر مربع (cm²)", name_en: "Square Cm (cm²)", factor: 1e-4 },
      ha: { name_ar: "هكتار (ha)", name_en: "Hectare (ha)", factor: 10000 },
      acre: { name_ar: "فدان / أكر (acre)", name_en: "Acre (acre)", factor: 4046.8564224 },
      ft2: { name_ar: "قدم مربع (ft²)", name_en: "Square Foot (ft²)", factor: 0.09290304 },
      in2: { name_ar: "بوصة مربعة (in²)", name_en: "Square Inch (in²)", factor: 0.00064516 }
    }
  },

  volume: {
    name_ar: "حجم",
    name_en: "Volume",
    base: "L",
    units: {
      L: { name_ar: "لتر (L)", name_en: "Liter (L)", factor: 1 },
      mL: { name_ar: "ميليلتر (mL)", name_en: "Milliliter (mL)", factor: 0.001 },
      m3: { name_ar: "متر مكعب (m³)", name_en: "Cubic Meter (m³)", factor: 1000 },
      gal: { name_ar: "غالون أمريكي (gal)", name_en: "Gallon (US)", factor: 3.785411784 },
      qt: { name_ar: "كوارت (qt)", name_en: "Quart (qt)", factor: 0.946352946 },
      pt: { name_ar: "باينت (pt)", name_en: "Pint (pt)", factor: 0.473176473 },
      cup: { name_ar: "كوب (cup)", name_en: "Cup (cup)", factor: 0.2365882365 },
      floz: { name_ar: "أونصة سائلة (fl oz)", name_en: "Fluid Ounce", factor: 0.0295735295625 }
    }
  },

  speed: {
    name_ar: "سرعة",
    name_en: "Speed",
    base: "mps",
    units: {
      mps: { name_ar: "متر/ثانية (m/s)", name_en: "Meter/sec (m/s)", factor: 1 },
      kmh: { name_ar: "كم/ساعة (km/h)", name_en: "Km/hour (km/h)", factor: 1 / 3.6 },
      mph: { name_ar: "ميل/ساعة (mph)", name_en: "Mile/hour (mph)", factor: 0.44704 },
      knot: { name_ar: "عقدة (knot)", name_en: "Knot", factor: 0.514444444 },
      fps: { name_ar: "قدم/ثانية (ft/s)", name_en: "Foot/sec (ft/s)", factor: 0.3048 }
    }
  },

  time: {
    name_ar: "زمن",
    name_en: "Time",
    base: "s",
    units: {
      s: { name_ar: "ثانية (s)", name_en: "Second (s)", factor: 1 },
      ms: { name_ar: "ميلي ثانية (ms)", name_en: "Millisecond (ms)", factor: 0.001 },
      min: { name_ar: "دقيقة (min)", name_en: "Minute (min)", factor: 60 },
      h: { name_ar: "ساعة (h)", name_en: "Hour (h)", factor: 3600 },
      d: { name_ar: "يوم (d)", name_en: "Day (d)", factor: 86400 },
      wk: { name_ar: "أسبوع (wk)", name_en: "Week (wk)", factor: 604800 },
      yr: { name_ar: "سنة (yr)", name_en: "Year (yr)", factor: 31536000 }
    }
  },

  data: {
    name_ar: "بيانات",
    name_en: "Data",
    base: "B",
    units: {
      B: { name_ar: "بايت (B)", name_en: "Byte (B)", factor: 1 },
      KB: { name_ar: "كيلوبايت (KB)", name_en: "Kilobyte (KB)", factor: 1024 },
      MB: { name_ar: "ميغابايت (MB)", name_en: "Megabyte (MB)", factor: 1024 ** 2 },
      GB: { name_ar: "غيغابايت (GB)", name_en: "Gigabyte (GB)", factor: 1024 ** 3 },
      TB: { name_ar: "تيرابايت (TB)", name_en: "Terabyte (TB)", factor: 1024 ** 4 },
      PB: { name_ar: "بيتابايت (PB)", name_en: "Petabyte (PB)", factor: 1024 ** 5 }
    }
  },

  pressure: {
    name_ar: "ضغط",
    name_en: "Pressure",
    base: "Pa",
    units: {
      Pa: { name_ar: "باسكال (Pa)", name_en: "Pascal (Pa)", factor: 1 },
      kPa: { name_ar: "كيلوباسكال (kPa)", name_en: "Kilopascal (kPa)", factor: 1000 },
      bar: { name_ar: "بار (bar)", name_en: "Bar", factor: 100000 },
      psi: { name_ar: "رطل/بوصة² (psi)", name_en: "PSI", factor: 6894.757293 },
      atm: { name_ar: "ضغط جوي (atm)", name_en: "Atmosphere (atm)", factor: 101325 },
      mmHg: { name_ar: "ملم زئبق (mmHg)", name_en: "mmHg / Torr", factor: 133.322387415 }
    }
  },

  energy: {
    name_ar: "طاقة",
    name_en: "Energy",
    base: "J",
    units: {
      J: { name_ar: "جول (J)", name_en: "Joule (J)", factor: 1 },
      kJ: { name_ar: "كيلوجول (kJ)", name_en: "Kilojoule (kJ)", factor: 1000 },
      cal: { name_ar: "سعرة (cal)", name_en: "Calorie (cal)", factor: 4.184 },
      kcal: { name_ar: "كيلوسعرة (kcal)", name_en: "Kilocalorie (kcal)", factor: 4184 },
      Wh: { name_ar: "واط ساعة (Wh)", name_en: "Watt-hour (Wh)", factor: 3600 },
      kWh: { name_ar: "كيلوواط ساعة (kWh)", name_en: "Kilowatt-hour", factor: 3.6e6 },
      BTU: { name_ar: "وحدة حرارية (BTU)", name_en: "BTU", factor: 1055.05585 }
    }
  },

  power: {
    name_ar: "قدرة",
    name_en: "Power",
    base: "W",
    units: {
      W: { name_ar: "واط (W)", name_en: "Watt (W)", factor: 1 },
      kW: { name_ar: "كيلوواط (kW)", name_en: "Kilowatt (kW)", factor: 1000 },
      MW: { name_ar: "ميغاواط (MW)", name_en: "Megawatt (MW)", factor: 1e6 },
      hp: { name_ar: "حصان ميكانيكي (hp)", name_en: "Horsepower (hp)", factor: 745.699872 }
    }
  },

  angle: {
    name_ar: "زوايا",
    name_en: "Angles",
    base: "deg",
    units: {
      deg: { name_ar: "درجة (°)", name_en: "Degree (°)", factor: 1 },
      rad: { name_ar: "راديان (rad)", name_en: "Radian (rad)", factor: 180 / Math.PI },
      grad: { name_ar: "غراد (grad)", name_en: "Gradian (grad)", factor: 0.9 }
    }
  },

  currency: {
    name_ar: "عملات",
    name_en: "Currencies",
    base: "USD",
    isCurrency: true
  }
};

// Default exchange rates (pegged / realistic reference, per 1 USD)
export const DEFAULT_CURRENCY_RATES = {
  USD: 1.0,
  EUR: 0.92,
  GBP: 0.79,
  SAR: 3.75,
  AED: 3.67,
  KWD: 0.31,
  EGP: 48.5,
  JPY: 155.0,
  CAD: 1.36,
  AUD: 1.52
};

export class UnitConverter {
  constructor(options = {}) {
    this.currencyRates = options.currencyRates || { ...DEFAULT_CURRENCY_RATES };
    this.ratesLastUpdated = options.ratesLastUpdated || "2026-08-15";
    this.precision = options.precision !== undefined ? options.precision : 1; // Default 1 decimal for 100°F -> 37.8°C
  }

  setRates(rates, updateDate = new Date().toISOString().split("T")[0]) {
    this.currencyRates = { ...this.currencyRates, ...rates };
    this.ratesLastUpdated = updateDate;
  }

  convert(categoryKey, value, fromUnit, toUnit, precision = this.precision) {
    if (isNaN(value)) return 0;
    const cat = CONVERSION_CATEGORIES[categoryKey];
    if (!cat) return value;

    if (categoryKey === "temp") {
      const res = cat.convert(value, fromUnit, toUnit);
      return this.roundResult(res, precision);
    }

    if (categoryKey === "currency") {
      const fromRate = this.currencyRates[fromUnit] || 1;
      const toRate = this.currencyRates[toUnit] || 1;
      // Value in USD = value / fromRate
      // Value in target = (value / fromRate) * toRate
      const res = (value / fromRate) * toRate;
      return this.roundResult(res, Math.max(precision, 2));
    }

    const fromFactor = cat.units[fromUnit]?.factor || 1;
    const toFactor = cat.units[toUnit]?.factor || 1;

    // Convert fromUnit -> base unit -> toUnit
    const inBase = value * fromFactor;
    const res = inBase / toFactor;
    return this.roundResult(res, precision);
  }

  roundResult(val, decimals) {
    if (!Number.isFinite(val)) return 0;
    const factor = Math.pow(10, decimals);
    const rounded = Math.round(val * factor) / factor;
    // Format to string
    return rounded;
  }
}
