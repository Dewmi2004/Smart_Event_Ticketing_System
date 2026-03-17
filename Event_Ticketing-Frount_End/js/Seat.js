/* ======================================================
    SMART SEAT MAP — Full Interactive Logic
    ====================================================== */

// ── API ───────────────────────────────────────────────
const EVENT_API = 'http://localhost:8080/api/v1/event';
const SEAT_API  = 'http://localhost:8080/api/v1/seat';

function getToken() {
    return localStorage.getItem('eh_token') || sessionStorage.getItem('eh_token') || '';
}
function authHeaders() {
    return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + getToken() };
}

const ROWS     = ['A','B','C','D','E','F','G','H','I','J'];
const COLS     = 12;
const ZONE_MAP = { A:'vip',B:'vip',C:'vip', D:'premium',E:'premium',F:'premium', G:'standard',H:'standard',I:'standard',J:'standard' };

let currentEvent  = null;
let apiSeats      = [];
let seatState     = {};
let selectedSeats = [];
let discountPct   = 0;

// ── Load events from API → build chips ────────────────
function loadEvents() {
    const chips = document.getElementById('eventChips');
    chips.innerHTML = '<p style="color:var(--text-sub);font-size:13px;padding:8px;">Loading events…</p>';

    $.ajax({
        url: EVENT_API, method: 'GET', headers: authHeaders(),
        success: function (res) {
            const events = Array.isArray(res) ? res : (res.data || []);
            const active = events.filter(e => e.status === 'Active' || e.status === 'Upcoming');

            if (active.length === 0) {
                chips.innerHTML = '<p style="color:var(--text-sub);font-size:13px;padding:8px;">No active events found.</p>';
                return;
            }

            chips.innerHTML = '';
            active.forEach((ev, idx) => {
                const chip = document.createElement('button');
                chip.className = 'event-chip' + (idx === 0 ? ' active' : '');
                chip.innerHTML =
                    '<span class="chip-name">' + ev.event_name + '</span>' +
                    '<span class="chip-meta">' +
                    (ev.date     ? formatDateChip(ev.date) : '') +
                    (ev.location ? ' · ' + ev.location     : '') +
                    '</span>';
                chip.addEventListener('click', function () { switchEvent(ev, chip); });
                chips.appendChild(chip);
            });

            switchEvent(active[0], chips.querySelector('.event-chip'));
        },
        error: function (err) {
            chips.innerHTML = '<p style="color:var(--danger);font-size:13px;padding:8px;">Failed to load events.</p>';
            console.error('loadEvents error:', err);
        }
    });
}

// ── Build seat state — ROWS×COLS + ZONE_MAP ───────────
// Zone/colour always guaranteed from ZONE_MAP.
// DB seat looked up for seatId / price / status only.
function buildSeatState() {
    seatState = {};
    let seatNum = 1;

    // Pre-build a lookup map from seat_number → seat for O(1) access
    // Handles both 'seat_number' field names from SeatDto
    const dbMap = {};
    apiSeats.forEach(s => {
        // SeatDto uses snake_case: seat_number, seat_id
        const key = (s.seat_number || s.seatNumber || '').toString().trim().toUpperCase();
        if (key) dbMap[key] = s;
    });

    // Log first seat to confirm field names
    if (apiSeats.length > 0) {
        console.log('[Seat.js] Sample DB seat:', apiSeats[0]);
        console.log('[Seat.js] dbMap keys (first 5):', Object.keys(dbMap).slice(0, 5));
    }

    ROWS.forEach(row => {
        for (let col = 1; col <= COLS; col++) {
            const id   = row + col;                  // e.g. "A3"
            const zone = ZONE_MAP[row];              // always vip/premium/standard

            const dbSeat = dbMap[id.toUpperCase()]; // look up e.g. "A3"

            if (!dbSeat) {
                console.warn('[Seat.js] No DB seat for:', id);
            }

            let status = 'available';
            if (dbSeat) {
                const s = (dbSeat.status || '').toLowerCase();
                if      (s === 'booked')  status = 'occupied';
                else if (s === 'locked')  status = 'locked';
            }

            // Support both snake_case (seat_id) and camelCase (seatId) from API
            const seatId = dbSeat ? (dbSeat.seat_id || dbSeat.seatId || null) : null;
            const price  = dbSeat ? (dbSeat.price   || 0)                     : 0;

            seatState[id] = { zone, status, row, col, seatNum, seatId, price };
            seatNum++;
        }
    });
}

// ── Render the seat grid ──────────────────────────────
function renderGrid() {
    const grid = document.getElementById('seatGrid');
    grid.innerHTML = '';

    ROWS.forEach(row => {
        const rowEl = document.createElement('div');
        rowEl.className = 'seat-row';

        // Row label
        const lbl = document.createElement('div');
        lbl.className = 'row-label';
        lbl.textContent = row;
        rowEl.appendChild(lbl);

        for (let col = 1; col <= COLS; col++) {
            // Centre aisle after seat 6
            if (col === 7) {
                const aisle = document.createElement('div');
                aisle.className = 'aisle';
                rowEl.appendChild(aisle);
            }

            const id   = row + col;
            const data = seatState[id];
            const btn  = document.createElement('button');
            btn.className = `seat ${data.zone} ${data.status}`;
            btn.dataset.id = id;
            btn.textContent = col;
            btn.setAttribute('aria-label', `Row ${row} Seat ${col} — ${data.zone} — ${data.status}`);

            if (data.status === 'available' || data.status === 'selected') {
                btn.addEventListener('click', () => toggleSeat(id));
                btn.addEventListener('mouseenter', (e) => showTooltip(e, id));
                btn.addEventListener('mouseleave', hideTooltip);
                btn.addEventListener('mousemove', moveTooltip);
            } else {
                btn.addEventListener('mouseenter', (e) => showTooltip(e, id));
                btn.addEventListener('mouseleave', hideTooltip);
                btn.addEventListener('mousemove', moveTooltip);
            }
            rowEl.appendChild(btn);
        }
        grid.appendChild(rowEl);
    });

    updateStats();
    updateMiniStats();
    updatePriceDisplay();
}

// ── Toggle seat selection ─────────────────────────────
function toggleSeat(id) {
    const data = seatState[id];
    if (data.status === 'occupied' || data.status === 'locked') return;

    if (data.status === 'selected') {
        data.status = 'available';
        selectedSeats = selectedSeats.filter(s => s !== id);
    } else {
        data.status = 'selected';
        selectedSeats.push(id);
    }

    const btn = document.querySelector(`[data-id="${id}"]`);
    if (btn) btn.className = `seat ${data.zone} ${data.status}`;

    updateStats();
    updateSummary();
    updateZoneHighlight();
}

// ── Update top stats ──────────────────────────────────
function updateStats() {
    const all   = Object.values(seatState);
    const avail = all.filter(s => s.status === 'available').length;
    const occ   = all.filter(s => s.status === 'occupied').length;
    const sel   = all.filter(s => s.status === 'selected').length;

    document.getElementById('statAvail').textContent    = avail;
    document.getElementById('statOccupied').textContent = occ;
    document.getElementById('statSelected').textContent = sel;
}

// ── Update mini stats ─────────────────────────────────
function updateMiniStats() {
    const miniEl = document.getElementById('miniStats');
    if (!miniEl) return;
    const all    = Object.values(seatState);
    const locked = all.filter(s => s.status === 'locked').length;
    const avail  = all.filter(s => s.status === 'available').length;
    const occ    = all.filter(s => s.status === 'occupied').length;

    miniEl.innerHTML = `
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--success)">${avail}</div><div class="mini-stat-lbl">Available</div></div>
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--danger)">${occ}</div><div class="mini-stat-lbl">Occupied</div></div>
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--warning)">${locked}</div><div class="mini-stat-lbl">Locked</div></div>
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--accent)">${selectedSeats.length}</div><div class="mini-stat-lbl">Selected</div></div>
  `;
}

// ── Update price display ──────────────────────────────
function updatePriceDisplay() {
    const prices = { vip: null, premium: null, standard: null };
    Object.values(seatState).forEach(s => {
        if (s.price && prices[s.zone] === null) prices[s.zone] = s.price;
    });
    document.getElementById('price-vip').textContent      = prices.vip      != null ? 'LKR ' + Number(prices.vip).toLocaleString()      : '—';
    document.getElementById('price-premium').textContent  = prices.premium  != null ? 'LKR ' + Number(prices.premium).toLocaleString()  : '—';
    document.getElementById('price-standard').textContent = prices.standard != null ? 'LKR ' + Number(prices.standard).toLocaleString() : '—';
}

// ── Highlight active zone ─────────────────────────────
function updateZoneHighlight() {
    ['vip','premium','standard'].forEach(z => {
        const el = document.getElementById('zone-' + z);
        if (el) el.classList.remove('active-zone');
    });
    if (selectedSeats.length > 0) {
        const lastZone = seatState[selectedSeats[selectedSeats.length - 1]]?.zone;
        if (lastZone) {
            const el = document.getElementById('zone-' + lastZone);
            if (el) el.classList.add('active-zone');
        }
    }
}

// ── Calculate price ───────────────────────────────────
function calcTotal(withDiscount = true) {
    let subtotal = 0;
    selectedSeats.forEach(id => {
        subtotal += seatState[id]?.price || 0;
    });
    const discount = withDiscount ? Math.round(subtotal * discountPct / 100) : 0;
    return { subtotal, discount, total: subtotal - discount };
}

// ── Update summary panel ──────────────────────────────
function updateSummary() {
    const list    = document.getElementById('selectedSeatsList');
    const hint    = document.getElementById('emptyHint');
    const details = document.getElementById('summaryDetails');
    const clearBtn = document.getElementById('clearBtn');
    const bookBtn  = document.getElementById('bookBtn');

    list.innerHTML = '';

    if (selectedSeats.length === 0) {
        hint.style.display = 'block';
        details.style.display = 'none';
        clearBtn.style.display = 'none';
        bookBtn.disabled = true;
        return;
    }

    hint.style.display = 'none';
    details.style.display = 'block';
    clearBtn.style.display = 'inline-flex';
    bookBtn.disabled = false;

    selectedSeats.forEach(id => {
        const data = seatState[id];
        const tag  = document.createElement('span');
        tag.className = 'seat-tag';
        tag.innerHTML = `${id} <span style="opacity:.5">×</span>`;
        tag.title = `Click to deselect ${id}`;
        tag.onclick = () => { toggleSeat(id); };
        list.appendChild(tag);
    });

    const { subtotal, discount, total } = calcTotal();

    document.getElementById('sumCount').textContent    = selectedSeats.length + ' seat' + (selectedSeats.length > 1 ? 's' : '');
    document.getElementById('sumSubtotal').textContent = 'LKR ' + subtotal.toLocaleString();
    document.getElementById('sumTotal').textContent    = 'LKR ' + total.toLocaleString();

    if (discount > 0) {
        document.getElementById('discountRow').style.display = 'flex';
        document.getElementById('sumDiscount').textContent = '− LKR ' + discount.toLocaleString();
    } else {
        document.getElementById('discountRow').style.display = 'none';
    }

    updateMiniStats();
}

// ── Coupon ────────────────────────────────────────────
const COUPONS = { 'GDSE2024': 20, 'EARLYBIRD': 15, 'VIP50': 50 };
function applyCoupon() {
    const code = document.getElementById('couponInput').value.trim().toUpperCase();
    if (!code) { showToast('Enter a coupon code first.', 'warning'); return; }
    if (COUPONS[code]) {
        discountPct = COUPONS[code];
        showToast(`✅ Coupon "${code}" applied — ${discountPct}% off!`, 'success');
        updateSummary();
    } else {
        discountPct = 0;
        showToast(`❌ Coupon "${code}" is invalid or expired.`, 'error');
        updateSummary();
    }
}

// ── Tooltip ───────────────────────────────────────────
let tooltipTimeout;
function showTooltip(e, id) {
    clearTimeout(tooltipTimeout);
    const data = seatState[id];
    const tip  = document.getElementById('seatTooltip');
    if (!tip || !data) return;

    document.getElementById('ttTitle').textContent  = `Row ${data.row} · Seat ${data.col}`;
    document.getElementById('ttType').textContent   = data.zone.charAt(0).toUpperCase() + data.zone.slice(1) + ' Zone';
    document.getElementById('ttPrice').textContent  = data.status === 'available' || data.status === 'selected'
        ? 'LKR ' + (data.price || 0).toLocaleString()
        : '—';
    document.getElementById('ttStatus').textContent = data.status.charAt(0).toUpperCase() + data.status.slice(1);

    tip.classList.add('show');
    moveTooltip(e);
}
function moveTooltip(e) {
    const tip = document.getElementById('seatTooltip');
    if (!tip) return;
    tip.style.left = (e.clientX + 14) + 'px';
    tip.style.top  = (e.clientY - 60) + 'px';
}
function hideTooltip() {
    tooltipTimeout = setTimeout(() => {
        const tip = document.getElementById('seatTooltip');
        if (tip) tip.classList.remove('show');
    }, 80);
}

// ── Switch event ──────────────────────────────────────
function switchEvent(ev, chipEl) {
    currentEvent  = ev;
    selectedSeats = [];
    discountPct   = 0;
    document.getElementById('couponInput').value = '';
    document.querySelectorAll('.event-chip').forEach(c => c.classList.remove('active'));
    chipEl.classList.add('active');

    document.getElementById('seatGrid').innerHTML =
        '<div style="color:var(--text-sub);font-size:13px;padding:24px;text-align:center;">Loading seats…</div>';

    $.ajax({
        url: SEAT_API + '/event/' + ev.eventId, method: 'GET', headers: authHeaders(),
        success: function (res) {
            apiSeats = Array.isArray(res) ? res : (res.data || []);
            buildSeatState();
            renderGrid();
            updateSummary();
            showToast('Switched to: ' + ev.event_name, 'success');
        },
        error: function (err) {
            apiSeats = [];
            buildSeatState();
            renderGrid();
            updateSummary();
            console.error('seat load error:', err);
        }
    });
}

// ── Admin actions ─────────────────────────────────────
function clearAll() {
    selectedSeats.forEach(id => {
        seatState[id].status = 'available';
        const btn = document.querySelector(`[data-id="${id}"]`);
        if (btn) btn.className = `seat ${seatState[id].zone} available`;
    });
    selectedSeats = [];
    updateStats();
    updateSummary();
}

function unlockAll() {
    let count = 0;
    Object.keys(seatState).forEach(id => {
        if (seatState[id].status === 'locked') {
            seatState[id].status = 'available';
            const btn = document.querySelector(`[data-id="${id}"]`);
            if (btn) {
                btn.className = `seat ${seatState[id].zone} available`;
                btn.addEventListener('click', () => toggleSeat(id));
                btn.addEventListener('mouseenter', (e) => showTooltip(e, id));
                btn.addEventListener('mouseleave', hideTooltip);
                btn.addEventListener('mousemove', moveTooltip);
            }
            count++;
        }
    });
    updateStats(); updateMiniStats();
    showToast(`🔓 ${count} locked seat(s) released.`, 'success');
}

function resetAllSeats() {
    if (!confirm('Reset all seats to Available for this event?')) return;
    selectedSeats = [];
    apiSeats = [];
    buildSeatState();
    renderGrid();
    updateSummary();
    showToast('🔄 All seats reset to Available.', 'warning');
}

function exportSeatData() {
    const data = JSON.stringify(seatState, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `seat-map-${currentEvent ? currentEvent.event_name.replace(/\s/g,'_') : 'event'}.json`;
    a.click();
    showToast('📤 Seat data exported!', 'success');
}

// ── Proceed to Booking ────────────────────────────────
function proceedBooking() {
    if (selectedSeats.length === 0) { showToast('Select at least one seat first.', 'warning'); return; }
    if (!currentEvent)              { showToast('No event selected.', 'warning'); return; }

    const { total } = calcTotal();

    // Collect seatIds — support both seat_id and seatId field names from API
    const seatIds = selectedSeats
        .map(id => {
            const s = seatState[id];
            return s.seatId || s.seat_id || null;
        })
        .filter(id => id !== null && id !== undefined);

    // If no DB seatIds found, warn clearly instead of silently sending empty array
    if (seatIds.length === 0) {
        showToast('⚠️ Seat IDs not found in DB. Check console for mismatch details.', 'error');
        console.error('[Seat.js] Selected seats have no DB seatId:', selectedSeats.map(id => seatState[id]));
        console.error('[Seat.js] apiSeats sample:', apiSeats.slice(0, 3));
        return;
    }

    const seatNums = selectedSeats.join(', ');

    const payload = {
        eventId:        currentEvent.eventId,
        event_name:     currentEvent.event_name,
        event_location: currentEvent.location,
        event_date:     currentEvent.date,
        total_amount:   total,
        couponCode:     document.getElementById('couponInput').value.trim(),
        seat_ids:       seatIds,
        seat_numbers:   seatNums
    };

    console.log('[Seat.js] pendingBooking payload:', payload);
    sessionStorage.setItem('pendingBooking', JSON.stringify(payload));

    showToast(`🎟️ Proceeding with ${selectedSeats.length} seat(s) — LKR ${total.toLocaleString()}`, 'success');
    setTimeout(() => { window.location.href = 'Booking.html'; }, 1500);
}

// ── Toast helper ──────────────────────────────────────
function showToast(msg, type = 'success') {
    const wrap  = document.getElementById('toastWrap');
    const icons = { success:'✅', warning:'⚠️', error:'❌', info:'ℹ️' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span>${icons[type]||'ℹ️'}</span><span>${msg}</span>`;
    wrap.appendChild(toast);
    setTimeout(() => { toast.style.transition='opacity .3s'; toast.style.opacity='0'; setTimeout(()=>toast.remove(),300); }, 3000);
}

// ── Date chip helper ──────────────────────────────────
function formatDateChip(d) {
    if (!d) return '';
    const parts = String(d).split('-');
    if (parts.length < 3) return d;
    return parts[2] + '/' + parts[1] + '/' + parts[0];
}

// ── Init ──────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    loadEvents();

    document.getElementById('couponInput').addEventListener('keydown', e => {
        if (e.key === 'Enter') applyCoupon();
    });
});