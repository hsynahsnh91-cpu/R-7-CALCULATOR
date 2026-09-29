import crypto from 'crypto';

export function generateRequestId() {
  const randomHex = crypto.randomBytes(4).toString('hex');
  const year = new Date().getFullYear();
  return `r7-${year}-${randomHex}`;
}
