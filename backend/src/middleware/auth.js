import { config } from '../config/env.js';

export function authenticateApp(req, res, next) {
  // Allow health check without authentication
  if (req.path === '/health' || req.path === '/') {
    return next();
  }

  const appKeyHeader = req.headers['x-r7-app-key'];
  const authHeader = req.headers['authorization'];

  let providedKey = '';
  if (appKeyHeader && typeof appKeyHeader === 'string') {
    providedKey = appKeyHeader.trim();
  } else if (authHeader && authHeader.startsWith('Bearer ')) {
    providedKey = authHeader.substring(7).trim();
  }

  // If a backend app key is configured, enforce matching
  if (config.appKey && config.appKey !== '') {
    if (!providedKey || providedKey !== config.appKey) {
      return res.status(401).json({
        success: false,
        error_code: 'UNAUTHORIZED_APPLICATION',
        message: 'Invalid or missing application authentication credentials.',
        request_id: req.id
      });
    }
  }

  next();
}
