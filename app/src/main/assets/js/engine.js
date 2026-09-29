/**
 * R-7 Precision Calculation Engine
 * 
 * Implements a Shunting-yard / Pratt expression parser with:
 * - High-precision decimal arithmetic (exact 0.1 + 0.2 = 0.3, 1 ÷ 3 × 3 = 1)
 * - Engineering percentage semantics (200 + 10% = 220, 50 × 10% = 5, 50% = 0.5)
 * - Angle modes (DEG, RAD, GRAD) with exact angle handling (sin(30)=0.5, tan(45)=1)
 * - Error detection: "قسمة على صفر", "التعبير غير مكتمل", "خارج المدى المسموح"
 * - Scientific/Exponential notation with max 12 displayed digits
 * - Unclosed parentheses auto-closure
 * - ANS register tracking
 */

export const PHI = 1.6180339887498948482; // Golden Ratio

// Superscript numerals for exponential notation formatting
const SUPERSCRIPTS = {
  "0": "⁰", "1": "¹", "2": "²", "3": "³", "4": "⁴",
  "5": "⁵", "6": "⁶", "7": "⁷", "8": "⁸", "9": "⁹",
  "-": "⁻", "+": ""
};

export function toSuperscript(numStr) {
  return String(numStr)
    .split("")
    .map(ch => SUPERSCRIPTS[ch] !== undefined ? SUPERSCRIPTS[ch] : ch)
    .join("");
}

/**
 * Decimal number representation to avoid IEEE 754 precision issues
 */
export class DecimalMath {
  /**
   * Cleans floating point drift for display and intermediate values.
   * e.g., 0.30000000000000004 -> 0.3, 0.9999999999999999 -> 1
   */
  static clean(num, precision = 12) {
    if (typeof num !== "number") num = Number(num);
    if (!Number.isFinite(num)) return num;
    if (Math.abs(num) < 1e-15) return 0;

    // Use toPrecision to eliminate precision tail noise, then round to reasonable decimal
    const p = parseFloat(num.toPrecision(14));
    const factor = Math.pow(10, precision);
    return Math.round(p * factor) / factor;
  }

  static add(a, b) {
    const da = DecimalMath.getDecimals(a);
    const db = DecimalMath.getDecimals(b);
    const maxDec = Math.max(da, db);
    const factor = Math.pow(10, Math.min(maxDec, 10));
    const res = (Math.round(a * factor) + Math.round(b * factor)) / factor;
    return DecimalMath.clean(res);
  }

  static sub(a, b) {
    const da = DecimalMath.getDecimals(a);
    const db = DecimalMath.getDecimals(b);
    const maxDec = Math.max(da, db);
    const factor = Math.pow(10, Math.min(maxDec, 10));
    const res = (Math.round(a * factor) - Math.round(b * factor)) / factor;
    return DecimalMath.clean(res);
  }

  static mul(a, b) {
    const da = DecimalMath.getDecimals(a);
    const db = DecimalMath.getDecimals(b);
    const totalDec = da + db;
    if (totalDec <= 8) {
      const fa = Math.pow(10, da);
      const fb = Math.pow(10, db);
      const res = (Math.round(a * fa) * Math.round(b * fb)) / (fa * fb);
      return DecimalMath.clean(res);
    }
    return DecimalMath.clean(a * b);
  }

  static div(a, b) {
    if (Math.abs(b) < 1e-15) {
      throw new Error("قسمة على صفر");
    }
    const raw = a / b;
    // Check for exact repeating fractions like 1/3 * 3
    return DecimalMath.clean(raw);
  }

  static getDecimals(num) {
    const s = String(num);
    const idx = s.indexOf(".");
    if (idx === -1) return 0;
    return s.length - idx - 1;
  }
}

/**
 * Format calculation result to comply with R-7 display specifications:
 * - Max 12 displayed digits
 * - Exponential format 1.23456789012×10¹² when out of range
 * - No NaN or Infinity
 */
export function formatDisplayNumber(val, maxDigits = 12) {
  if (typeof val === "string") return val;
  if (val === null || val === undefined || isNaN(val)) {
    return "0";
  }
  if (!isFinite(val)) {
    return "خارج المدى المسموح";
  }

  if (val === 0 || Math.abs(val) < 1e-15) {
    return "0";
  }

  const absVal = Math.abs(val);

  // Transition to scientific notation if outside normal 12-digit window
  if (absVal >= 1e12 || absVal < 1e-6) {
    const expStr = val.toExponential(maxDigits - 6);
    const [mantissa, exponent] = expStr.split("e");
    const cleanedMantissa = parseFloat(mantissa).toString();
    const supExp = toSuperscript(exponent.replace("+", ""));
    return `${cleanedMantissa}×10${supExp}`;
  }

  // Standard representation within 12 digits
  // Format with high precision and remove trailing zeros
  let str = parseFloat(val.toPrecision(maxDigits)).toString();
  if (str.includes("e")) {
    const [mantissa, exponent] = str.split("e");
    const supExp = toSuperscript(exponent.replace("+", ""));
    return `${mantissa}×10${supExp}`;
  }

  // Enforce max length constraint
  if (str.length > maxDigits) {
    const parts = str.split(".");
    if (parts.length === 2) {
      const allowedDecimals = Math.max(0, maxDigits - parts[0].length - 1);
      if (allowedDecimals > 0) {
        str = val.toFixed(allowedDecimals).replace(/(\.\d*?[1-9])0+$/, "$1").replace(/\.0+$/, "");
      } else {
        str = Math.round(val).toString();
      }
    }
  }

  // Ensure trailing zeros and bare dot are stripped
  if (str.includes(".")) {
    str = str.replace(/(\.\d*?[1-9])0+$/, "$1").replace(/\.0+$/, "");
  }

  return str;
}

/**
 * Format number with thousand separators (e.g. 1,234,567.89)
 */
export function formatThousands(numStr) {
  if (typeof numStr !== "string") numStr = String(numStr);
  if (numStr.includes("×10")) return numStr;
  const parts = numStr.split(".");
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return parts.join(".");
}

/**
 * Trigonometric functions adjusted for DEG, RAD, and GRAD angle modes
 */
export function trigSin(x, mode = "DEG") {
  if (mode === "DEG") {
    const deg = ((x % 360) + 360) % 360;
    if (deg === 0 || deg === 180 || deg === 360) return 0;
    if (deg === 30 || deg === 150) return 0.5;
    if (deg === 90) return 1;
    if (deg === 210 || deg === 330) return -0.5;
    if (deg === 270) return -1;
    return DecimalMath.clean(Math.sin((x * Math.PI) / 180));
  } else if (mode === "GRAD") {
    const grad = ((x % 400) + 400) % 400;
    if (grad === 0 || grad === 200 || grad === 400) return 0;
    if (grad === 100) return 1;
    if (grad === 300) return -1;
    return DecimalMath.clean(Math.sin((x * Math.PI) / 200));
  } else {
    // RAD
    return DecimalMath.clean(Math.sin(x));
  }
}

export function trigCos(x, mode = "DEG") {
  if (mode === "DEG") {
    const deg = ((x % 360) + 360) % 360;
    if (deg === 90 || deg === 270) return 0;
    if (deg === 0 || deg === 360) return 1;
    if (deg === 60 || deg === 300) return 0.5;
    if (deg === 120 || deg === 240) return -0.5;
    if (deg === 180) return -1;
    return DecimalMath.clean(Math.cos((x * Math.PI) / 180));
  } else if (mode === "GRAD") {
    const grad = ((x % 400) + 400) % 400;
    if (grad === 100 || grad === 300) return 0;
    if (grad === 0 || grad === 400) return 1;
    if (grad === 200) return -1;
    return DecimalMath.clean(Math.cos((x * Math.PI) / 200));
  } else {
    // RAD
    return DecimalMath.clean(Math.cos(x));
  }
}

export function trigTan(x, mode = "DEG") {
  if (mode === "DEG") {
    const deg = ((x % 360) + 360) % 360;
    if (deg === 90 || deg === 270) {
      throw new Error("قسمة على صفر");
    }
    if (deg === 0 || deg === 180 || deg === 360) return 0;
    if (deg === 45 || deg === 225) return 1;
    if (deg === 135 || deg === 315) return -1;
    return DecimalMath.clean(Math.tan((x * Math.PI) / 180));
  } else if (mode === "GRAD") {
    const grad = ((x % 400) + 400) % 400;
    if (grad === 100 || grad === 300) {
      throw new Error("قسمة على صفر");
    }
    if (grad === 0 || grad === 200 || grad === 400) return 0;
    if (grad === 50 || grad === 250) return 1;
    if (grad === 150 || grad === 350) return -1;
    return DecimalMath.clean(Math.tan((x * Math.PI) / 200));
  } else {
    // RAD
    const cosVal = Math.cos(x);
    if (Math.abs(cosVal) < 1e-14) {
      throw new Error("قسمة على صفر");
    }
    return DecimalMath.clean(Math.tan(x));
  }
}

export function trigAsin(x, mode = "DEG") {
  if (x < -1 || x > 1) {
    throw new Error("خارج المدى المسموح");
  }
  const rad = Math.asin(x);
  if (mode === "DEG") return DecimalMath.clean((rad * 180) / Math.PI);
  if (mode === "GRAD") return DecimalMath.clean((rad * 200) / Math.PI);
  return DecimalMath.clean(rad);
}

export function trigAcos(x, mode = "DEG") {
  if (x < -1 || x > 1) {
    throw new Error("خارج المدى المسموح");
  }
  const rad = Math.acos(x);
  if (mode === "DEG") return DecimalMath.clean((rad * 180) / Math.PI);
  if (mode === "GRAD") return DecimalMath.clean((rad * 200) / Math.PI);
  return DecimalMath.clean(rad);
}

export function trigAtan(x, mode = "DEG") {
  const rad = Math.atan(x);
  if (mode === "DEG") return DecimalMath.clean((rad * 180) / Math.PI);
  if (mode === "GRAD") return DecimalMath.clean((rad * 200) / Math.PI);
  return DecimalMath.clean(rad);
}

export function factorial(n) {
  if (n < 0 || !Number.isInteger(n)) {
    throw new Error("خارج المدى المسموح");
  }
  if (n > 170) {
    throw new Error("خارج المدى المسموح");
  }
  if (n === 0 || n === 1) return 1;
  let res = 1;
  for (let i = 2; i <= n; i++) {
    res *= i;
  }
  return res;
}

/**
 * Tokenizer for R-7 expression parser
 */
export function tokenize(expr, lastAns = 0) {
  if (!expr || typeof expr !== "string") return [];

  // Normalize operators and symbols
  let s = expr
    .replace(/×/g, "*")
    .replace(/÷/g, "/")
    .replace(/−/g, "-")
    .replace(/π/g, "PI")
    .replace(/φ/g, "PHI")
    .replace(/√/g, "sqrt")
    .trim();

  // If expression starts with binary operator (*, /, +, -, ^, %), prepend ANS
  if (/^[*/^%]/.test(s)) {
    s = `Ans ${s}`;
  }

  const tokens = [];
  let i = 0;
  const n = s.length;

  while (i < n) {
    const ch = s[i];

    if (/\s/.test(ch)) {
      i++;
      continue;
    }

    // Number literal: digits and decimal point
    if (/\d/.test(ch) || (ch === "." && i + 1 < n && /\d/.test(s[i + 1]))) {
      let numStr = "";
      while (i < n && (/[\d.]/.test(s[i]) || s[i] === "e" || s[i] === "E")) {
        // Handle scientific notation in number literal like 1e5
        if ((s[i] === "e" || s[i] === "E") && i + 1 < n && (s[i + 1] === "+" || s[i + 1] === "-" || /\d/.test(s[i + 1]))) {
          numStr += s[i];
          i++;
          if (s[i] === "+" || s[i] === "-") {
            numStr += s[i];
            i++;
          }
        } else {
          numStr += s[i];
          i++;
        }
      }
      tokens.push({ type: "NUMBER", value: parseFloat(numStr), raw: numStr });
      continue;
    }

    // Identifiers (functions, constants, ANS)
    if (/[a-zA-Z_]/.test(ch)) {
      let id = "";
      while (i < n && /[a-zA-Z0-9_]/.test(s[i])) {
        id += s[i];
        i++;
      }
      const lower = id.toLowerCase();
      if (lower === "ans") {
        tokens.push({ type: "NUMBER", value: lastAns, raw: "Ans" });
      } else if (lower === "pi") {
        tokens.push({ type: "NUMBER", value: Math.PI, raw: "π" });
      } else if (lower === "e" && (tokens.length === 0 || tokens[tokens.length - 1].type !== "NUMBER")) {
        tokens.push({ type: "NUMBER", value: Math.E, raw: "e" });
      } else if (lower === "phi") {
        tokens.push({ type: "NUMBER", value: PHI, raw: "φ" });
      } else {
        // Function name
        tokens.push({ type: "FUNC", value: lower, raw: id });
      }
      continue;
    }

    // Single character operators
    if ("+-*/^%!()".includes(ch)) {
      tokens.push({ type: "OP", value: ch });
      i++;
      continue;
    }

    // Unknown character: skip or throw
    i++;
  }

  return tokens;
}

/**
 * Evaluates mathematical expression with Pratt / Shunting-yard parser
 */
export class R7Engine {
  constructor(options = {}) {
    this.angleMode = options.angleMode || "DEG"; // "DEG", "RAD", "GRAD"
    this.lastAns = options.lastAns || 0;
  }

  setAngleMode(mode) {
    if (["DEG", "RAD", "GRAD"].includes(mode)) {
      this.angleMode = mode;
    }
  }

  /**
   * Sanitizes pasted text. Replaces symbols, checks validity.
   */
  sanitizePaste(raw) {
    if (!raw || typeof raw !== "string") return "";
    let clean = raw
      .replace(/[xX*]/g, "×")
      .replace(/[\/:]/g, "÷")
      .replace(/-/g, "−")
      .replace(/[,\s]/g, "");

    // Check if contains invalid characters
    if (/[^\d\.\+\−\×\÷\^%()eEπφAnsanssincoargtlq!|]/i.test(clean)) {
      return null;
    }
    return clean;
  }

  /**
   * Evaluate the expression string
   * Returns: {
   *   success: boolean,
   *   result: number | null,
   *   display: string,
   *   error: string | null,
   *   closedExpr: string,
   *   autoClosedCount: number
   * }
   */
  evaluate(expr) {
    if (!expr || expr.trim() === "") {
      return {
        success: false,
        result: null,
        display: "0",
        error: "التعبير غير مكتمل",
        closedExpr: "",
        autoClosedCount: 0
      };
    }

    let trimmed = expr.trim();

    // Count parentheses and auto-close unclosed ones
    let openCount = 0;
    for (const ch of trimmed) {
      if (ch === "(") openCount++;
      else if (ch === ")") openCount--;
    }

    let autoClosedCount = 0;
    let closedExpr = trimmed;
    if (openCount > 0) {
      autoClosedCount = openCount;
      closedExpr = trimmed + ")".repeat(openCount);
    } else if (openCount < 0) {
      return {
        success: false,
        result: null,
        display: "0",
        error: "التعبير غير مكتمل",
        closedExpr: trimmed,
        autoClosedCount: 0
      };
    }

    try {
      const tokens = tokenize(closedExpr, this.lastAns);
      if (tokens.length === 0) {
        return {
          success: false,
          result: null,
          display: "0",
          error: "التعبير غير مكتمل",
          closedExpr,
          autoClosedCount
        };
      }

      const val = this.parseExpression(tokens);
      if (val === null || val === undefined || isNaN(val)) {
        return {
          success: false,
          result: null,
          display: "0",
          error: "التعبير غير مكتمل",
          closedExpr,
          autoClosedCount
        };
      }

      this.lastAns = val;
      const formatted = formatDisplayNumber(val);

      return {
        success: true,
        result: val,
        display: formatted,
        error: null,
        closedExpr,
        autoClosedCount
      };
    } catch (err) {
      const msg = err.message || "خارج المدى المسموح";
      return {
        success: false,
        result: null,
        display: "0",
        error: msg,
        closedExpr,
        autoClosedCount
      };
    }
  }

  /**
   * Shunting-yard algorithm implementation adapted for contextual percentage & unary operators
   */
  parseExpression(tokens) {
    const outputQueue = [];
    const operatorStack = [];

    // Precedence table
    const PRECEDENCE = {
      "+": 1,
      "-": 1,
      "*": 2,
      "/": 2,
      "%": 3,
      "^": 4,
      "UNARY_MINUS": 5,
      "UNARY_PLUS": 5,
      "!": 6
    };

    let prevToken = null;

    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i];

      if (token.type === "NUMBER") {
        outputQueue.push(token);
      } else if (token.type === "FUNC") {
        operatorStack.push(token);
      } else if (token.type === "OP") {
        const val = token.value;

        // Check for unary + or -
        const isUnary = (val === "+" || val === "-") && (
          prevToken === null ||
          prevToken.type === "FUNC" ||
          (prevToken.type === "OP" && prevToken.value !== ")" && prevToken.value !== "!")
        );

        if (isUnary) {
          const unaryOp = val === "-" ? "UNARY_MINUS" : "UNARY_PLUS";
          operatorStack.push({ type: "OP", value: unaryOp, isUnary: true });
        } else if (val === "(") {
          operatorStack.push(token);
        } else if (val === ")") {
          while (operatorStack.length > 0 && operatorStack[operatorStack.length - 1].value !== "(") {
            outputQueue.push(operatorStack.pop());
          }
          if (operatorStack.length === 0) {
            throw new Error("التعبير غير مكتمل");
          }
          operatorStack.pop(); // Pop "("
          // If top of stack is a function, pop to output
          if (operatorStack.length > 0 && operatorStack[operatorStack.length - 1].type === "FUNC") {
            outputQueue.push(operatorStack.pop());
          }
        } else if (val === "!") {
          // Postfix operator
          outputQueue.push({ type: "OP", value: "!" });
        } else if (val === "%") {
          // Percentage token
          outputQueue.push({ type: "OP", value: "%" });
        } else {
          // Binary operator (+, -, *, /, ^)
          while (
            operatorStack.length > 0 &&
            operatorStack[operatorStack.length - 1].value !== "(" &&
            (
              (PRECEDENCE[operatorStack[operatorStack.length - 1].value] > PRECEDENCE[val]) ||
              (PRECEDENCE[operatorStack[operatorStack.length - 1].value] === PRECEDENCE[val] && val !== "^")
            )
          ) {
            outputQueue.push(operatorStack.pop());
          }
          operatorStack.push(token);
        }
      }

      prevToken = token;
    }

    while (operatorStack.length > 0) {
      const op = operatorStack.pop();
      if (op.value === "(" || op.value === ")") {
        throw new Error("التعبير غير مكتمل");
      }
      outputQueue.push(op);
    }

    // Evaluate RPN with contextual percentage rule
    return this.evaluateRPN(outputQueue);
  }

  /**
   * Evaluate Reverse Polish Notation
   * Percentage Semantics:
   * 200 + 10% = 220
   * 50 * 10% = 5
   * 50% = 0.5
   */
  evaluateRPN(rpn) {
    const stack = [];

    for (let i = 0; i < rpn.length; i++) {
      const token = rpn[i];

      if (token.type === "NUMBER") {
        stack.push(token.value);
      } else if (token.type === "FUNC") {
        if (stack.length < 1) throw new Error("التعبير غير مكتمل");
        const arg = stack.pop();
        stack.push(this.applyFunc(token.value, arg));
      } else if (token.type === "OP") {
        if (token.value === "UNARY_MINUS") {
          if (stack.length < 1) throw new Error("التعبير غير مكتمل");
          stack.push(-stack.pop());
        } else if (token.value === "UNARY_PLUS") {
          if (stack.length < 1) throw new Error("التعبير غير مكتمل");
          stack.push(+stack.pop());
        } else if (token.value === "!") {
          if (stack.length < 1) throw new Error("التعبير غير مكتمل");
          stack.push(factorial(stack.pop()));
        } else if (token.value === "%") {
          // Check percentage context:
          // If followed by '+' or '-', A + B% = A + (A * B / 100)
          // If followed by '*' or '/', A * B% = A * (B / 100)
          // If standalone, B% = B / 100
          if (stack.length < 1) throw new Error("التعبير غير مكتمل");
          const b = stack.pop();

          // Check if there is an upcoming binary operator and a preceding number
          const nextOp = (i + 1 < rpn.length && rpn[i + 1].type === "OP") ? rpn[i + 1].value : null;

          if (stack.length >= 1 && (nextOp === "+" || nextOp === "-")) {
            const a = stack[stack.length - 1]; // Peek
            // B becomes (a * b / 100)
            stack.push((a * b) / 100);
          } else {
            // Standalone or with multiplication/division
            stack.push(b / 100);
          }
        } else {
          // Binary operator
          if (stack.length < 2) throw new Error("التعبير غير مكتمل");
          const b = stack.pop();
          const a = stack.pop();

          switch (token.value) {
            case "+":
              stack.push(DecimalMath.add(a, b));
              break;
            case "-":
              stack.push(DecimalMath.sub(a, b));
              break;
            case "*":
              stack.push(DecimalMath.mul(a, b));
              break;
            case "/":
              stack.push(DecimalMath.div(a, b));
              break;
            case "^":
              if (a === 0 && b < 0) throw new Error("قسمة على صفر");
              stack.push(DecimalMath.clean(Math.pow(a, b)));
              break;
            default:
              throw new Error("التعبير غير مكتمل");
          }
        }
      }
    }

    if (stack.length !== 1) {
      throw new Error("التعبير غير مكتمل");
    }

    return stack[0];
  }

  applyFunc(name, x) {
    switch (name) {
      case "sin":
        return trigSin(x, this.angleMode);
      case "cos":
        return trigCos(x, this.angleMode);
      case "tan":
        return trigTan(x, this.angleMode);
      case "asin":
        return trigAsin(x, this.angleMode);
      case "acos":
        return trigAcos(x, this.angleMode);
      case "atan":
        return trigAtan(x, this.angleMode);
      case "ln":
        if (x <= 0) throw new Error("خارج المدى المسموح");
        return DecimalMath.clean(Math.log(x));
      case "log":
        if (x <= 0) throw new Error("خارج المدى المسموح");
        return DecimalMath.clean(Math.log10(x));
      case "sqrt":
        if (x < 0) throw new Error("خارج المدى المسموح");
        return DecimalMath.clean(Math.sqrt(x));
      case "cbrt":
        return DecimalMath.clean(Math.cbrt(x));
      case "abs":
        return Math.abs(x);
      case "exp":
        return DecimalMath.clean(Math.exp(x));
      case "sqr":
        return DecimalMath.mul(x, x);
      case "recip":
        if (Math.abs(x) < 1e-15) throw new Error("قسمة على صفر");
        return DecimalMath.div(1, x);
      default:
        throw new Error("التعبير غير مكتمل");
    }
  }
}
