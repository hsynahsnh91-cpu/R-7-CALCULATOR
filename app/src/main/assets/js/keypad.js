/**
 * R-7 Keypad & Desktop Keyboard Module
 * Desktop-first keyboard navigation, bash-style expression history (↑/↓),
 * long-press backspace AC flash, and unfailing button focus management.
 */

import { playClickSound, triggerHaptic } from "./audio.js";

export class KeypadController {
  constructor(options = {}) {
    this.onInput = options.onInput || (() => {});
    this.onAction = options.onAction || (() => {});
    this.onCopy = options.onCopy || (() => {});
    this.onPaste = options.onPaste || (() => {});
    this.onHistoryNav = options.onHistoryNav || (() => {});
    this.onToggleHistory = options.onToggleHistory || (() => {});
    this.onClearAll = options.onClearAll || (() => {});

    this.backspaceTimer = null;
    this.backspaceLongPressed = false;
    this.initKeyboardListeners();
  }

  initKeyboardListeners() {
    // Desktop keyboard listener on window
    window.addEventListener("keydown", (e) => {
      // Don't intercept if user is typing in a modal text input (e.g. search or rate edit)
      const activeEl = document.activeElement;
      if (activeEl && (activeEl.tagName === "INPUT" || activeEl.tagName === "TEXTAREA")) {
        return;
      }

      // Check Ctrl+C or Cmd+C
      if ((e.ctrlKey || e.metaKey) && (e.key === "c" || e.key === "C")) {
        const selection = window.getSelection()?.toString();
        if (!selection || selection.trim() === "") {
          e.preventDefault();
          this.onCopy();
          return;
        }
      }

      // Check Ctrl+V / Cmd+V
      if ((e.ctrlKey || e.metaKey) && (e.key === "v" || e.key === "V")) {
        // Handled by paste event
        return;
      }

      // Navigation: Up/Down arrow for bash-like history recall
      if (e.key === "ArrowUp") {
        e.preventDefault();
        this.onHistoryNav(-1);
        playClickSound();
        return;
      }
      if (e.key === "ArrowDown") {
        e.preventDefault();
        this.onHistoryNav(1);
        playClickSound();
        return;
      }

      // History shortcut 'h' or 'H'
      if (e.key === "h" || e.key === "H") {
        e.preventDefault();
        this.onToggleHistory();
        playClickSound();
        return;
      }

      // Pi shortcut 'p' or 'P'
      if (e.key === "p" || e.key === "P") {
        e.preventDefault();
        this.onInput("π");
        playClickSound();
        triggerHaptic(8);
        return;
      }

      // Digits
      if (/^[0-9]$/.test(e.key)) {
        e.preventDefault();
        this.onInput(e.key);
        playClickSound();
        triggerHaptic(8);
        return;
      }

      // Decimal point
      if (e.key === "." || e.key === ",") {
        e.preventDefault();
        this.onInput(".");
        playClickSound();
        triggerHaptic(8);
        return;
      }

      // Basic operators
      if (e.key === "+") {
        e.preventDefault();
        this.onInput("+");
        playClickSound();
        triggerHaptic(8);
        return;
      }
      if (e.key === "-") {
        e.preventDefault();
        this.onInput("−");
        playClickSound();
        triggerHaptic(8);
        return;
      }
      if (e.key === "*" || e.key === "x" || e.key === "X") {
        e.preventDefault();
        this.onInput("×");
        playClickSound();
        triggerHaptic(8);
        return;
      }
      if (e.key === "/") {
        e.preventDefault();
        this.onInput("÷");
        playClickSound();
        triggerHaptic(8);
        return;
      }
      if (e.key === "%") {
        e.preventDefault();
        this.onInput("%");
        playClickSound();
        triggerHaptic(8);
        return;
      }
      if (e.key === "^") {
        e.preventDefault();
        this.onInput("^");
        playClickSound();
        triggerHaptic(8);
        return;
      }
      if (e.key === "(" || e.key === ")") {
        e.preventDefault();
        this.onInput(e.key);
        playClickSound();
        triggerHaptic(8);
        return;
      }
      if (e.key === "!") {
        e.preventDefault();
        this.onInput("!");
        playClickSound();
        triggerHaptic(8);
        return;
      }

      // Equals / Calculate
      if (e.key === "Enter" || e.key === "=") {
        e.preventDefault();
        this.onAction("equals");
        playClickSound();
        triggerHaptic(12);
        return;
      }

      // Backspace (with long press check)
      if (e.key === "Backspace") {
        e.preventDefault();
        if (!e.repeat) {
          this.backspaceLongPressed = false;
          this.backspaceTimer = setTimeout(() => {
            this.backspaceLongPressed = true;
            this.onClearAll();
            playClickSound();
            triggerHaptic(30);
          }, 600);
        }
        return;
      }

      // Esc = AC (All Clear)
      if (e.key === "Escape") {
        e.preventDefault();
        this.onClearAll();
        playClickSound();
        triggerHaptic(16);
        return;
      }

      // Delete = CE (Clear Entry)
      if (e.key === "Delete") {
        e.preventDefault();
        this.onAction("clear_entry");
        playClickSound();
        triggerHaptic(10);
        return;
      }
    });

    window.addEventListener("keyup", (e) => {
      if (e.key === "Backspace") {
        if (this.backspaceTimer) {
          clearTimeout(this.backspaceTimer);
          this.backspaceTimer = null;
        }
        if (!this.backspaceLongPressed) {
          this.onAction("backspace");
          playClickSound();
          triggerHaptic(8);
        }
        this.backspaceLongPressed = false;
      }
    });

    // Paste handler
    window.addEventListener("paste", (e) => {
      const activeEl = document.activeElement;
      if (activeEl && (activeEl.tagName === "INPUT" || activeEl.tagName === "TEXTAREA")) {
        return;
      }
      e.preventDefault();
      const text = e.clipboardData?.getData("text") || "";
      this.onPaste(text);
    });
  }

  /**
   * Bind button events ensuring focus is immediately blurred to window
   * so keyboard shortcuts are never lost.
   */
  bindButton(button, onClick, onLongPress = null) {
    let pressTimer = null;
    let isLongPress = false;

    const startPress = () => {
      isLongPress = false;
      if (onLongPress) {
        pressTimer = setTimeout(() => {
          isLongPress = true;
          onLongPress();
          playClickSound();
          triggerHaptic(28);
        }, 600);
      }
    };

    const cancelPress = () => {
      if (pressTimer) {
        clearTimeout(pressTimer);
        pressTimer = null;
      }
    };

    const endPress = (e) => {
      cancelPress();
      if (!isLongPress) {
        onClick(e);
        playClickSound();
        triggerHaptic(8);
      }
      // Blur button immediately to keep window keyboard focus active
      button.blur();
    };

    button.addEventListener("pointerdown", startPress);
    button.addEventListener("pointerup", endPress);
    button.addEventListener("pointercancel", cancelPress);
    button.addEventListener("pointerleave", cancelPress);
  }
}
