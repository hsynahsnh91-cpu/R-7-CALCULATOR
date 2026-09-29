import { config } from '../config/env.js';

export function securityHeaders(req, res, next) {
  // Prevent MIME type sniffing
  res.setHeader('X-Content-Type-Options', 'nosniff');
  // Strict Transport Security (HSTS)
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
  // Referrer policy
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  // Remove X-Powered-By
  res.removeHeader('X-Powered-By');

  // Allow iframe embedding for AI Studio preview while protecting against third-party framing
  res.setHeader(
    'Content-Security-Policy',
    "frame-ancestors 'self' https://*.google.com https://localhost.corp.google.com:26001;"
  );

  // CORS handling
  const origin = req.headers.origin;
  if (origin) {
    const isAllowed = config.allowedOrigins.length === 0 ||
                      config.allowedOrigins.includes(origin) ||
                      origin.endsWith('.google.com') ||
                      origin.includes('europe-west2.run.app');
    if (isAllowed) {
      res.setHeader('Access-Control-Allow-Origin', origin);
      res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
      res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-R7-App-Key');
      res.setHeader('Access-Control-Max-Age', '86400');
    }
  }

  if (req.method === 'OPTIONS') {
    return res.status(204).end();
  }

  next();
}
