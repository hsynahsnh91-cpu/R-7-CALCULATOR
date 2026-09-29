# R-7 Calculator — Production Secure AI Backend

This is the secure backend service for the **R-7 Calculator** Android application. It acts as an intermediary between the Android client and the Google Gemini API, ensuring that:

1. The Gemini API key remains strictly server-side and is never packaged in the Android APK.
2. Direct client-side calls to `generativelanguage.googleapis.com` are eliminated.
3. Deterministic arithmetic and physical calculations are evaluated locally in <1ms without calling external APIs.
4. Input validation, sanitization, rate limiting, and structured logging protect against abuse and credential leaks.

---

## Architecture

```
Android APK (R-7 Calculator)
    │
    │  HTTPS (X-R7-App-Key)
    ▼
R-7 Secure Backend
    ├── Security Headers & CORS (security.js)
    ├── Request ID & Audit Logger (requestId.js, logger.js)
    ├── Sliding Window Rate Limiter (rateLimiter.js)
    ├── Application Authenticator (auth.js)
    ├── Input Sanitization & Validation (validator.js)
    ├── Local Deterministic Solver (localSolver.js) ──► Instant Verified Math (0ms)
    ├── In-Memory LRU Cache (cacheService.js)
    ├── Server-Side Prompt Manager (promptManager.js)
    └── Model Router & Gemini Client (modelRouter.js, geminiService.js)
            │
            │  HTTPS + Server-Side Secret (GEMINI_API_KEY)
            ▼
    Google Generative AI API (gemini-3.5-flash / gemini-3.5-flash-lite)
```

---

## API Endpoints

### 1. Health Monitoring
* **Endpoint:** `GET /health`
* **Authentication:** Public
* **Response:**
```json
{
  "status": "ok",
  "service": "r7-ai-backend",
  "version": "1.0.0"
}
```

### 2. AI Problem Solver
* **Endpoint:** `POST /api/v1/ai/solve`
* **Headers:**
  * `Content-Type: application/json`
  * `X-R7-App-Key: <R7_APP_KEY>`
* **Request Body:**
```json
{
  "question": "25 * 25",
  "subject": "math",
  "mode": "solve",
  "language": "ar"
}
```
* **Success Response (200 OK):**
```json
{
  "success": true,
  "answer": "الناتج الحسابي:\n25 * 25 = 625",
  "subject": "math",
  "ai_version": "r7-ai-1.0.0",
  "request_id": "r7-2026-e5473e4c",
  "confidence": "verified",
  "verification_status": "verified",
  "processing_time_ms": 1
}
```
* **Rate Limited Response (429 Too Many Requests):**
```json
{
  "success": false,
  "error_code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests in a short interval. Please wait a few seconds.",
  "request_id": "r7-2026-xxxxxxxx"
}
```

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `PORT` | Server listen port | `8080` |
| `NODE_ENV` | Environment (`production` / `development`) | `production` |
| `GEMINI_API_KEY` | Server-Side Google Gemini API Key | *(Required)* |
| `PRIMARY_MODEL` | Primary generative model name | `gemini-3.5-flash` |
| `FALLBACK_MODEL` | Automatic fallback model name | `gemini-3.5-flash-lite` |
| `FAST_MODEL` | Fast query model name | `gemini-3.5-flash-lite` |
| `R7_APP_KEY` | Client application authentication key | `r7-app-production-key-v1` |
| `RATE_LIMIT_MAX_REQUESTS` | Maximum requests per client per minute | `30` |
| `RATE_LIMIT_BURST_MAX` | Maximum burst requests per 10 seconds | `10` |

---

## Running Locally

```bash
cd backend
npm install
cp .env.example .env
# Edit .env and supply GEMINI_API_KEY
npm start
```

## Running Automated Tests

```bash
cd backend
npm test
```

## Production Deployment (Google Cloud Run)

```bash
# 1. Build and push container image
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/r7-ai-backend backend/

# 2. Deploy to Cloud Run with HTTPS and environment secrets
gcloud run deploy r7-ai-backend \
  --image gcr.io/YOUR_PROJECT_ID/r7-ai-backend \
  --platform managed \
  --region europe-west2 \
  --allow-unauthenticated \
  --set-env-vars="NODE_ENV=production,PRIMARY_MODEL=gemini-3.5-flash,FALLBACK_MODEL=gemini-3.5-flash-lite,R7_APP_KEY=r7-app-production-key-v1" \
  --set-secrets="GEMINI_API_KEY=GEMINI_API_KEY:latest"
```
