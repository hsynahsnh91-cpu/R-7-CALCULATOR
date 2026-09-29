import { createApp } from './src/app.js';
import { config, validateServerConfig } from './src/config/env.js';
import { logger } from './src/utils/logger.js';

validateServerConfig();

const app = createApp();

const server = app.listen(config.port, '0.0.0.0', () => {
  logger.info('SERVER_STARTED', {
    service: config.serviceName,
    port: config.port,
    env: config.env,
    primaryModel: config.primaryModel,
    fallbackModel: config.fallbackModel
  });
});

// Set server keep-alive and request timeouts
server.keepAliveTimeout = 65000;
server.headersTimeout = 66000;

// Graceful shutdown handling
function handleShutdown(signal) {
  logger.info('SHUTDOWN_SIGNAL_RECEIVED', { signal });
  server.close(() => {
    logger.info('SERVER_STOPPED');
    process.exit(0);
  });
  // Force exit after 10s if hanging
  setTimeout(() => {
    logger.error('FORCE_SHUTDOWN_TIMEOUT');
    process.exit(1);
  }, 10000).unref();
}

process.on('SIGTERM', () => handleShutdown('SIGTERM'));
process.on('SIGINT', () => handleShutdown('SIGINT'));
