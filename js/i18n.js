/**
 * R-7 Calculator - Internationalization (i18n)
 * Default: Arabic (RTL UI with LTR LCD display)
 * Secondary: English
 */

export const translations = {
  ar: {
    app_title: "R-7",
    app_subtitle: "آلة حاسبة دقيقة",
    standard: "قياسي",
    scientific: "علمي",
    programmer: "مبرمج",
    converter: "محوّل",
    history: "السجل",
    settings: "الإعدادات",
    about: "حول R-7",
    empty_history: "لا عمليات بعد — نتيجتك الأولى ستظهر هنا.",
    first_launch_hint: "جرّب لوحة المفاتيح — كل شيء يعمل بلا فأرة.",
    result_copied: "نُسخت النتيجة",
    expr_copied: "تم نسخ التعبير",
    item_deleted: "تم حذف العملية",
    history_cleared: "تم مسح السجل",
    export_filename: "r7-history.txt",
    export_header: "سجل عمليات R-7",
    search_history: "بحث في السجل...",
    clear_history_btn: "مسح السجل",
    export_history_btn: "تصدير TXT",
    close: "إغلاق",
    copy: "نسخ",
    insert_expr: "إدراج التعبير",
    delete: "حذف",
    
    // Status & Engine Errors (Exact strings as required)
    err_div_zero: "قسمة على صفر",
    err_incomplete: "التعبير غير مكتمل",
    err_out_of_range: "خارج المدى المسموح",
    err_invalid_paste: "نص غير صالح للحساب",

    // Settings
    theme_label: "المظهر",
    theme_dark: "داكن (فوسفوري)",
    theme_light: "فاتح (عاجي)",
    language_label: "اللغة",
    angle_mode_label: "وحدة الزوايا الافتراضية",
    sound_label: "صوت النقرات (WebAudio)",
    sound_enabled: "مفعل",
    sound_disabled: "معطل",
    vibration_label: "الاهتزاز اللمسي",
    vibration_enabled: "مفعل",
    vibration_disabled: "معطل",
    decimals_label: "خانات التقريب في المحوّل",
    
    // About modal
    version_label: "الإصدار",
    license_label: "الترخيص: MIT License — أداة هندسية حرة ومفتوحة",
    changelog_title: "سجل التغييرات",
    changelog: [
      {
        version: "1.2.0",
        date: "2026-08-15",
        notes: "إضافة وضع المبرمج (HEX/DEC/OCT/BIN مع عمليات البت وعرض 64 بت المتزامن)، محوّل الوحدات الشامل مع أسعار الصرف المحلية بدون إنترنت."
      },
      {
        version: "1.1.0",
        date: "2026-04-10",
        notes: "إضافة الوضع العلمي المتقدم مع الدوال المثلثية واللوغاريتمية والأقواس المتداخلة، ونظام السجل المستمر مع البحث والتصدير."
      },
      {
        version: "1.0.0",
        date: "2025-11-20",
        notes: "الإطلاق الأولي: محرك حسابي عشري دقيق بدقة 0.1 + 0.2 = 0.3، معالجة النسب المئوية، ولوحة أجهزة دقيقة تحاكي العتاد الكلاسيكي."
      }
    ],

    // Programmer mode labels
    prog_hex: "HEX",
    prog_dec: "DEC",
    prog_oct: "OCT",
    prog_bin: "BIN",
    prog_word_size: "64-BIT SIGNED",
    
    // Converter categories
    cat_length: "طول",
    cat_mass: "كتلة",
    cat_temp: "درجة الحرارة",
    cat_area: "مساحة",
    cat_volume: "حجم",
    cat_speed: "سرعة",
    cat_time: "زمن",
    cat_data: "بيانات",
    cat_pressure: "ضغط",
    cat_energy: "طاقة",
    cat_power: "قدرة",
    cat_angle: "زوايا",
    cat_currency: "عملات",
    currency_rates_notice: "أسعار صرف محلية محفوظة في الجهاز — صفر طلبات شبكة.",
    currency_rate_edit: "تعديل سعر الصرف لـ 1 USD",
    save: "حفظ",
    last_updated: "آخر تحديث",

    // Accessibility ARIA labels
    aria_sqrt: "جذر تربيعي",
    aria_cbrt: "جذر تكعيبي",
    aria_sqr: "تربيع",
    aria_pow: "أس",
    aria_fact: "مضروب العدد",
    aria_abs: "القيمة المطلقة",
    aria_reciprocal: "مقلوب العدد",
    aria_percent: "نسبة مئوية",
    aria_backspace: "حذف حرف",
    aria_clear: "مسح الكل",
    aria_clear_entry: "مسح المدخل الحالي",
    aria_equals: "يساوي",
    aria_history: "سجل العمليات",
    aria_settings: "إعدادات الحاسبة",
    aria_mode_selector: "تبديل وضع الحاسبة"
  },
  en: {
    app_title: "R-7",
    app_subtitle: "Precision Calculator",
    standard: "Standard",
    scientific: "Scientific",
    programmer: "Programmer",
    converter: "Converter",
    history: "History",
    settings: "Settings",
    about: "About R-7",
    empty_history: "No operations yet — your first result will appear here.",
    first_launch_hint: "Try the keyboard — everything works without a mouse.",
    result_copied: "Result copied",
    expr_copied: "Expression copied",
    item_deleted: "Item deleted",
    history_cleared: "History cleared",
    export_filename: "r7-history.txt",
    export_header: "R-7 Calculation Log",
    search_history: "Search history...",
    clear_history_btn: "Clear History",
    export_history_btn: "Export TXT",
    close: "Close",
    copy: "Copy",
    insert_expr: "Insert Expression",
    delete: "Delete",

    // Status & Engine Errors (Exact strings as required)
    err_div_zero: "قسمة على صفر", // Keeping primary message as requested or bilingual
    err_incomplete: "التعبير غير مكتمل",
    err_out_of_range: "خارج المدى المسموح",
    err_invalid_paste: "Invalid text for calculation",

    // Settings
    theme_label: "Appearance",
    theme_dark: "Dark (Phosphor LCD)",
    theme_light: "Light (Ivory LCD)",
    language_label: "Language",
    angle_mode_label: "Default Angle Unit",
    sound_label: "Key Click Sound (WebAudio)",
    sound_enabled: "Enabled",
    sound_disabled: "Disabled",
    vibration_label: "Haptic Feedback",
    vibration_enabled: "Enabled",
    vibration_disabled: "Disabled",
    decimals_label: "Converter Decimals",

    // About modal
    version_label: "Version",
    license_label: "License: MIT License — Open Engineering Tool",
    changelog_title: "Changelog",
    changelog: [
      {
        version: "1.2.0",
        date: "2026-08-15",
        notes: "Added Programmer mode (64-bit HEX/DEC/OCT/BIN with simultaneous readout and bitwise operations) and Comprehensive Unit Converter with zero-network local currency storage."
      },
      {
        version: "1.1.0",
        date: "2026-04-10",
        notes: "Introduced Advanced Scientific mode with trigonometric, logarithmic functions, nested parentheses, and persistent searchable history with TXT export."
      },
      {
        version: "1.0.0",
        date: "2025-11-20",
        notes: "Initial launch: Precision decimal math engine (0.1 + 0.2 = 0.3), contextual percentage semantics, and vintage precision instrument hardware aesthetic."
      }
    ],

    // Programmer mode labels
    prog_hex: "HEX",
    prog_dec: "DEC",
    prog_oct: "OCT",
    prog_bin: "BIN",
    prog_word_size: "64-BIT SIGNED",

    // Converter categories
    cat_length: "Length",
    cat_mass: "Mass",
    cat_temp: "Temperature",
    cat_area: "Area",
    cat_volume: "Volume",
    cat_speed: "Speed",
    cat_time: "Time",
    cat_data: "Data",
    cat_pressure: "Pressure",
    cat_energy: "Energy",
    cat_power: "Power",
    cat_angle: "Angle",
    cat_currency: "Currencies",
    currency_rates_notice: "Local exchange rates stored on device — zero network requests.",
    currency_rate_edit: "Edit exchange rate per 1 USD",
    save: "Save",
    last_updated: "Last updated",

    // Accessibility ARIA labels
    aria_sqrt: "Square root",
    aria_cbrt: "Cube root",
    aria_sqr: "Square",
    aria_pow: "Power",
    aria_fact: "Factorial",
    aria_abs: "Absolute value",
    aria_reciprocal: "Reciprocal",
    aria_percent: "Percentage",
    aria_backspace: "Backspace",
    aria_clear: "Clear all",
    aria_clear_entry: "Clear entry",
    aria_equals: "Equals",
    aria_history: "Calculation history",
    aria_settings: "Calculator settings",
    aria_mode_selector: "Select calculator mode"
  }
};

let currentLang = "ar";

export function getLang() {
  return currentLang;
}

export function setLang(lang) {
  if (translations[lang]) {
    currentLang = lang;
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === "ar" ? "rtl" : "ltr";
  }
}

export function t(key) {
  return translations[currentLang]?.[key] || translations["ar"]?.[key] || key;
}
