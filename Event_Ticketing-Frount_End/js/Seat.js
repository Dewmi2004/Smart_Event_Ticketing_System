
    /* ======================================================
    SMART SEAT MAP — Full Interactive Logic
    ====================================================== */

    // ── Config per event ──────────────────────────────────
    const EVENTS = [
    {
        name: 'GDSE Musical Night',
        prices: { vip: 5000, premium: 3500, standard: 2500 },
        occupiedSeeds: [2,7,14,23,31,36,42,55,61,68,74,82,91,97,103,110,118],
        lockedSeeds:   [5,18,44,79,115]
    },
    {
        name: 'Cricket Final',
        prices: { vip: 3500, premium: 2500, standard: 1000 },
        occupiedSeeds: [1,8,15,22,30,37,43,56,62,69,75,83,92,98,104,111,119],
        lockedSeeds:   [3,20,50,88,116]
    },
    {
        name: 'Comedy Night',
        prices: { vip: 4000, premium: 3000, standard: 1800 },
        occupiedSeeds: [4,9,16,24,32,38,46,58,64,71,77,85,94,99,106,113,120],
        lockedSeeds:   [11,27,57,95,117]
    }
    ];

    const ROWS     = ['A','B','C','D','E','F','G','H','I','J'];
    const COLS     = 12;
    const ZONE_MAP = { A:'vip',B:'vip',C:'vip', D:'premium',E:'premium',F:'premium', G:'standard',H:'standard',I:'standard',J:'standard' };

    let currentEvent   = 0;
    let seatState      = {};   // { 'A3': { zone, status:'available'|'occupied'|'locked'|'selected' } }
    let selectedSeats  = [];   // list of seat IDs
    let discountPct    = 0;

    // ── Build initial seat state ──────────────────────────
    function buildSeatState(eventIdx) {
    const ev = EVENTS[eventIdx];
    seatState  = {};
    let seatNum = 1;
    ROWS.forEach(row => {
    for (let col = 1; col <= COLS; col++) {
    const id   = row + col;
    const zone = ZONE_MAP[row];
    let status = 'available';
    if (ev.occupiedSeeds.includes(seatNum)) status = 'occupied';
    else if (ev.lockedSeeds.includes(seatNum)) status = 'locked';
    seatState[id] = { zone, status, row, col, seatNum };
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

    const id    = row + col;
    const data  = seatState[id];
    const btn   = document.createElement('button');
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

    // Update DOM button class
    const btn = document.querySelector(`[data-id="${id}"]`);
    if (btn) btn.className = `seat ${data.zone} ${data.status}`;

    updateStats();
    updateSummary();
    updateZoneHighlight();
}

    // ── Update top stats ──────────────────────────────────
    function updateStats() {
    const all = Object.values(seatState);
    const avail = all.filter(s => s.status === 'available').length;
    const occ   = all.filter(s => s.status === 'occupied').length;
    const sel   = all.filter(s => s.status === 'selected').length;

    document.getElementById('statAvail').textContent    = avail;
    document.getElementById('statOccupied').textContent = occ;
    document.getElementById('statSelected').textContent = sel;
}

    // ── Update mini stats in admin panel ─────────────────
    function updateMiniStats() {
    const all    = Object.values(seatState);
    const locked = all.filter(s => s.status === 'locked').length;
    const avail  = all.filter(s => s.status === 'available').length;
    const occ    = all.filter(s => s.status === 'occupied').length;

    document.getElementById('miniStats').innerHTML = `
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--success)">${avail}</div><div class="mini-stat-lbl">Available</div></div>
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--danger)">${occ}</div><div class="mini-stat-lbl">Occupied</div></div>
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--warning)">${locked}</div><div class="mini-stat-lbl">Locked</div></div>
    <div class="mini-stat"><div class="mini-stat-val" style="color:var(--accent)">${selectedSeats.length}</div><div class="mini-stat-lbl">Selected</div></div>
  `;
}

    // ── Update price display ──────────────────────────────
    function updatePriceDisplay() {
    const ev = EVENTS[currentEvent];
    document.getElementById('price-vip').textContent      = 'LKR ' + ev.prices.vip.toLocaleString();
    document.getElementById('price-premium').textContent  = 'LKR ' + ev.prices.premium.toLocaleString();
    document.getElementById('price-standard').textContent = 'LKR ' + ev.prices.standard.toLocaleString();
}

    // ── Highlight active zone ─────────────────────────────
    function updateZoneHighlight() {
    ['vip','premium','standard'].forEach(z => {
        document.getElementById('zone-' + z).classList.remove('active-zone');
    });
    if (selectedSeats.length > 0) {
    const lastZone = seatState[selectedSeats[selectedSeats.length - 1]]?.zone;
    if (lastZone) document.getElementById('zone-' + lastZone).classList.add('active-zone');
}
}

    // ── Calculate price ───────────────────────────────────
    function calcTotal(withDiscount = true) {
    const ev = EVENTS[currentEvent];
    let subtotal = 0;
    selectedSeats.forEach(id => {
    const zone = seatState[id]?.zone;
    subtotal += ev.prices[zone] || 0;
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

    // Seat tags
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
    const ev = EVENTS[currentEvent];

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
    const ev   = EVENTS[currentEvent];
    const tip  = document.getElementById('seatTooltip');

    document.getElementById('ttTitle').textContent  = `Row ${data.row} · Seat ${data.col}`;
    document.getElementById('ttType').textContent   = data.zone.charAt(0).toUpperCase() + data.zone.slice(1) + ' Zone';
    document.getElementById('ttPrice').textContent  = data.status === 'available' || data.status === 'selected'
    ? 'LKR ' + (ev.prices[data.zone] || 0).toLocaleString()
    : '—';
    document.getElementById('ttStatus').textContent = data.status.charAt(0).toUpperCase() + data.status.slice(1);

    tip.classList.add('show');
    moveTooltip(e);
}
    function moveTooltip(e) {
    const tip = document.getElementById('seatTooltip');
    tip.style.left = (e.clientX + 14) + 'px';
    tip.style.top  = (e.clientY - 60) + 'px';
}
    function hideTooltip() {
    tooltipTimeout = setTimeout(() => {
        document.getElementById('seatTooltip').classList.remove('show');
    }, 80);
}

    // ── Switch event ──────────────────────────────────────
    function switchEvent(idx, chipEl) {
    currentEvent  = idx;
    selectedSeats = [];
    discountPct   = 0;
    document.getElementById('couponInput').value = '';
    document.querySelectorAll('.event-chip').forEach(c => c.classList.remove('active'));
    chipEl.classList.add('active');
    buildSeatState(idx);
    renderGrid();
    updateSummary();
    showToast('Switched to: ' + EVENTS[idx].name, 'success');
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
    buildSeatState(currentEvent);
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
    a.download = `seat-map-${EVENTS[currentEvent].name.replace(/\s/g,'_')}.json`;
    a.click();
    showToast('📤 Seat data exported!', 'success');
}

    function proceedBooking() {
    if (selectedSeats.length === 0) { showToast('Select at least one seat first.', 'warning'); return; }
    const { total } = calcTotal();
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

    // ── Init ──────────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
    buildSeatState(0);
    renderGrid();
    updateSummary();

    // Enter on coupon input
    document.getElementById('couponInput').addEventListener('keydown', e => {
    if (e.key === 'Enter') applyCoupon();
});
});

