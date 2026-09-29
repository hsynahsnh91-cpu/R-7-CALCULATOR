/**
 * R-7 Programmer Mode Engine (64-bit)
 * Handles HEX, DEC, OCT, BIN bases, 64-bit bitwise logic, and 4-digit grouping.
 */

export class ProgrammerEngine {
  constructor() {
    this.currentBase = "HEX"; // "HEX", "DEC", "OCT", "BIN"
    this.value = 0n; // 64-bit BigInt
    this.buffer = "0";
    this.pendingOp = null; // "+", "-", "*", "/", "%", "AND", "OR", "XOR", "SHL", "SHR"
    this.storedValue = null;
    this.isNewInput = true;
  }

  setBase(newBase) {
    if (!["HEX", "DEC", "OCT", "BIN"].includes(newBase)) return;
    this.currentBase = newBase;
    this.buffer = this.formatValueForBase(this.value, newBase);
    this.isNewInput = true;
  }

  // 64-bit signed representation clamp
  static clamp64(val) {
    return BigInt.asIntN(64, BigInt(val));
  }

  // 64-bit unsigned representation
  static toUint64(val) {
    return BigInt.asUintN(64, BigInt(val));
  }

  // Format binary into 4-bit nibbles with spaces
  static formatBinaryGrouped(val, padTo64 = false) {
    const uint = ProgrammerEngine.toUint64(val);
    let binStr = uint.toString(2);
    if (padTo64) {
      binStr = binStr.padStart(64, "0");
    } else {
      // Pad to full 4-bit nibble
      const rem = binStr.length % 4;
      if (rem !== 0) {
        binStr = binStr.padStart(binStr.length + (4 - rem), "0");
      }
    }
    // Group into 4 digits from right to left
    return binStr.replace(/\B(?=(\d{4})+(?!\d))/g, " ");
  }

  // Format hex into 4-digit groups
  static formatHexGrouped(val, padTo16 = false) {
    const uint = ProgrammerEngine.toUint64(val);
    let hexStr = uint.toString(16).toUpperCase();
    if (padTo16) {
      hexStr = hexStr.padStart(16, "0");
    }
    return hexStr.replace(/\B(?=([0-9A-F]{4})+(?![0-9A-F]))/g, " ");
  }

  // Format octal
  static formatOctalGrouped(val) {
    const uint = ProgrammerEngine.toUint64(val);
    return uint.toString(8);
  }

  // Format decimal
  static formatDec(val) {
    const signed = ProgrammerEngine.clamp64(val);
    return signed.toString(10);
  }

  formatValueForBase(val, base) {
    switch (base) {
      case "HEX":
        return ProgrammerEngine.toUint64(val).toString(16).toUpperCase();
      case "DEC":
        return ProgrammerEngine.clamp64(val).toString(10);
      case "OCT":
        return ProgrammerEngine.toUint64(val).toString(8);
      case "BIN":
        return ProgrammerEngine.toUint64(val).toString(2);
      default:
        return "0";
    }
  }

  parseBuffer(str, base) {
    if (!str || str === "" || str === "-") return 0n;
    try {
      const clean = str.replace(/\s/g, "");
      switch (base) {
        case "HEX":
          return BigInt("0x" + clean);
        case "DEC":
          return BigInt(clean);
        case "OCT":
          return BigInt("0o" + clean);
        case "BIN":
          return BigInt("0b" + clean);
        default:
          return 0n;
      }
    } catch {
      return 0n;
    }
  }

  inputDigit(digit) {
    const d = digit.toUpperCase();
    if (!this.isValidDigitForBase(d, this.currentBase)) return;

    if (this.isNewInput || this.buffer === "0") {
      this.buffer = d;
      this.isNewInput = false;
    } else {
      // Prevent buffer overflow (max 64 bits = 16 hex chars)
      if (this.currentBase === "HEX" && this.buffer.length >= 16) return;
      if (this.currentBase === "BIN" && this.buffer.length >= 64) return;
      if (this.currentBase === "DEC" && this.buffer.length >= 20) return;
      this.buffer += d;
    }

    this.value = ProgrammerEngine.clamp64(this.parseBuffer(this.buffer, this.currentBase));
  }

  isValidDigitForBase(digit, base) {
    switch (base) {
      case "HEX":
        return /^[0-9A-F]$/i.test(digit);
      case "DEC":
        return /^[0-9]$/.test(digit);
      case "OCT":
        return /^[0-7]$/.test(digit);
      case "BIN":
        return /^[01]$/.test(digit);
      default:
        return false;
    }
  }

  backspace() {
    if (this.isNewInput) return;
    if (this.buffer.length <= 1) {
      this.buffer = "0";
      this.value = 0n;
      this.isNewInput = true;
    } else {
      this.buffer = this.buffer.slice(0, -1);
      this.value = ProgrammerEngine.clamp64(this.parseBuffer(this.buffer, this.currentBase));
    }
  }

  clear() {
    this.value = 0n;
    this.buffer = "0";
    this.pendingOp = null;
    this.storedValue = null;
    this.isNewInput = true;
  }

  clearEntry() {
    this.buffer = "0";
    this.value = 0n;
    this.isNewInput = true;
  }

  setOperation(op) {
    if (this.pendingOp && !this.isNewInput && this.storedValue !== null) {
      this.calculate();
    }
    this.storedValue = this.value;
    this.pendingOp = op;
    this.isNewInput = true;
  }

  calculate() {
    if (!this.pendingOp || this.storedValue === null) {
      return { success: true, value: this.value };
    }

    const a = this.storedValue;
    const b = this.value;
    let res = 0n;

    try {
      switch (this.pendingOp) {
        case "+":
          res = ProgrammerEngine.clamp64(a + b);
          break;
        case "-":
          res = ProgrammerEngine.clamp64(a - b);
          break;
        case "*":
          res = ProgrammerEngine.clamp64(a * b);
          break;
        case "/":
          if (b === 0n) throw new Error("قسمة على صفر");
          res = ProgrammerEngine.clamp64(a / b);
          break;
        case "%":
          if (b === 0n) throw new Error("قسمة على صفر");
          res = ProgrammerEngine.clamp64(a % b);
          break;
        case "AND":
          res = ProgrammerEngine.clamp64(a & b);
          break;
        case "OR":
          res = ProgrammerEngine.clamp64(a | b);
          break;
        case "XOR":
          res = ProgrammerEngine.clamp64(a ^ b);
          break;
        case "SHL":
          {
            const shift = Number(BigInt.asUintN(6, b)); // Max 63
            res = ProgrammerEngine.clamp64(a << BigInt(shift));
          }
          break;
        case "SHR":
          {
            const shift = Number(BigInt.asUintN(6, b));
            res = ProgrammerEngine.clamp64(a >> BigInt(shift));
          }
          break;
        default:
          res = b;
      }

      this.value = res;
      this.buffer = this.formatValueForBase(res, this.currentBase);
      this.storedValue = null;
      this.pendingOp = null;
      this.isNewInput = true;
      return { success: true, value: res };
    } catch (err) {
      return { success: false, error: err.message || "خارج المدى المسموح" };
    }
  }

  not() {
    this.value = ProgrammerEngine.clamp64(~this.value);
    this.buffer = this.formatValueForBase(this.value, this.currentBase);
    this.isNewInput = true;
  }

  toggleSign() {
    this.value = ProgrammerEngine.clamp64(-this.value);
    this.buffer = this.formatValueForBase(this.value, this.currentBase);
  }

  // Returns all four representations simultaneously
  getAllBases() {
    return {
      HEX: ProgrammerEngine.formatHexGrouped(this.value),
      DEC: ProgrammerEngine.formatDec(this.value),
      OCT: ProgrammerEngine.formatOctalGrouped(this.value),
      BIN: ProgrammerEngine.formatBinaryGrouped(this.value)
    };
  }
}
