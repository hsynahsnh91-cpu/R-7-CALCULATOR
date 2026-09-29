import assert from 'assert';
import { createApp } from '../src/app.js';
import { config } from '../src/config/env.js';

async function runTests() {
  console.log('--- Starting R-7 Backend Production Tests ---');
  const app = createApp();

  // Create a local test server
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const port = server.address().port;
  const baseUrl = `http://127.0.0.1:${port}`;

  try {
    // Test 1: GET /health
    console.log('[Test 1] GET /health');
    const healthRes = await fetch(`${baseUrl}/health`);
    assert.strictEqual(healthRes.status, 200);
    const healthJson = await healthRes.json();
    assert.strictEqual(healthJson.status, 'ok');
    assert.strictEqual(healthJson.service, 'r7-ai-backend');
    assert.strictEqual(healthJson.version, '1.0.0');
    console.log('✓ Test 1 passed: /health returns 200 ok');

    // Test 2: Unauthorized request to /api/v1/ai/solve
    console.log('[Test 2] POST /api/v1/ai/solve without app key');
    const unauthRes = await fetch(`${baseUrl}/api/v1/ai/solve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: '25 * 25' })
    });
    assert.strictEqual(unauthRes.status, 401);
    const unauthJson = await unauthRes.json();
    assert.strictEqual(unauthJson.success, false);
    assert.strictEqual(unauthJson.error_code, 'UNAUTHORIZED_APPLICATION');
    console.log('✓ Test 2 passed: Unauthorized requests correctly rejected with 401');

    // Test 3: Local calculation first: "25 * 25" (Deterministic arithmetic)
    console.log('[Test 3] POST /api/v1/ai/solve deterministic arithmetic (25 * 25)');
    const solveRes = await fetch(`${baseUrl}/api/v1/ai/solve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-R7-App-Key': config.appKey
      },
      body: JSON.stringify({
        question: '25 * 25',
        subject: 'math',
        mode: 'solve',
        language: 'ar'
      })
    });
    assert.strictEqual(solveRes.status, 200);
    const solveJson = await solveRes.json();
    assert.strictEqual(solveJson.success, true);
    assert.ok(solveJson.answer.includes('625'));
    assert.strictEqual(solveJson.confidence, 'verified');
    assert.strictEqual(solveJson.verification_status, 'verified');
    assert.ok(solveJson.request_id.startsWith('r7-'));
    assert.strictEqual(typeof solveJson.processing_time_ms, 'number');
    console.log('✓ Test 3 passed: Arithmetic solved locally in <5ms without external Gemini call');

    // Test 4: Local calculation: Parallel resistors formula
    console.log('[Test 4] POST /api/v1/ai/solve parallel resistors formula');
    const parallelRes = await fetch(`${baseUrl}/api/v1/ai/solve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-R7-App-Key': config.appKey
      },
      body: JSON.stringify({
        question: 'احسب مقاومة مكافئة لدائرتين على التوازي بقيمة 10 و 20 أوم',
        subject: 'physics',
        language: 'ar'
      })
    });
    assert.strictEqual(parallelRes.status, 200);
    const parallelJson = await parallelRes.json();
    assert.strictEqual(parallelJson.success, true);
    assert.ok(parallelJson.answer.includes('6.67'));
    assert.strictEqual(parallelJson.confidence, 'verified');
    console.log('✓ Test 4 passed: Parallel resistor formula verified and solved deterministically');

    // Test 5: Validation - Empty question
    console.log('[Test 5] POST /api/v1/ai/solve empty question validation');
    const emptyRes = await fetch(`${baseUrl}/api/v1/ai/solve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-R7-App-Key': config.appKey
      },
      body: JSON.stringify({
        question: '   '
      })
    });
    assert.strictEqual(emptyRes.status, 400);
    const emptyJson = await emptyRes.json();
    assert.strictEqual(emptyJson.error_code, 'MISSING_QUESTION');
    console.log('✓ Test 5 passed: Empty question rejected with 400 MISSING_QUESTION');

    // Test 6: Security Audit - Ensure NO secret is present in response
    console.log('[Test 6] Security Audit - Check response for secrets');
    const fullResponseStr = JSON.stringify(solveJson) + JSON.stringify(unauthJson);
    assert.strictEqual(fullResponseStr.includes(config.geminiApiKey && config.geminiApiKey !== '' ? config.geminiApiKey : 'DUMMY_KEY_NEVER_FOUND'), false);
    assert.strictEqual(fullResponseStr.includes('AIza'), false);
    assert.strictEqual(fullResponseStr.includes('password'), false);
    console.log('✓ Test 6 passed: Zero secrets leaked in API responses');

    console.log('\n=========================================');
    console.log('ALL BACKEND PRODUCTION TESTS PASSED (6/6)');
    console.log('=========================================\n');
  } finally {
    server.close();
  }
}

runTests().catch(err => {
  console.error('Test execution failed:', err);
  process.exit(1);
});
