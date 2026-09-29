/**
 * R-7 Storage & Settings Module
 * Single key storage `r7:v1` with graceful corrupt-data recovery.
 */

import { DEFAULT_CURRENCY_RATES } from "./converter.js";

export const STORAGE_KEY = "r7:v1";

const DEFAULT_STATE = {
  settings: {
    theme: "dark", // "dark" (phosphor green) or "light" (ivory)
    lang: "ar",    // "ar" (default) or "en"
    angleMode: "DEG", // "DEG", "RAD", "GRAD"
    sound: false,  // disabled by default
    vibration: true,
    converterPrecision: 1,
    firstLaunchHintSeen: false
  },
  memory: 0,
  history: [],
  currencyRates: {
    rates: { ...DEFAULT_CURRENCY_RATES },
    lastUpdated: "2026-08-15"
  }
};

let appState = null;

export function loadAppState() {
  if (appState) return appState;

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      appState = JSON.parse(JSON.stringify(DEFAULT_STATE));
      saveAppState(appState);
      return appState;
    }

    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object") {
      throw new Error("Invalid storage structure");
    }

    // Merge with defaults to guarantee all fields exist
    appState = {
      settings: { ...DEFAULT_STATE.settings, ...(parsed.settings || {}) },
      memory: typeof parsed.memory === "number" ? parsed.memory : 0,
      history: Array.isArray(parsed.history) ? parsed.history.slice(0, 200) : [],
      currencyRates: {
        rates: { ...DEFAULT_CURRENCY_RATES, ...(parsed.currencyRates?.rates || {}) },
        lastUpdated: parsed.currencyRates?.lastUpdated || "2026-08-15"
      }
    };
    return appState;
  } catch (err) {
    console.warn("R-7 storage corrupted, resetting to defaults:", err);
    appState = JSON.parse(JSON.stringify(DEFAULT_STATE));
    saveAppState(appState);
    return appState;
  }
}

export function saveAppState(state = appState) {
  try {
    if (!state) return;
    appState = state;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appState));
  } catch (e) {
    console.error("Failed to save R-7 state:", e);
  }
}

export function getSettings() {
  return loadAppState().settings;
}

export function updateSettings(partial) {
  const state = loadAppState();
  state.settings = { ...state.settings, ...partial };
  saveAppState(state);
  return state.settings;
}

export function getMemory() {
  return loadAppState().memory;
}

export function setMemory(val) {
  const state = loadAppState();
  state.memory = Number(val) || 0;
  saveAppState(state);
  return state.memory;
}

export function getStoredCurrencyRates() {
  return loadAppState().currencyRates;
}

export function saveCurrencyRates(rates, lastUpdated) {
  const state = loadAppState();
  state.currencyRates = { rates, lastUpdated };
  saveAppState(state);
}
