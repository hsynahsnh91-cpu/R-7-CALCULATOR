/**
 * R-7 Display Module
 * Controls the internal LCD display rendering, tabular numbers, status indicators, and live preview.
 */

import { formatThousands } from "./engine.js";

export class DisplayController {
  constructor(elements) {
    this.modeBadge = elements.modeBadge;
    this.angleBadge = elements.angleBadge;
    this.memoryBadge = elements.memoryBadge;
    this.exprLine = elements.exprLine;
    this.previewLine = elements.previewLine;
    this.mainLine = elements.mainLine;
    this.statusLine = elements.statusLine;
    this.lcdContainer = elements.lcdContainer;

    this.livePreviewTimer = null;
    this.errorTimer = null;
  }

  setModeBadge(text) {
    if (this.modeBadge) this.modeBadge.textContent = text;
  }

  setAngleBadge(text) {
    if (this.angleBadge) this.angleBadge.textContent = text;
  }

  setMemoryActive(active) {
    if (this.memoryBadge) {
      if (active) {
        this.memoryBadge.classList.add("active");
        this.memoryBadge.setAttribute("aria-hidden", "false");
      } else {
        this.memoryBadge.classList.remove("active");
        this.memoryBadge.setAttribute("aria-hidden", "true");
      }
    }
  }

  setExpression(expr) {
    if (this.exprLine) {
      this.exprLine.textContent = expr || "";
      // Auto-scroll to end so user sees the cursor position
      this.exprLine.scrollLeft = this.exprLine.scrollWidth;
    }
  }

  setMain(text, isError = false) {
    if (this.mainLine) {
      this.mainLine.textContent = text;
      if (isError) {
        this.mainLine.classList.add("error-text");
      } else {
        this.mainLine.classList.remove("error-text");
      }
    }
  }

  setStatus(msg) {
    if (this.statusLine) {
      this.statusLine.textContent = msg || "";
    }
  }

  clearStatus() {
    if (this.statusLine) {
      this.statusLine.textContent = "";
    }
  }

  setLivePreview(val) {
    if (this.previewLine) {
      if (val !== null && val !== undefined && val !== "") {
        this.previewLine.textContent = `= ${val}`;
        this.previewLine.style.opacity = "0.7";
      } else {
        this.previewLine.textContent = "";
        this.previewLine.style.opacity = "0";
      }
    }
  }

  flashLcd() {
    if (this.lcdContainer) {
      this.lcdContainer.classList.add("flash");
      setTimeout(() => {
        this.lcdContainer.classList.remove("flash");
      }, 160);
    }
  }
}
