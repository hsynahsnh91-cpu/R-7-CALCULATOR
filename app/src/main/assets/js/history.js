/**
 * R-7 History Module
 * Manages up to 200 operations, search, long-press actions, and TXT export.
 */

import { loadAppState, saveAppState } from "./settings.js";
import { t } from "./i18n.js";

const MAX_HISTORY = 200;

export class HistoryManager {
  constructor() {
    this.onSelectResult = null;
    this.onSelectExpression = null;
  }

  getAll() {
    const state = loadAppState();
    return state.history || [];
  }

  addEntry({ expr, rawExpr, result, display, mode, autoClosedCount = 0 }) {
    const state = loadAppState();
    const entry = {
      id: Date.now().toString(36) + Math.random().toString(36).substr(2, 4),
      expr,
      rawExpr: rawExpr || expr,
      result,
      display,
      mode: mode || "standard",
      autoClosedCount,
      timestamp: Date.now()
    };

    state.history.unshift(entry);
    if (state.history.length > MAX_HISTORY) {
      state.history = state.history.slice(0, MAX_HISTORY);
    }
    saveAppState(state);
    return entry;
  }

  deleteEntry(id) {
    const state = loadAppState();
    state.history = state.history.filter(item => item.id !== id);
    saveAppState(state);
  }

  clear() {
    const state = loadAppState();
    state.history = [];
    saveAppState(state);
  }

  search(query) {
    if (!query || query.trim() === "") return this.getAll();
    const q = query.trim().toLowerCase();
    return this.getAll().filter(item => {
      const e = (item.expr || "").toLowerCase();
      const d = (item.display || "").toLowerCase();
      const m = (item.mode || "").toLowerCase();
      return e.includes(q) || d.includes(q) || m.includes(q);
    });
  }

  /**
   * Generates formatted text content and initiates browser download
   */
  exportTxt() {
    const list = this.getAll();
    if (list.length === 0) return;

    let content = `========================================\n`;
    content += `        ${t("export_header")}\n`;
    content += `========================================\n`;
    content += `Date: ${new Date().toISOString()}\n`;
    content += `Total Entries: ${list.length}\n\n`;

    list.forEach((item, idx) => {
      const d = new Date(item.timestamp);
      const timeStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}:${String(d.getSeconds()).padStart(2, "0")}`;
      content += `[#${list.length - idx}] [${timeStr}] [${item.mode.toUpperCase()}]\n`;
      content += `  Expr:   ${item.expr}\n`;
      content += `  Result: ${item.display}\n\n`;
    });

    const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = t("export_filename");
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}
