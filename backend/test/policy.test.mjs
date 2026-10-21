import test from 'node:test';
import assert from 'node:assert/strict';
import { enforceGuidancePolicy } from '../src/policy.mjs';
import { parseAssistRequest } from '../src/request.mjs';
import { authorized } from '../src/http.mjs';

export const response = () => ({ canHelp: true, heardQuestion: 'Quiero ver mi factura',
  message: 'Tocá Facturas.', target: { visible: true, label: 'Facturas', x: .5, y: .4,
    width: .6, height: .1, confidence: .9 }, risk: 'LOW', requiresConfirmation: false, reason: 'Visible.' });

test('high risk and confirmations never expose a touch target', () => {
  for (const input of [{ risk: 'HIGH' }, { requiresConfirmation: true }, { message: 'Tocá Confirmar pago.' }]) {
    const result = enforceGuidancePolicy({ ...response(), ...input }, true);
    assert.equal(result.target.visible, false);
  }
});
test('missing images and uncertain coordinates cannot produce highlights', () => {
  assert.equal(enforceGuidancePolicy(response(), false).target.visible, false);
  const uncertain = response(); uncertain.target.confidence = .64;
  assert.equal(enforceGuidancePolicy(uncertain, true).target.visible, false);
  const invalid = response(); invalid.target.x = 1.2;
  assert.throws(() => enforceGuidancePolicy(invalid, true), /INVALID_GUIDANCE/);
  const missing = response(); missing.target.y = null;
  assert.throws(() => enforceGuidancePolicy(missing, true), /INVALID_TARGET/);
});
test('credential sharing instructions are replaced before speech', () => {
  const result = enforceGuidancePolicy({ ...response(), message: 'Enviá tu contraseña a soporte.' }, true);
  assert.equal(result.risk, 'HIGH'); assert.equal(result.target.visible, false);
  assert.match(result.message, /No compartas/);
});
test('malformed, oversized and wrong-format audio/image input is rejected', () => {
  const base = { question: 'Ayuda', screen: { width: 1080, height: 1920 } };
  for (const patch of [{ question: '' }, { audioBase64: '???' }, { imageBase64: Buffer.from('not an image').toString('base64') },
    { audioBase64: Buffer.alloc(960_002).toString('base64') }, { screen: { width: 0, height: 3 } }]) {
    assert.throws(() => parseAssistRequest({ ...base, ...patch }), /INVALID_/);
  }
  const input = parseAssistRequest({ ...base, context: Array.from({ length: 20 }, () => ({ role: 'user', text: 'x'.repeat(900) })) });
  assert.equal(input.context.length, 6); assert.equal(input.context[0].text.length, 800);
});
test('backend requires the exact connection code, including non-ASCII attempts', () => {
  assert.equal(authorized({ headers: { authorization: 'Bearer demo-token' } }, 'demo-token'), true);
  for (const authorization of ['', 'Bearer other', 'Bearer éééééééééé', 'x'.repeat(2000)]) {
    assert.equal(authorized({ headers: { authorization } }, 'demo-token'), false);
  }
});
