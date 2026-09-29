import dotenv from 'dotenv';
dotenv.config();

export const config = {
  env: process.env.NODE_ENV || 'production',
  port: parseInt(process.env.PORT || '8080', 10),
  
  // Gemini API Secrets (STRICTLY SERVER-SIDE)
  geminiApiKey: process.env.GEMINI_API_KEY || '',
  
  // Dynamic Model Configuration
  primaryModel: process.env.PRIMARY_MODEL || 'gemini-3.5-flash',
  fallbackModel: process.env.FALLBACK_MODEL || 'gemini-3.5-flash-lite',
  fastModel: process.env.FAST_MODEL || 'gemini-3.5-flash-lite',
  
  // Application Authentication & Security
  appKey: process.env.R7_APP_KEY || 'r7-app-production-key-v1',
  allowedOrigins: (process.env.ALLOWED_ORIGINS || '').split(',').map(s => s.trim()).filter(Boolean),
  
  // Rate Limiting Policy
  rateLimitWindowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '60000', 10), // 1 minute
  rateLimitMaxRequests: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS || '30', 10), // 30 req/min
  rateLimitBurstMax: parseInt(process.env.RATE_LIMIT_BURST_MAX || '10', 10), // 10 req/10sec
  
  // Service Metadata
  serviceName: 'r7-ai-backend',
  aiVersion: process.env.AI_VERSION || 'r7-ai-1.0.0',
  maxRequestLength: 2000
};

export function validateServerConfig() {
  if (!config.geminiApiKey) {
    console.warn('[SECURITY WARNING] GEMINI_API_KEY is not set in backend environment! AI requests will return service unavailable.');
  }
}
