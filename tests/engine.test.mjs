/**
 * R-7 Engine Test Suite
 * Validates §12 acceptance criteria and arithmetic edge cases
 */

import { R7Engine, DecimalMath, formatDisplayNumber } from "../js/engine.js";

const engine = new R7Engine({ angleMode: "DEG" });
let passed = 0;
let failed = 0;

function assert(description, actual, expected) {
  const match = typeof expected === "number"
    ? Math.abs(actual - expected) < 1e-10
    : actual === expected;

  if (match) {
    passed++;
    console.log(`  ✓ ${description}`);
  } else {
    failed++;
    console.error(`  ✗ ${description}`);
    console.error(`    Expected: ${expected}`);
    console.error(`    Actual:   ${actual}`);
  }
}

console.log("=== R-7 Engine Verification Suite ===");

// 1. Exact Decimal Arithmetic
console.log("\n[1] Decimal Precision:");
const t1 = engine.evaluate("0.1 + 0.2");
assert("0.1 + 0.2 === 0.3", t1.result, 0.3);
assert("Display format 0.1 + 0.2 === '0.3'", t1.display, "0.3");

const t2 = engine.evaluate("1 / 3 * 3");
assert("1 ÷ 3 × 3 === 1", t2.result, 1);
assert("Display format 1 ÷ 3 × 3 === '1'", t2.display, "1");

const t3 = engine.evaluate("0.3 - 0.2");
assert("0.3 - 0.2 === 0.1", t3.result, 0.1);

// 2. Percentage Semantics
console.log("\n[2] Percentage Semantics:");
const p1 = engine.evaluate("200 + 10%");
assert("200 + 10% === 220", p1.result, 220);

const p2 = engine.evaluate("200 - 10%");
assert("200 - 10% === 180", p2.result, 180);

const p3 = engine.evaluate("50 * 10%");
assert("50 × 10% === 5", p3.result, 5);

const p4 = engine.evaluate("50%");
assert("50% alone === 0.5", p4.result, 0.5);

// 3. Parentheses Auto-closure
console.log("\n[3] Auto-close Parentheses:");
const ac1 = engine.evaluate("5 * (3 + 2");
assert("5 × (3 + 2 yields 25", ac1.result, 25);
assert("Auto-closed count === 1", ac1.autoClosedCount, 1);
assert("Closed expression === '5 * (3 + 2)'", ac1.closedExpr, "5 * (3 + 2)");

const ac2 = engine.evaluate("((2 + 3) * (4 + 1");
assert("((2 + 3) * (4 + 1 yields 25", ac2.result, 25);
assert("Auto-closed count === 2", ac2.autoClosedCount, 2);

// 4. Trigonometric & Exact Angles in DEG
console.log("\n[4] Trigonometry (DEG):");
const sin30 = engine.evaluate("sin(30)");
assert("sin(30) in DEG === 0.5", sin30.result, 0.5);

const cos60 = engine.evaluate("cos(60)");
assert("cos(60) in DEG === 0.5", cos60.result, 0.5);

const tan45 = engine.evaluate("tan(45)");
assert("tan(45) in DEG === 1", tan45.result, 1);

const sin90 = engine.evaluate("sin(90)");
assert("sin(90) in DEG === 1", sin90.result, 1);

const cos90 = engine.evaluate("cos(90)");
assert("cos(90) in DEG === 0", cos90.result, 0);

// 5. Division by Zero
console.log("\n[5] Division by Zero:");
const divZero = engine.evaluate("10 / 0");
assert("Division by zero returns error", divZero.success, false);
assert("Division by zero message === 'قسمة على صفر'", divZero.error, "قسمة على صفر");
assert("Does not output NaN or Infinity", !divZero.display.includes("NaN") && !divZero.display.includes("Infinity"), true);

// 6. Functions & Powers
console.log("\n[6] Math Functions & Powers:");
const sqrtVal = engine.evaluate("sqrt(144)");
assert("sqrt(144) === 12", sqrtVal.result, 12);

const powVal = engine.evaluate("2 ^ 10");
assert("2 ^ 10 === 1024", powVal.result, 1024);

const factVal = engine.evaluate("5!");
assert("5! === 120", factVal.result, 120);

const factZero = engine.evaluate("0!");
assert("0! === 1", factZero.result, 1);

const logVal = engine.evaluate("log(100)");
assert("log(100) === 2", logVal.result, 2);

const lnVal = engine.evaluate("ln(e)");
assert("ln(e) === 1", lnVal.result, 1);

// 7. Unary Negation & Precedence
console.log("\n[7] Unary Negation & Precedence:");
const neg1 = engine.evaluate("-(3 + 4)");
assert("-(3 + 4) === -7", neg1.result, -7);

const neg2 = engine.evaluate("5 + -3");
assert("5 + -3 === 2", neg2.result, 2);

const prec = engine.evaluate("2 + 3 * 4");
assert("2 + 3 × 4 === 14", prec.result, 14);

// 8. ANS Chaining
console.log("\n[8] ANS Register Chaining:");
engine.evaluate("25");
const ansChain = engine.evaluate("* 2");
assert("Ans chaining '* 2' yields 50", ansChain.result, 50);

// 9. Display Formatting & Exponential transition
console.log("\n[9] Display Formatting & Exponential:");
const largeNum = formatDisplayNumber(1.23456789012e12);
assert("Large number transitions to exponential with superscripts", largeNum.includes("×10"), true);

const smallNum = formatDisplayNumber(0.000000123);
assert("Small number transitions to exponential", smallNum.includes("×10"), true);

// 10. Programmer Mode (64-bit & Base conversions)
console.log("\n[10] Programmer Mode:");
const { ProgrammerEngine } = await import("../js/programmer.js");
const pProg = new ProgrammerEngine();
pProg.setBase("HEX");
pProg.inputDigit("F");
pProg.inputDigit("F");
pProg.setOperation("+");
pProg.inputDigit("1");
pProg.calculate();
assert("Programmer HEX: FF + 1 === 100", pProg.buffer, "100");
const bases = pProg.getAllBases();
assert("Programmer bases HEX === '100'", bases.HEX, "100");
assert("Programmer bases DEC === '256'", bases.DEC, "256");
assert("Programmer bases OCT === '400'", bases.OCT, "400");
assert("Programmer bases BIN === '0001 0000 0000'", bases.BIN, "0001 0000 0000");

// Bitwise operations
pProg.clear();
pProg.setBase("DEC");
pProg.inputDigit("1");
pProg.inputDigit("2");
pProg.setOperation("AND");
pProg.inputDigit("1");
pProg.inputDigit("0");
pProg.calculate();
assert("Programmer 12 AND 10 === 8", pProg.buffer, "8");

// 11. Unit Converter
console.log("\n[11] Unit Converter:");
const { UnitConverter } = await import("../js/converter.js");
const converter = new UnitConverter({ precision: 1 });
const tempRes = converter.convert("temp", 100, "F", "C", 1);
assert("Unit Converter 100°F -> 37.8°C", tempRes, 37.8);

console.log(`\n========================================`);
console.log(`Results: ${passed} passed, ${failed} failed`);
console.log(`========================================`);

if (failed > 0) {
  process.exit(1);
} else {
  process.exit(0);
}
