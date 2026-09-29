export function trySolveLocally(question, language = 'ar') {
  const q = question.trim().toLowerCase();

  // 1. Direct arithmetic detection (e.g. "25 × 25", "25 * 25", "100 + 50 / 2", "sqrt(16)")
  // Clean Arabic math symbols and words
  let sanitized = q
    .replace(/احسب\s*:/g, '')
    .replace(/احسب/g, '')
    .replace(/calculate/g, '')
    .replace(/what is/g, '')
    .replace(/ما هو/g, '')
    .replace(/كم يساوي/g, '')
    .replace(/ناتج/g, '')
    .replace(/×/g, '*')
    .replace(/÷/g, '/')
    .replace(/\^/g, '**')
    .trim();

  // Parallel resistors detection (e.g., "10 و 20 أوم على التوازي" or "10 and 20 ohms in parallel")
  if (q.includes('توازي') || q.includes('parallel')) {
    const numbers = q.match(/\b\d+(?:\.\d+)?\b/g);
    if (numbers && numbers.length >= 2) {
      const r1 = parseFloat(numbers[0]);
      const r2 = parseFloat(numbers[1]);
      if (r1 > 0 && r2 > 0) {
        const req = (r1 * r2) / (r1 + r2);
        const rounded = Math.round(req * 100) / 100;
        const answer = language === 'ar'
          ? `حساب المقاومة المكافئة على التوازي:\nالقانون: Req = (R1 × R2) / (R1 + R2)\nReq = (${r1} × ${r2}) / (${r1} + ${r2}) = ${r1 * r2} / ${r1 + r2}\nالناتج النهائي = ${rounded} Ω (أوم).`
          : `Parallel Resistance Calculation:\nFormula: Req = (R1 × R2) / (R1 + R2)\nReq = (${r1} × ${r2}) / (${r1} + ${r2}) = ${r1 * r2} / ${r1 + r2}\nFinal Result = ${rounded} Ω.`;
        return {
          solvedLocally: true,
          answer,
          confidence: 'verified',
          verification_status: 'verified'
        };
      }
    }
  }

  // Pure arithmetic expression match: digits, +, -, *, /, %, (, ), ., spaces
  if (/^[\d\s+\-*/%().]+$/.test(sanitized)) {
    try {
      // Evaluate safely without arbitrary code execution
      const result = safeEvaluateArithmetic(sanitized);
      if (result !== null && !isNaN(result) && isFinite(result)) {
        const answer = language === 'ar'
          ? `الناتج الحسابي:\n${question.trim()} = ${result}`
          : `Calculation Result:\n${question.trim()} = ${result}`;
        return {
          solvedLocally: true,
          answer,
          confidence: 'verified',
          verification_status: 'verified'
        };
      }
    } catch (_) {}
  }

  return null;
}

function safeEvaluateArithmetic(expr) {
  // Disallow any characters other than digits, operators, parentheses, and decimals
  if (!/^[\d\s+\-*/().]+$/.test(expr)) return null;
  // Function constructor restricted to pure arithmetic evaluation
  const fn = new Function(`"use strict"; return (${expr});`);
  return fn();
}
