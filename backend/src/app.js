import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { generateRequestId } from './utils/requestId.js';
import { securityHeaders } from './middleware/security.js';
import { rateLimiter } from './middleware/rateLimiter.js';
import { authenticateApp } from './middleware/auth.js';
import { errorHandler } from './middleware/errorHandler.js';
import healthRouter from './routes/health.js';
import aiRouter from './routes/ai.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '../../');

export function createApp() {
  const app = express();

  // Attach unique non-identifying request ID
  app.use((req, res, next) => {
    req.id = generateRequestId();
    res.setHeader('X-Request-Id', req.id);
    next();
  });

  // Security headers & CORS
  app.use(securityHeaders);

  // Serve static files from root directory for web preview
  app.use(express.static(rootDir));

  // Request size limit & JSON parser
  app.use(express.json({ limit: '64kb' }));

  // Global rate limiter
  app.use(rateLimiter);

  // Application-level authentication (for API routes)
  app.use('/api', authenticateApp);

  // Route registration
  app.use(healthRouter);
  app.use(aiRouter);

  // 404 handler for unknown routes (API vs Static SPA fallback)
  app.use((req, res) => {
    if (req.path.startsWith('/api/')) {
      return res.status(404).json({
        success: false,
        error_code: 'NOT_FOUND',
        message: 'The requested API endpoint does not exist.',
        request_id: req.id
      });
    }
    res.sendFile(path.join(rootDir, 'index.html'));
  });

  // Centralized safe error handler
  app.use(errorHandler);

  return app;
}
