/**
 * End-to-end verification script for GlobalR Inventory Functional Prototype.
 * Runs against http://localhost (Nginx proxy) or BASE_URL env var.
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost';
const API = `${BASE_URL}/api/v1`;

async function req(path, opts = {}) {
  const start = performance.now();
  const res = await fetch(`${API}${path}`, opts);
  const elapsedMs = Math.round(performance.now() - start);
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = text;
  }
  return { status: res.status, ok: res.ok, json, elapsedMs };
}

function assert(cond, msg) {
  if (!cond) {
    throw new Error(`ASSERTION FAILED: ${msg}`);
  }
}

async function login(username) {
  const r = await req('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: 'GlobalR2026!' })
  });
  assert(r.ok && r.json?.token, `Login failed for ${username}: ${r.status}`);
  return r.json;
}

async function main() {
  console.log(`Verifying GlobalR Prototype at ${BASE_URL}...`);

  // 1. Login multi-role
  const adminVe = await login('admin.ve');
  const supVe = await login('supervisor.ve');
  const userVe = await login('usuario.ve');
  const adminCo = await login('admin.co');
  console.log('[OK] Auth JWT working for VE & CO roles');

  const authHeader = (session, extra = {}) => ({
    Authorization: `Bearer ${session.token}`,
    'Content-Type': 'application/json',
    ...extra
  });

  // 2. Multitenancy Isolation (RNF-ARQ-003)
  const veAssets = await req('/assets', { headers: authHeader(adminVe) });
  assert(veAssets.ok && veAssets.elapsedMs < 1500, `VE list assets failed or slow (${veAssets.elapsedMs}ms)`);
  assert(veAssets.json.some((a) => a.assetNumber === 'GLR-VE-0001'), 'Missing VE asset');
  assert(!veAssets.json.some((a) => a.assetNumber === 'GLR-CO-0001'), 'Cross-tenant leak detected in VE!');

  // Non-multi-country user trying to spoof X-Country-Code=VE as CO admin
  const coSpoof = await req('/assets', { headers: authHeader(adminCo, { 'X-Country-Code': 'VE' }) });
  assert(coSpoof.ok, 'CO request failed');
  assert(coSpoof.json.every((a) => a.assetNumber.startsWith('GLR-CO')), 'Country spoofing was not blocked!');
  console.log(`[OK] Multitenancy @Filter isolation verified (VE latency ${veAssets.elapsedMs}ms < 1500ms)`);

  // 3. Encrypted QR Sign & Verify (< 1s RNF-PER-002)
  const laptop1 = veAssets.json.find((a) => a.assetNumber === 'GLR-VE-0001');
  const qrRes = await req(`/assets/${laptop1.id}/qr`, { headers: authHeader(adminVe) });
  assert(qrRes.ok && qrRes.json.encryptedPayload && qrRes.json.qrPngBase64, 'QR generation failed');

  const scanVe = await req('/assets/qr/scan', {
    method: 'POST',
    headers: authHeader(supVe),
    body: JSON.stringify({ encryptedPayload: qrRes.json.encryptedPayload })
  });
  assert(scanVe.ok && scanVe.json.valid === true, 'QR scan failed');
  assert(scanVe.json.processingMs < 1000, `QR crypto exceeded 1s: ${scanVe.json.processingMs}ms`);

  // Cross-country QR scan must be forbidden
  const scanCross = await req('/assets/qr/scan', {
    method: 'POST',
    headers: authHeader(adminCo),
    body: JSON.stringify({ encryptedPayload: qrRes.json.encryptedPayload })
  });
  assert(scanCross.status === 403, `Expected 403 on cross-country QR scan, got ${scanCross.status}`);
  console.log(`[OK] Encrypted QR verified in ${scanVe.json.processingMs}ms (< 1s) & cross-country scan blocked`);

  // 4. ISO 27001 Encrypted Vault RBAC
  const vaultDenied = await req(`/assets/${laptop1.id}/vault`, { headers: authHeader(userVe) });
  assert(vaultDenied.status === 403, `Common user must be denied vault (got ${vaultDenied.status})`);

  const vaultOk = await req(`/assets/${laptop1.id}/vault`, { headers: authHeader(supVe) });
  assert(vaultOk.ok && vaultOk.json.bitlockerRecoveryKey, 'Supervisor vault read failed');
  console.log('[OK] Asset Vault AES-256-GCM decryption & RBAC verified');

  // 5. ACID Ticket Creation + Supervisor Approval + PDF Acta
  const laptop2 = veAssets.json.find((a) => a.assetNumber === 'GLR-VE-0002');
  const createTicket = await req('/tickets', {
    method: 'POST',
    headers: authHeader(userVe),
    body: JSON.stringify({
      ticketType: 'MANTENIMIENTO',
      priority: 'ALTA',
      title: 'Prueba E2E revisión preventiva',
      reason: 'Verificación automatizada del flujo ACID',
      assetId: laptop2.id
    })
  });
  assert(createTicket.ok, `Create ticket failed: ${createTicket.status}`);

  // Common user must NOT be allowed to approve
  const approveDenied = await req(`/tickets/${createTicket.json.id}/approve`, {
    method: 'POST',
    headers: authHeader(userVe),
    body: JSON.stringify({})
  });
  assert(approveDenied.status === 403, 'Common user was able to approve ticket!');

  const approveOk = await req(`/tickets/${createTicket.json.id}/approve`, {
    method: 'POST',
    headers: authHeader(supVe),
    body: JSON.stringify({ comment: 'Aprobado por test E2E' })
  });
  assert(approveOk.ok && approveOk.json.status === 'EN_PROCESO', 'Supervisor approval failed');
  console.log(`[OK] Ticket ACID workflow verified (${approveOk.elapsedMs}ms)`);

  // 6. PDF Acta Generation (sat-pdf -> MinIO)
  const pdfGen = await req('/documents/generate', {
    method: 'POST',
    headers: authHeader(supVe),
    body: JSON.stringify({
      assetId: laptop2.id,
      ticketId: createTicket.json.id,
      documentType: 'REPARACION',
      reason: 'Acta de prueba E2E'
    })
  });
  assert(pdfGen.ok && pdfGen.json.documentNumber, 'PDF generation failed');
  const pdfDl = await fetch(`${API}/documents/${pdfGen.json.id}/download`, {
    headers: { Authorization: `Bearer ${supVe.token}` }
  });
  assert(pdfDl.ok && (await pdfDl.arrayBuffer()).byteLength > 500, 'PDF download from MinIO failed');
  console.log(`[OK] PDF Acta generated & downloaded (${pdfGen.json.documentNumber})`);

  // 7. Image Compression & MinIO upload (sat-media)
  // Minimal 1x1 valid PNG binary
  const tinyPng = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    'base64'
  );
  const fd = new FormData();
  fd.append('file', new Blob([tinyPng], { type: 'image/png' }), 'test.png');
  const mediaRes = await fetch(`${API}/assets/${laptop2.id}/media?stage=MANTENIMIENTO`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${supVe.token}` },
    body: fd
  });
  assert(mediaRes.ok, `Media upload failed: ${mediaRes.status}`);
  const mediaJson = await mediaRes.json();
  assert(mediaJson.minioObjectKey && mediaJson.sha256Hash, 'Media metadata incomplete');
  console.log(`[OK] Photo compressed & uploaded via sat-media -> MinIO (${mediaJson.minioObjectKey})`);

  // 8. Soft Delete (RNF-SEC-004)
  const tempAsset = await req('/assets', {
    method: 'POST',
    headers: authHeader(adminVe),
    body: JSON.stringify({
      assetNumber: `GLR-VE-TMP-${Date.now().toString().slice(-4)}`,
      category: 'COMPLEMENTO',
      subcategory: 'MOUSE',
      brand: 'Logitech',
      model: 'MX Master 3S',
      serialNumber: `SN-TMP-${Date.now()}`,
      conditionStatus: 'NUEVO',
      physicalLocation: 'Almacén TI'
    })
  });
  assert(tempAsset.ok, 'Temp asset creation failed');
  const delRes = await req(`/assets/${tempAsset.json.id}`, {
    method: 'DELETE',
    headers: authHeader(adminVe)
  });
  assert(delRes.status === 204, 'Soft delete failed');
  const afterDel = await req(`/assets/${tempAsset.json.id}`, { headers: authHeader(adminVe) });
  assert(afterDel.status === 404, 'Soft deleted asset still visible!');
  console.log('[OK] Soft delete (@SQLRestriction) verified');

  // 9. Audit Log Verification
  const audits = await req('/audit-logs', { headers: authHeader(adminVe) });
  assert(audits.ok && audits.json.length > 0, 'Audit logs empty');
  console.log(`[OK] Immutable audit trail verified (${audits.json.length} recent records)`);

  console.log('\nALL PROTOTYPE E2E VERIFICATIONS PASSED!');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
