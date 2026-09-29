/**
 * R-7 Main Application Orchestrator
 * Integrates Engine, Display, Keypad, History, Converter, Programmer, Audio, Settings, and i18n.
 */

import { R7Engine, formatThousands } from "./engine.js";
import { DisplayController } from "./display.js";
import { KeypadController } from "./keypad.js";
import { HistoryManager } from "./history.js";
import { ProgrammerEngine } from "./programmer.js";
import { UnitConverter, CONVERSION_CATEGORIES } from "./converter.js";
import {
  loadAppState,
  getSettings,
  updateSettings,
  getMemory,
  setMemory,
  getStoredCurrencyRates,
  saveCurrencyRates
} from "./settings.js";
import { initAudio, playClickSound, triggerHaptic, triggerErrorFeedback, setSoundEnabled, setVibrationEnabled } from "./audio.js";
import { t, getLang, setLang } from "./i18n.js";

document.addEventListener("DOMContentLoaded", () => {
  // Load persistent state
  const state = loadAppState();
  const settings = state.settings;

  // Initialize Audio & Haptics
  initAudio({ sound: settings.sound, vibration: settings.vibration });

  // Apply Theme and Language
  document.documentElement.setAttribute("data-theme", settings.theme || "dark");
  setLang(settings.lang || "ar");

  // Engines
  const mathEngine = new R7Engine({ angleMode: settings.angleMode || "DEG", lastAns: 0 });
  const progEngine = new ProgrammerEngine();
  const converter = new UnitConverter({
    currencyRates: state.currencyRates?.rates,
    ratesLastUpdated: state.currencyRates?.lastUpdated,
    precision: settings.converterPrecision ?? 1
  });
  const historyMgr = new HistoryManager();

  // Mode states (Preserved separately!)
  const modeStates = {
    standard: { expr: "", result: "0", rawExpr: "", isNewInput: true },
    scientific: { expr: "", result: "0", rawExpr: "", isNewInput: true },
    programmer: { isNewInput: true },
    converter: {
      category: "temp",
      val: "100",
      fromUnit: "F",
      toUnit: "C"
    }
  };

  let currentMode = "standard"; // "standard", "scientific", "programmer", "converter"
  let livePreviewTimer = null;
  let errorTimer = null;
  let historyNavIndex = -1;

  // DOM Elements
  const lcdElements = {
    modeBadge: document.getElementById("lcdModeBadge"),
    angleBadge: document.getElementById("lcdAngleBadge"),
    memoryBadge: document.getElementById("lcdMemoryBadge"),
    exprLine: document.getElementById("lcdExprLine"),
    previewLine: document.getElementById("lcdPreviewLine"),
    mainLine: document.getElementById("lcdMainLine"),
    statusLine: document.getElementById("lcdStatusLine"),
    lcdContainer: document.getElementById("lcdContainer")
  };

  const display = new DisplayController(lcdElements);
  display.setMemoryActive(getMemory() !== 0);
  display.setAngleBadge(mathEngine.angleMode);

  // Keypad Controller
  const keypad = new KeypadController({
    onInput: (val) => handleInput(val),
    onAction: (act) => handleAction(act),
    onCopy: () => copyResultToClipboard(),
    onPaste: (text) => handlePaste(text),
    onHistoryNav: (dir) => handleHistoryNav(dir),
    onToggleHistory: () => toggleHistoryModal(true),
    onClearAll: () => clearAll()
  });

  // UI Setup & Bindings
  setupModeTabs();
  setupKeypadButtons();
  setupConverterUI();
  setupModals();
  setupFirstLaunchHint();

  // Initial render
  updateUIForCurrentMode();

  /* ==========================================================================
     Mode Switching
     ========================================================================== */
  function switchMode(newMode) {
    if (newMode === currentMode) return;
    currentMode = newMode;
    historyNavIndex = -1;
    display.clearStatus();

    // Update active tab styles
    document.querySelectorAll(".r7-mode-tab").forEach(tab => {
      tab.classList.toggle("active", tab.dataset.mode === newMode);
      tab.setAttribute("aria-selected", tab.dataset.mode === newMode ? "true" : "false");
    });

    // Toggle panels visibility
    document.getElementById("keypadStandard").style.display = newMode === "standard" ? "grid" : "none";
    document.getElementById("keypadScientific").style.display = newMode === "scientific" ? "grid" : "none";
    document.getElementById("keypadProgrammer").style.display = newMode === "programmer" ? "grid" : "none";
    document.getElementById("panelConverter").style.display = newMode === "converter" ? "flex" : "none";
    document.getElementById("progReadout").style.display = newMode === "programmer" ? "flex" : "none";
    document.getElementById("memBar").style.display = (newMode === "standard" || newMode === "scientific") ? "flex" : "none";
    document.getElementById("lcdContainer").style.display = newMode === "converter" ? "none" : "flex";

    updateUIForCurrentMode();
  }

  function updateUIForCurrentMode() {
    if (currentMode === "standard" || currentMode === "scientific") {
      const st = modeStates[currentMode];
      display.setModeBadge(currentMode === "standard" ? "STD" : "SCI");
      lcdElements.angleBadge.style.display = currentMode === "scientific" ? "inline-block" : "none";
      display.setExpression(st.expr);
      display.setMain(st.result || "0");
      scheduleLivePreview(st.expr);
    } else if (currentMode === "programmer") {
      display.setModeBadge(progEngine.currentBase);
      lcdElements.angleBadge.style.display = "none";
      display.setExpression(progEngine.pendingOp ? `${progEngine.storedValue?.toString(16).toUpperCase() || ""} ${progEngine.pendingOp}` : "");
      display.setMain(progEngine.buffer);
      updateProgrammerReadout();
      updateProgrammerKeypadState();
    } else if (currentMode === "converter") {
      updateConverterDisplay();
    }
  }

  /* ==========================================================================
     Math / Standard & Scientific Input Handling
     ========================================================================== */
  function handleInput(char) {
    if (currentMode === "programmer") {
      progEngine.inputDigit(char);
      display.setMain(progEngine.buffer);
      updateProgrammerReadout();
      return;
    }

    if (currentMode === "converter") {
      handleConverterDigit(char);
      return;
    }

    const st = modeStates[currentMode];
    display.clearStatus();

    // Starting new calculation after equals
    if (st.isNewInput) {
      if (["+", "−", "×", "÷", "^", "%"].includes(char)) {
        st.expr = `Ans ${char} `;
      } else {
        st.expr = char;
      }
      st.isNewInput = false;
    } else {
      if (["+", "−", "×", "÷", "^"].includes(char)) {
        st.expr += ` ${char} `;
      } else {
        st.expr += char;
      }
    }

    display.setExpression(st.expr);
    scheduleLivePreview(st.expr);
  }

  function handleAction(act) {
    if (currentMode === "programmer") {
      handleProgrammerAction(act);
      return;
    }

    if (currentMode === "converter") {
      handleConverterAction(act);
      return;
    }

    const st = modeStates[currentMode];

    switch (act) {
      case "equals":
        evaluateCurrent();
        break;

      case "backspace":
        if (st.expr.length > 0) {
          // If ends with space and operator e.g. " + ", remove full token
          if (st.expr.endsWith(" ")) {
            st.expr = st.expr.trimEnd();
            st.expr = st.expr.slice(0, -1).trimEnd();
          } else {
            st.expr = st.expr.slice(0, -1);
          }
          display.setExpression(st.expr);
          display.clearStatus();
          scheduleLivePreview(st.expr);
        }
        break;

      case "clear_entry":
        st.expr = "";
        display.setExpression("");
        display.setLivePreview("");
        display.clearStatus();
        break;

      case "clear_all":
        clearAll();
        break;

      case "sign":
        toggleSign();
        break;

      case "toggle_angle":
        toggleAngleMode();
        break;

      // Memory actions
      case "mc":
        setMemory(0);
        display.setMemoryActive(false);
        showToast(t("close"));
        break;
      case "mr":
        {
          const m = getMemory();
          handleInput(String(m));
        }
        break;
      case "m_plus":
        {
          const curVal = Number(st.result) || 0;
          const newM = getMemory() + curVal;
          setMemory(newM);
          display.setMemoryActive(newM !== 0);
          showToast(`M = ${newM}`);
        }
        break;
      case "m_minus":
        {
          const curVal = Number(st.result) || 0;
          const newM = getMemory() - curVal;
          setMemory(newM);
          display.setMemoryActive(newM !== 0);
          showToast(`M = ${newM}`);
        }
        break;
    }
  }

  function evaluateCurrent() {
    const st = modeStates[currentMode];
    if (!st.expr || st.expr.trim() === "") return;

    if (livePreviewTimer) clearTimeout(livePreviewTimer);
    if (errorTimer) clearTimeout(errorTimer);

    const evalResult = mathEngine.evaluate(st.expr);

    if (evalResult.success) {
      st.result = evalResult.display;
      st.isNewInput = true;
      display.setMain(evalResult.display);
      display.clearStatus();
      display.setLivePreview("");

      // Record in History
      historyMgr.addEntry({
        expr: evalResult.closedExpr,
        rawExpr: st.expr,
        result: evalResult.result,
        display: evalResult.display,
        mode: currentMode,
        autoClosedCount: evalResult.autoClosedCount
      });

      renderHistoryList();
    } else {
      triggerErrorFeedback();
      display.setStatus(evalResult.error);
    }
  }

  function scheduleLivePreview(expr) {
    if (livePreviewTimer) clearTimeout(livePreviewTimer);
    if (errorTimer) clearTimeout(errorTimer);

    if (!expr || expr.trim() === "") {
      display.setLivePreview("");
      return;
    }

    // Debounced live preview 150ms
    livePreviewTimer = setTimeout(() => {
      const res = mathEngine.evaluate(expr);
      if (res.success) {
        display.setLivePreview(res.display);
        display.clearStatus();
      } else {
        display.setLivePreview("");
        // Show errors only after 1 second of inactivity as required by §8
        errorTimer = setTimeout(() => {
          display.setStatus(res.error);
        }, 850);
      }
    }, 150);
  }

  function clearAll() {
    display.flashLcd();
    const st = modeStates[currentMode];
    if (currentMode === "programmer") {
      progEngine.clear();
      display.setMain("0");
      display.setExpression("");
      updateProgrammerReadout();
    } else {
      st.expr = "";
      st.result = "0";
      st.isNewInput = true;
      display.setExpression("");
      display.setMain("0");
      display.setLivePreview("");
      display.clearStatus();
    }
  }

  function toggleSign() {
    const st = modeStates[currentMode];
    if (!st.expr || st.expr === "") {
      st.expr = "-";
      display.setExpression(st.expr);
      return;
    }

    if (st.expr.startsWith("-(")) {
      st.expr = st.expr.slice(2);
      if (st.expr.endsWith(")")) st.expr = st.expr.slice(0, -1);
    } else if (st.expr.startsWith("-")) {
      st.expr = st.expr.slice(1);
    } else {
      st.expr = `-(${st.expr})`;
    }
    display.setExpression(st.expr);
    scheduleLivePreview(st.expr);
  }

  function toggleAngleMode() {
    const modes = ["DEG", "RAD", "GRAD"];
    const curIdx = modes.indexOf(mathEngine.angleMode);
    const nextMode = modes[(curIdx + 1) % modes.length];
    mathEngine.setAngleMode(nextMode);
    updateSettings({ angleMode: nextMode });
    display.setAngleBadge(nextMode);
    showToast(`Angle: ${nextMode}`);
    if (modeStates[currentMode].expr) {
      scheduleLivePreview(modeStates[currentMode].expr);
    }
  }

  function handlePaste(rawText) {
    const sanitized = mathEngine.sanitizePaste(rawText);
    if (sanitized === null) {
      triggerErrorFeedback();
      display.setStatus(t("err_invalid_paste"));
      return;
    }
    const st = modeStates[currentMode];
    st.expr += sanitized;
    display.setExpression(st.expr);
    scheduleLivePreview(st.expr);
  }

  function copyResultToClipboard() {
    let textToCopy = "";
    if (currentMode === "programmer") {
      textToCopy = progEngine.buffer;
    } else if (currentMode === "converter") {
      textToCopy = document.getElementById("convResultDisplay")?.textContent || "";
    } else {
      textToCopy = modeStates[currentMode].result || "0";
    }

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(textToCopy).then(() => {
        showToast(t("result_copied"));
      });
    } else {
      showToast(t("result_copied"));
    }
  }

  /* ==========================================================================
     Bash-style History Recall (↑ / ↓)
     ========================================================================== */
  function handleHistoryNav(dir) {
    const list = historyMgr.getAll();
    if (list.length === 0) return;

    if (dir === -1) {
      // Arrow Up: recall older expression
      if (historyNavIndex < list.length - 1) {
        historyNavIndex++;
        const item = list[historyNavIndex];
        modeStates[currentMode].expr = item.rawExpr || item.expr;
        modeStates[currentMode].isNewInput = false;
        display.setExpression(modeStates[currentMode].expr);
        scheduleLivePreview(modeStates[currentMode].expr);
      }
    } else if (dir === 1) {
      // Arrow Down: recall newer expression
      if (historyNavIndex > 0) {
        historyNavIndex--;
        const item = list[historyNavIndex];
        modeStates[currentMode].expr = item.rawExpr || item.expr;
        modeStates[currentMode].isNewInput = false;
        display.setExpression(modeStates[currentMode].expr);
        scheduleLivePreview(modeStates[currentMode].expr);
      } else if (historyNavIndex === 0) {
        historyNavIndex = -1;
        modeStates[currentMode].expr = "";
        display.setExpression("");
        display.setLivePreview("");
      }
    }
  }

  /* ==========================================================================
     Programmer Mode Operations
     ========================================================================== */
  function handleProgrammerAction(act) {
    switch (act) {
      case "equals":
        {
          const res = progEngine.calculate();
          if (res.success) {
            display.setMain(progEngine.buffer);
            display.setExpression("");
            updateProgrammerReadout();
          } else {
            triggerErrorFeedback();
            display.setStatus(res.error);
          }
        }
        break;
      case "backspace":
        progEngine.backspace();
        display.setMain(progEngine.buffer);
        updateProgrammerReadout();
        break;
      case "clear_entry":
        progEngine.clearEntry();
        display.setMain("0");
        updateProgrammerReadout();
        break;
      case "clear_all":
        clearAll();
        break;
      case "not":
        progEngine.not();
        display.setMain(progEngine.buffer);
        updateProgrammerReadout();
        break;
      case "sign":
        progEngine.toggleSign();
        display.setMain(progEngine.buffer);
        updateProgrammerReadout();
        break;
      default:
        // Binary operations (+, -, *, /, %, AND, OR, XOR, SHL, SHR)
        progEngine.setOperation(act);
        display.setExpression(`${progEngine.storedValue !== null ? progEngine.formatValueForBase(progEngine.storedValue, progEngine.currentBase) : ""} ${act}`);
        break;
    }
  }

  function updateProgrammerReadout() {
    const bases = progEngine.getAllBases();
    document.getElementById("progHexVal").textContent = bases.HEX;
    document.getElementById("progDecVal").textContent = bases.DEC;
    document.getElementById("progOctVal").textContent = bases.OCT;
    document.getElementById("progBinVal").textContent = bases.BIN;

    // Highlight active base row
    ["HEX", "DEC", "OCT", "BIN"].forEach(b => {
      const row = document.getElementById(`progRow${b}`);
      if (row) {
        row.classList.toggle("active", b === progEngine.currentBase);
      }
    });
  }

  function updateProgrammerKeypadState() {
    // Enable/disable digits based on active base
    const base = progEngine.currentBase;
    const buttons = document.querySelectorAll("#keypadProgrammer button[data-char]");
    buttons.forEach(btn => {
      const ch = btn.dataset.char;
      const valid = progEngine.isValidDigitForBase(ch, base);
      btn.disabled = !valid;
    });
  }

  /* ==========================================================================
     Unit Converter Handling
     ========================================================================== */
  function setupConverterUI() {
    const catSelect = document.getElementById("convCatSelect");
    const fromSelect = document.getElementById("convFromSelect");
    const toSelect = document.getElementById("convToSelect");
    const inputField = document.getElementById("convInputField");
    const resultDisplay = document.getElementById("convResultDisplay");

    if (!catSelect || !fromSelect || !toSelect) return;

    // Populate category dropdown
    catSelect.innerHTML = "";
    Object.keys(CONVERSION_CATEGORIES).forEach(k => {
      const opt = document.createElement("option");
      opt.value = k;
      opt.textContent = getLang() === "ar" ? CONVERSION_CATEGORIES[k].name_ar : CONVERSION_CATEGORIES[k].name_en;
      catSelect.appendChild(opt);
    });

    catSelect.value = modeStates.converter.category;

    const populateUnits = (catKey) => {
      const cat = CONVERSION_CATEGORIES[catKey];
      fromSelect.innerHTML = "";
      toSelect.innerHTML = "";

      if (cat.isCurrency) {
        const currencies = Object.keys(converter.currencyRates);
        currencies.forEach(c => {
          fromSelect.appendChild(new Option(c, c));
          toSelect.appendChild(new Option(c, c));
        });
        document.getElementById("convCurrencyRow").style.display = "block";
      } else {
        Object.keys(cat.units).forEach(uKey => {
          const u = cat.units[uKey];
          const label = getLang() === "ar" ? u.name_ar : u.name_en;
          fromSelect.appendChild(new Option(label, uKey));
          toSelect.appendChild(new Option(label, uKey));
        });
        document.getElementById("convCurrencyRow").style.display = "none";
      }

      // Default unit pairs
      const keys = cat.isCurrency ? Object.keys(converter.currencyRates) : Object.keys(cat.units);
      fromSelect.value = keys[0] || "";
      toSelect.value = keys[1] || keys[0] || "";
    };

    populateUnits(catSelect.value);

    catSelect.addEventListener("change", (e) => {
      modeStates.converter.category = e.target.value;
      populateUnits(e.target.value);
      updateConverterDisplay();
    });

    fromSelect.addEventListener("change", () => updateConverterDisplay());
    toSelect.addEventListener("change", () => updateConverterDisplay());

    inputField.value = modeStates.converter.val;
    inputField.addEventListener("input", (e) => {
      modeStates.converter.val = e.target.value;
      updateConverterDisplay();
    });

    // Edit currency rates button
    document.getElementById("btnEditCurrencyRates")?.addEventListener("click", () => {
      openCurrencyModal();
    });
  }

  function updateConverterDisplay() {
    const cat = modeStates.converter.category;
    const from = document.getElementById("convFromSelect")?.value;
    const to = document.getElementById("convToSelect")?.value;
    const valStr = modeStates.converter.val || "0";
    const val = parseFloat(valStr) || 0;

    const res = converter.convert(cat, val, from, to);
    const resultDisplay = document.getElementById("convResultDisplay");
    if (resultDisplay) {
      resultDisplay.textContent = res.toString();
    }
  }

  function handleConverterDigit(digit) {
    if (modeStates.converter.val === "0" && digit !== ".") {
      modeStates.converter.val = digit;
    } else {
      if (digit === "." && modeStates.converter.val.includes(".")) return;
      modeStates.converter.val += digit;
    }
    const inputField = document.getElementById("convInputField");
    if (inputField) inputField.value = modeStates.converter.val;
    updateConverterDisplay();
  }

  function handleConverterAction(act) {
    if (act === "backspace") {
      modeStates.converter.val = modeStates.converter.val.slice(0, -1) || "0";
    } else if (act === "clear_all" || act === "clear_entry") {
      modeStates.converter.val = "0";
    }
    const inputField = document.getElementById("convInputField");
    if (inputField) inputField.value = modeStates.converter.val;
    updateConverterDisplay();
  }

  /* ==========================================================================
     Keypad Button Bindings
     ========================================================================== */
  function setupKeypadButtons() {
    document.querySelectorAll(".r7-btn").forEach(btn => {
      const char = btn.dataset.char;
      const act = btn.dataset.act;

      if (char) {
        keypad.bindButton(btn, () => handleInput(char));
      } else if (act) {
        if (act === "backspace") {
          keypad.bindButton(
            btn,
            () => handleAction("backspace"),
            () => clearAll() // Long press backspace clears all with flash!
          );
        } else {
          keypad.bindButton(btn, () => handleAction(act));
        }
      }
    });

    // Programmer base switcher rows
    ["HEX", "DEC", "OCT", "BIN"].forEach(base => {
      document.getElementById(`progRow${base}`)?.addEventListener("click", () => {
        progEngine.setBase(base);
        display.setModeBadge(base);
        display.setMain(progEngine.buffer);
        updateProgrammerReadout();
        updateProgrammerKeypadState();
        playClickSound();
        triggerHaptic(10);
      });
    });

    // Angle mode badge click toggle
    document.getElementById("lcdAngleBadge")?.addEventListener("click", () => {
      toggleAngleMode();
    });
  }

  function setupModeTabs() {
    document.querySelectorAll(".r7-mode-tab").forEach(tab => {
      tab.addEventListener("click", () => {
        switchMode(tab.dataset.mode);
        playClickSound();
        triggerHaptic(8);
      });
    });
  }

  /* ==========================================================================
     History & Modals Setup
     ========================================================================== */
  function setupModals() {
    // Header Buttons
    document.getElementById("btnOpenHistory")?.addEventListener("click", () => toggleHistoryModal(true));
    document.getElementById("btnOpenSettings")?.addEventListener("click", () => toggleSettingsModal(true));
    document.getElementById("btnOpenAbout")?.addEventListener("click", () => toggleAboutModal(true));

    // Close Buttons
    document.getElementById("btnCloseHistory")?.addEventListener("click", () => toggleHistoryModal(false));
    document.getElementById("btnCloseSettings")?.addEventListener("click", () => toggleSettingsModal(false));
    document.getElementById("btnCloseAbout")?.addEventListener("click", () => toggleAboutModal(false));
    document.getElementById("btnCloseCurrency")?.addEventListener("click", () => toggleCurrencyModal(false));
    document.getElementById("btnCloseItemMenu")?.addEventListener("click", () => toggleItemMenuModal(false));

    // History Actions
    document.getElementById("btnClearHistory")?.addEventListener("click", () => {
      historyMgr.clear();
      renderHistoryList();
      showToast(t("history_cleared"));
    });

    document.getElementById("btnExportHistory")?.addEventListener("click", () => {
      historyMgr.exportTxt();
    });

    document.getElementById("historySearchInput")?.addEventListener("input", (e) => {
      renderHistoryList(e.target.value);
    });

    // Settings Form Handlers
    const themeSelect = document.getElementById("settingThemeSelect");
    const langSelect = document.getElementById("settingLangSelect");
    const soundCheck = document.getElementById("settingSoundCheck");
    const vibrationCheck = document.getElementById("settingVibrationCheck");
    const decimalsInput = document.getElementById("settingDecimalsInput");

    if (themeSelect) {
      themeSelect.value = settings.theme || "dark";
      themeSelect.addEventListener("change", (e) => {
        document.documentElement.setAttribute("data-theme", e.target.value);
        updateSettings({ theme: e.target.value });
      });
    }

    if (langSelect) {
      langSelect.value = getLang();
      langSelect.addEventListener("change", (e) => {
        setLang(e.target.value);
        updateSettings({ lang: e.target.value });
        window.location.reload(); // Reload to refresh interface labels
      });
    }

    if (soundCheck) {
      soundCheck.checked = !!settings.sound;
      soundCheck.addEventListener("change", (e) => {
        setSoundEnabled(e.target.checked);
        updateSettings({ sound: e.target.checked });
      });
    }

    if (vibrationCheck) {
      vibrationCheck.checked = settings.vibration !== false;
      vibrationCheck.addEventListener("change", (e) => {
        setVibrationEnabled(e.target.checked);
        updateSettings({ vibration: e.target.checked });
      });
    }

    if (decimalsInput) {
      decimalsInput.value = settings.converterPrecision ?? 1;
      decimalsInput.addEventListener("change", (e) => {
        const val = parseInt(e.target.value, 10) || 1;
        converter.precision = val;
        updateSettings({ converterPrecision: val });
        updateConverterDisplay();
      });
    }
  }

  function renderHistoryList(query = "") {
    const listEl = document.getElementById("historyListContainer");
    if (!listEl) return;

    const items = historyMgr.search(query);
    listEl.innerHTML = "";

    if (items.length === 0) {
      listEl.innerHTML = `<div class="r7-empty-history">${t("empty_history")}</div>`;
      return;
    }

    items.forEach(item => {
      const card = document.createElement("div");
      card.className = "r7-history-item";

      // Render auto-closed parentheses with dim opacity
      let exprHtml = item.rawExpr || item.expr;
      if (item.autoClosedCount > 0) {
        exprHtml = `${item.rawExpr}<span class="dim-paren">${")".repeat(item.autoClosedCount)}</span>`;
      }

      card.innerHTML = `
        <div class="r7-history-item-top">
          <span>${item.mode.toUpperCase()}</span>
          <span>${new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
        </div>
        <div class="r7-history-expr">${exprHtml}</div>
        <div class="r7-history-result">= ${item.display}</div>
      `;

      // Tap: inserts result into current expression
      let pressTimer = null;
      let isLong = false;

      card.addEventListener("pointerdown", () => {
        isLong = false;
        pressTimer = setTimeout(() => {
          isLong = true;
          openItemMenuModal(item);
          triggerHaptic(24);
        }, 500);
      });

      card.addEventListener("pointerup", () => {
        if (pressTimer) clearTimeout(pressTimer);
        if (!isLong) {
          handleInput(String(item.display));
          toggleHistoryModal(false);
          showToast(`+ ${item.display}`);
        }
      });

      card.addEventListener("pointercancel", () => {
        if (pressTimer) clearTimeout(pressTimer);
      });

      listEl.appendChild(card);
    });
  }

  let selectedHistoryItem = null;
  function openItemMenuModal(item) {
    selectedHistoryItem = item;
    toggleItemMenuModal(true);

    document.getElementById("btnItemCopy")?.onclick = () => {
      navigator.clipboard?.writeText(item.display);
      showToast(t("result_copied"));
      toggleItemMenuModal(false);
    };

    document.getElementById("btnItemInsertExpr")?.onclick = () => {
      modeStates[currentMode].expr = item.rawExpr || item.expr;
      modeStates[currentMode].isNewInput = false;
      display.setExpression(modeStates[currentMode].expr);
      toggleItemMenuModal(false);
      toggleHistoryModal(false);
      showToast(t("expr_copied"));
    };

    document.getElementById("btnItemDelete")?.onclick = () => {
      historyMgr.deleteEntry(item.id);
      renderHistoryList();
      toggleItemMenuModal(false);
      showToast(t("item_deleted"));
    };
  }

  function openCurrencyModal() {
    toggleCurrencyModal(true);
    const container = document.getElementById("currencyRatesInputsContainer");
    if (!container) return;

    container.innerHTML = "";
    Object.keys(converter.currencyRates).forEach(curr => {
      if (curr === "USD") return; // USD is base
      const row = document.createElement("div");
      row.className = "r7-setting-row";
      row.innerHTML = `
        <span class="r7-setting-label">1 USD =</span>
        <div style="display:flex; align-items:center; gap:6px;">
          <input type="number" step="0.001" id="rate_${curr}" value="${converter.currencyRates[curr]}" class="r7-history-search" style="width:110px; text-align:right;">
          <span style="font-family:var(--r7-font-mono); font-weight:700;">${curr}</span>
        </div>
      `;
      container.appendChild(row);
    });

    document.getElementById("btnSaveCurrencyRates")?.onclick = () => {
      const newRates = { ...converter.currencyRates };
      Object.keys(converter.currencyRates).forEach(curr => {
        if (curr === "USD") return;
        const inp = document.getElementById(`rate_${curr}`);
        if (inp) {
          newRates[curr] = parseFloat(inp.value) || newRates[curr];
        }
      });
      const today = new Date().toISOString().split("T")[0];
      converter.setRates(newRates, today);
      saveCurrencyRates(newRates, today);
      toggleCurrencyModal(false);
      updateConverterDisplay();
      showToast(t("save"));
    };
  }

  function toggleHistoryModal(open) {
    const modal = document.getElementById("historyModalOverlay");
    if (modal) {
      modal.classList.toggle("open", open);
      if (open) renderHistoryList();
    }
  }

  function toggleSettingsModal(open) {
    const modal = document.getElementById("settingsModalOverlay");
    if (modal) modal.classList.toggle("open", open);
  }

  function toggleAboutModal(open) {
    const modal = document.getElementById("aboutModalOverlay");
    if (modal) modal.classList.toggle("open", open);
  }

  function toggleCurrencyModal(open) {
    const modal = document.getElementById("currencyModalOverlay");
    if (modal) modal.classList.toggle("open", open);
  }

  function toggleItemMenuModal(open) {
    const modal = document.getElementById("itemMenuModalOverlay");
    if (modal) modal.classList.toggle("open", open);
  }

  function setupFirstLaunchHint() {
    if (!settings.firstLaunchHintSeen) {
      setTimeout(() => {
        showToast(t("first_launch_hint"), 4500);
        updateSettings({ firstLaunchHintSeen: true });
      }, 1000);
    }
  }

  function showToast(message, duration = 2200) {
    const toast = document.getElementById("r7Toast");
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add("show");
    setTimeout(() => {
      toast.classList.remove("show");
    }, duration);
  }
});
