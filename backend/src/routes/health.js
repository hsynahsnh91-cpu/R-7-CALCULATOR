import { Router } from 'express';
import { config } from '../config/env.js';

const router = Router();

router.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: config.serviceName,
    version: '1.0.0'
  });
});

export default router;
