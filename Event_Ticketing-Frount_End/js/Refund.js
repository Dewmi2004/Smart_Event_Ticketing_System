/* ================================================================
   Refund.js  —  EventHub Refund Management
   Connects to Spring Boot backend: http://localhost:8080/api/v1/refund
================================================================ */

'use strict';

const API = 'http://localhost:8080/api/v1/refund';

/* ── Auth helper ───────────────────────────────────────────── */
function getToken() {
    return localStorage.getItem('eh_token') || sessionStorage.getItem('eh_token') || '';
}

function authHeaders() {
    const t = getToken();
    const h = { 'Content-Type': 'application/json' };
    if (t) h['Authorization'] = 'Bearer ' + t;
    return h;
}

/* ── Toast ─────────────────────────────────────────────────── */
function toast(msg, type = 'info') {
    const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
    const wrap = document.getElementById('toastWrap');
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = `<span class="toast-icon">${icons[type] || 'ℹ️'}</span><span>${msg}</span>`;
    wrap.appendChild(el);
    setTimeout(() => {
        el.style.animation = 'toastOut 0.3s ease forwards';
        setTimeout(() => el.remove(), 320);
    }, 3800);
}

/* ── Tab switching ─────────────────────────────────────────── */
function switchTab(name, btn) {
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.s-tab').forEach(b => b.classList.remove('active'));
    document.getElementById('tab-' + name).classList.add('active');
    btn.classList.add('active');

    if (name === 'allRefunds') loadAllRefunds(false);
    if (name === 'process')    loadAllRefunds(true);
}

/* ── Status badge helper ───────────────────────────────────── */
function statusBadge(status) {
    const s = (status || '').toUpperCase();
    const cls = s === 'APPROVED' ? 'badge-approved'
        : s === 'REJECTED' ? 'badge-rejected'
            : 'badge-pending';
    return `<span class="status-badge ${cls}">${status || '—'}</span>`;
}

/* ── Format ISO datetime ───────────────────────────────────── */
function fmt(dt) {
    if (!dt) return '—';
    try {
        return new Date(dt).toLocaleString('en-GB', {
            day: 'numeric', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    } catch { return dt; }
}

/* ── Render a detail grid from a RefundDto ─────────────────── */
function renderDetailGrid(dto) {
    const fields = [
        { label: 'Refund ID',        val: dto.refundId       || '—' },
        { label: 'Booking ID',       val: dto.bookingId      || '—' },
        { label: 'Payment ID',       val: dto.paymentId      || '—' },
        { label: 'Amount (LKR)',     val: dto.amount != null ? `<span class="pc-amount">LKR ${Number(dto.amount).toLocaleString()}</span>` : '—', html: true },
        { label: 'Reason',           val: dto.reason         || '—' },
        { label: 'Status',           val: statusBadge(dto.status), html: true },
        { label: 'PayHere Refund ID',val: dto.payhereRefundId ? `<span class="payhere-tag">⚡ ${dto.payhereRefundId}</span>` : '—', html: true },
        { label: 'Requested At',     val: fmt(dto.requestedAt) },
        { label: 'Processed At',     val: fmt(dto.processedAt) },
    ];
    return fields.map(f => `
    <div class="detail-item">
      <div class="detail-item-label">${f.label}</div>
      <div class="detail-item-val">${f.html ? f.val : escHtml(String(f.val))}</div>
    </div>`).join('');
}

function escHtml(s) {
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

/* ── Build a process card for a single pending refund ──────── */
function buildProcessCard(dto) {
    return `
  <div class="process-card" id="pcard-${dto.refundId}">
    <div class="pc-top">
      <div>
        <div class="pc-id">Refund #${dto.refundId} &nbsp;·&nbsp; Booking #${dto.bookingId} &nbsp;·&nbsp; Payment #${dto.paymentId}</div>
        <div class="pc-event">${escHtml(dto.reason || 'No reason provided')}</div>
      </div>
      ${statusBadge(dto.status)}
    </div>
    <div class="pc-meta">
      <div class="pc-field">
        <div class="pc-field-label">Amount</div>
        <div class="pc-field-val pc-amount">LKR ${Number(dto.amount || 0).toLocaleString()}</div>
      </div>
      <div class="pc-field">
        <div class="pc-field-label">Requested</div>
        <div class="pc-field-val">${fmt(dto.requestedAt)}</div>
      </div>
      <div class="pc-field">
        <div class="pc-field-label">Processed</div>
        <div class="pc-field-val">${fmt(dto.processedAt)}</div>
      </div>
      ${dto.payhereRefundId ? `
      <div class="pc-field">
        <div class="pc-field-label">PayHere ID</div>
        <div class="pc-field-val"><span class="payhere-tag">⚡ ${escHtml(dto.payhereRefundId)}</span></div>
      </div>` : ''}
    </div>
    ${dto.status === 'PENDING' ? `
    <div class="pc-actions">
      <button class="btn btn-primary btn-sm" onclick="quickProcess(${dto.refundId})">
        ⚡ Process via PayHere
      </button>
    </div>` : ''}
  </div>`;
}

/* ── Stats update ──────────────────────────────────────────── */
function updateStats(list) {
    const total    = list.length;
    const pending  = list.filter(r => (r.status||'').toUpperCase() === 'PENDING').length;
    const approved = list.filter(r => (r.status||'').toUpperCase() === 'APPROVED').length;
    const rejected = list.filter(r => (r.status||'').toUpperCase() === 'REJECTED').length;
    document.getElementById('statTotal').textContent    = total;
    document.getElementById('statPending').textContent  = pending;
    document.getElementById('statApproved').textContent = approved;
    document.getElementById('statRejected').textContent = rejected;
}

/* ================================================================
   API CALLS
================================================================ */

/* POST /api/v1/refund/request */
async function handleRequestRefund(e) {
    e.preventDefault();

    const bookingId = parseInt(document.getElementById('req_bookingId').value);
    if (!bookingId) { toast('Please enter a Booking ID.', 'warning'); return; }

    const reasonEl  = document.getElementById('req_reason').value;
    const customEl  = document.getElementById('req_custom').value.trim();
    const reason    = (reasonEl === 'Other' && customEl) ? customEl : reasonEl;

    const btn = document.getElementById('btnRequest');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-inline"></span> Submitting…';

    try {
        const res = await fetch(`${API}/request`, {
            method: 'POST',
            headers: authHeaders(),
            // FIX: send bookingId as a number (integer), not a string.
            // The backend record type expects Integer — sending a JS string
            // causes Jackson to fail deserialization → 500.
            body: JSON.stringify({ bookingId: bookingId, reason })
        });

        const json = await res.json();

        if (!res.ok || json.code !== 200) {
            toast(json.message || `Error ${res.status}`, 'error');
            return;
        }

        const dto = json.data;
        toast('Refund request submitted successfully! Status: ' + dto.status, 'success');

        document.getElementById('requestResult').style.display = 'block';
        document.getElementById('requestResultGrid').innerHTML = renderDetailGrid(dto);

        document.getElementById('formRequest').reset();
        document.getElementById('req_custom_wrap').style.display = 'none';

    } catch (err) {
        toast('Network error — is the backend running on port 8080?', 'error');
        console.error('[Refund.request]', err);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '↩ Submit Refund Request';
    }
}

/* POST /api/v1/refund/process/{refundId} */
async function handleProcess() {
    const refundId = parseInt(document.getElementById('proc_refundId').value);
    if (!refundId) { toast('Please enter a Refund ID.', 'warning'); return; }
    await processRefundById(refundId, 'btnProcess', 'processResult', 'processResultGrid');
}

async function quickProcess(refundId) {
    /* Set the process input and trigger */
    document.getElementById('proc_refundId').value = refundId;
    await processRefundById(refundId, null, 'processResult', 'processResultGrid');
}

async function processRefundById(refundId, btnId, resultId, gridId) {
    const btn = btnId ? document.getElementById(btnId) : null;
    if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-inline"></span> Calling PayHere…'; }

    /* Optimistically update the card UI */
    const card = document.getElementById('pcard-' + refundId);
    if (card) {
        const actions = card.querySelector('.pc-actions');
        if (actions) actions.innerHTML = '<span style="font-size:0.8rem;color:var(--text-muted)"><span class="spinner-inline dark"></span> Processing…</span>';
    }

    try {
        const res  = await fetch(`${API}/process/${refundId}`, {
            method: 'POST',
            headers: authHeaders()
        });
        const json = await res.json();

        if (!res.ok || json.code !== 200) {
            toast(json.message || `Error ${res.status}`, 'error');
            if (card) loadAllRefunds(true); /* refresh pending list */
            return;
        }

        const dto = json.data;
        const statusText = (dto.status || '').toUpperCase();
        const type = statusText === 'APPROVED' ? 'success' : statusText === 'REJECTED' ? 'error' : 'info';
        toast(`Refund #${refundId} — ${dto.status}`, type);

        document.getElementById(resultId).style.display = 'block';
        document.getElementById(gridId).innerHTML = renderDetailGrid(dto);

        /* Refresh both list sections */
        loadAllRefunds(true);

    } catch (err) {
        toast('Network error — is the backend running on port 8080?', 'error');
        console.error('[Refund.process]', err);
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '⚡ Process via PayHere'; }
    }
}

/* GET /api/v1/refund/booking/{bookingId} */
async function handleLookup() {
    const bookingId = parseInt(document.getElementById('lk_bookingId').value);
    if (!bookingId) { toast('Please enter a Booking ID.', 'warning'); return; }

    document.getElementById('lookupResult').style.display = 'none';
    document.getElementById('lookupEmpty').style.display  = 'none';

    try {
        const res  = await fetch(`${API}/booking/${bookingId}`, { headers: authHeaders() });
        const json = await res.json();

        if (res.status === 404 || !json.data) {
            document.getElementById('lookupEmpty').style.display = 'block';
            document.getElementById('lookupEmptyMsg').textContent =
                json.message || 'No refund found for this booking.';
            return;
        }

        if (!res.ok) { toast(json.message || `Error ${res.status}`, 'error'); return; }

        const dto = json.data;
        document.getElementById('lookupResult').style.display = 'block';
        document.getElementById('lookupGrid').innerHTML = renderDetailGrid(dto);

    } catch (err) {
        toast('Network error — is the backend running on port 8080?', 'error');
        console.error('[Refund.lookup]', err);
    }
}

/* GET /api/v1/refund */
async function loadAllRefunds(pendingOnly = false) {
    const listEl    = pendingOnly
        ? document.getElementById('pendingList')
        : document.getElementById('allRefundsList');

    listEl.innerHTML = `
    <div class="empty-state">
      <div class="empty-icon"><span class="loading-spinner" style="width:32px;height:32px;"></span></div>
      <div style="margin-top:12px;color:var(--text-sub)">Loading…</div>
    </div>`;

    try {
        const res  = await fetch(API, { headers: authHeaders() });
        const json = await res.json();

        if (!res.ok) { toast(json.message || `Error ${res.status}`, 'error'); return; }

        const list = Array.isArray(json.data) ? json.data : [];
        updateStats(list);

        if (!pendingOnly) {
            /* Full table view */
            if (list.length === 0) {
                listEl.innerHTML = `
          <div class="empty-state">
            <div class="empty-icon">📋</div>
            <div>No refund records found.</div>
          </div>`;
                return;
            }

            listEl.innerHTML = `
        <div class="table-section">
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Refund ID</th>
                  <th>Booking</th>
                  <th>Payment</th>
                  <th>Amount (LKR)</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>PayHere ID</th>
                  <th>Requested</th>
                  <th>Processed</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                ${list.map((r, i) => `
                  <tr>
                    <td class="muted">${i + 1}</td>
                    <td>${r.refundId}</td>
                    <td>${r.bookingId}</td>
                    <td>${r.paymentId}</td>
                    <td><strong>LKR ${Number(r.amount || 0).toLocaleString()}</strong></td>
                    <td class="muted">${escHtml(r.reason || '—')}</td>
                    <td>${statusBadge(r.status)}</td>
                    <td class="muted">${r.payhereRefundId
                ? `<span class="payhere-tag">⚡ ${escHtml(r.payhereRefundId)}</span>`
                : '—'}</td>
                    <td class="muted">${fmt(r.requestedAt)}</td>
                    <td class="muted">${fmt(r.processedAt)}</td>
                    <td>
                      ${(r.status||'').toUpperCase() === 'PENDING'
                ? `<button class="tbl-btn tbl-btn-edit" onclick="switchTab('process',document.querySelector('[data-tab=process]'));document.getElementById('proc_refundId').value=${r.refundId}">
                             ⚡ Process
                           </button>`
                : `<span class="muted" style="font-size:0.78rem;">${escHtml(r.status||'')}</span>`}
                    </td>
                  </tr>`).join('')}
              </tbody>
            </table>
          </div>
        </div>`;

        } else {
            /* Pending-only cards for the Process tab */
            const pending = list.filter(r => (r.status||'').toUpperCase() === 'PENDING');
            if (pending.length === 0) {
                listEl.innerHTML = `
          <div class="empty-state">
            <div class="empty-icon">✅</div>
            <div>No pending refunds — all requests have been processed.</div>
          </div>`;
                return;
            }
            listEl.innerHTML = pending.map(r => buildProcessCard(r)).join('');
        }

    } catch (err) {
        listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">⚠️</div>
        <div>Could not load refunds. Make sure the backend is running on port 8080.</div>
      </div>`;
        console.error('[Refund.loadAll]', err);
    }
}

/* ── Custom reason toggle ──────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
    const reasonSel = document.getElementById('req_reason');
    const customWrap = document.getElementById('req_custom_wrap');

    if (reasonSel) {
        reasonSel.addEventListener('change', () => {
            customWrap.style.display = reasonSel.value === 'Other' ? 'block' : 'none';
        });
    }

    /* Attach request form submit */
    const formReq = document.getElementById('formRequest');
    if (formReq) formReq.addEventListener('submit', handleRequestRefund);

    /* Enter key on lookup */
    const lkInput = document.getElementById('lk_bookingId');
    if (lkInput) lkInput.addEventListener('keydown', e => { if (e.key === 'Enter') handleLookup(); });

    /* Load stats silently on page open */
    loadAllRefunds(false);
});