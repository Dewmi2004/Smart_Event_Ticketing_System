/* ================================================================
   SMART EVENT TICKETING — Shared JavaScript (app.js)
   Handles: QR generation (ZXing backend), form interactions, toasts, copy, nav
================================================================ */

(function () {
    'use strict';

    /* ── Backend URL — ZXing QR is generated here ── */
    var BACKEND_URL = 'http://localhost:8080';
    window.BACKEND_URL = BACKEND_URL;

    /* ─────────────────────────────────────────
       TOAST SYSTEM
    ───────────────────────────────────────── */
    function showToast(message, type, duration) {
        type     = type     || 'info';
        duration = duration || 3200;
        var container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        var icons = { success: '✅', error: '❌', info: 'ℹ️' };
        var toast = document.createElement('div');
        toast.className = 'toast ' + type;
        toast.innerHTML = '<span class="toast-icon">' + (icons[type] || 'ℹ️') + '</span><span>' + message + '</span>';
        container.appendChild(toast);
        setTimeout(function () {
            toast.style.animation = 'toastOut 0.3s ease forwards';
            setTimeout(function () { if (toast.parentNode) toast.parentNode.removeChild(toast); }, 320);
        }, duration);
    }
    window.showToast = showToast;

    /* ─────────────────────────────────────────
       QR HELPERS
       renderQR — fetches ZXing-generated QR PNG from backend.
       Pass eventId to load the event QR from /api/v1/event/{id}/qr
       If no eventId, hides the image (QR only available after save).
    ───────────────────────────────────────── */
    function buildQRPayload(fields) {
        var obj = {};
        fields.forEach(function (f) { if (f.value) obj[f.key] = f.value; });
        return JSON.stringify(obj);
    }

    function renderQR(imgEl, previewEl, payload, eventId) {
        if (!imgEl) return;

        if (!payload || payload === '{}') {
            imgEl.style.display = 'none';
            if (previewEl) previewEl.textContent = '— fill in form fields to generate QR —';
            return;
        }

        if (previewEl) previewEl.textContent = payload;

        if (eventId) {
            // Use ZXing-generated QR PNG from backend
            var url = BACKEND_URL + '/api/v1/event/' + eventId + '/qr';
            imgEl.style.display = 'block';
            imgEl.src = url;
            imgEl.onerror = function () {
                imgEl.style.display = 'none';
                if (previewEl) previewEl.textContent = 'QR generation failed. Make sure backend is running.';
                showToast('QR generation failed', 'error');
            };
        } else {
            // No eventId yet — event not saved yet
            imgEl.style.display = 'none';
            if (previewEl) previewEl.textContent = '— save the event first to generate QR —';
        }
    }

    window.renderQR       = renderQR;
    window.buildQRPayload = buildQRPayload;

    /* ─────────────────────────────────────────
       COPY TO CLIPBOARD
    ───────────────────────────────────────── */
    window.copyText = function (text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text)
                .then(function () { showToast('Copied to clipboard!', 'success', 2000); })
                .catch(function () { showToast('Copy failed', 'error'); });
        } else {
            showToast('Clipboard not supported', 'error');
        }
    };

    /* ─────────────────────────────────────────
       DOWNLOAD QR
       Downloads ZXing-generated QR PNG from backend.
       eventId is required — QR is only available after event is saved.
    ───────────────────────────────────────── */
    window.downloadQR = function (payload, filename, eventId) {
        if (!payload || payload === '{}') {
            showToast('Fill in required fields first', 'error');
            return;
        }
        if (!eventId) {
            showToast('Save the event first to download its QR', 'info');
            return;
        }
        var url = BACKEND_URL + '/api/v1/event/' + eventId + '/qr';
        var a   = document.createElement('a');
        a.href     = url;
        a.download = filename || 'event-qr.png';
        a.target   = '_blank';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        showToast('QR download started!', 'success');
    };

    /* ─────────────────────────────────────────
       FORM RESET
    ───────────────────────────────────────── */
    window.resetForm = function (formId) {
        var form = document.getElementById(formId);
        if (form) {
            form.reset();
            var qrImg     = document.getElementById('qrImage');
            var qrPreview = document.getElementById('qrPreview');
            if (qrImg)     { qrImg.src = ''; qrImg.style.display = 'none'; }
            if (qrPreview) qrPreview.textContent = '— fill in form fields to generate QR —';
            showToast('Form cleared', 'info', 2000);
        }
    };

    /* ─────────────────────────────────────────
       SET ACTIVE NAV LINK
    ───────────────────────────────────────── */
    function setActiveNav() {
        var page = location.pathname.split('/').pop() || '';
        document.querySelectorAll('.nav-link').forEach(function (link) {
            var href = link.getAttribute('href') || '';
            link.classList.toggle('active', href === page || href.split('/').pop() === page);
        });
    }

    /* ─────────────────────────────────────────
       INIT ENTITY FORM
       Called by each HTML page:
         initEntityForm({ formId, qrFields, filenamePrefix })

       For Event page — pass eventIdField so QR loads from backend:
         initEntityForm({ formId, qrFields, filenamePrefix, eventIdField: 'f_eventId' })
    ───────────────────────────────────────── */
    window.initEntityForm = function (config) {
        var form      = document.getElementById(config.formId || 'entityForm');
        var qrImg     = document.getElementById('qrImage');
        var qrPreview = document.getElementById('qrPreview');
        var dlBtn     = document.getElementById('btnDownload');

        function getEventId() {
            if (!config.eventIdField) return null;
            var el = document.getElementById(config.eventIdField);
            return el ? (parseInt(el.value) || null) : null;
        }

        function getPayload() {
            var fields = (config.qrFields || []).map(function (id) {
                var el = document.getElementById(id);
                return { key: id.replace(/^f_/, ''), value: el ? el.value.trim() : '' };
            });
            return buildQRPayload(fields);
        }

        function refresh() {
            var payload = getPayload();
            var eventId = getEventId();
            renderQR(qrImg, qrPreview, payload, eventId);
            if (dlBtn) {
                dlBtn.onclick = function () {
                    downloadQR(payload, (config.filenamePrefix || 'entity') + '-qr.png', eventId);
                };
            }
        }

        if (form) {
            form.addEventListener('input',  refresh);
            form.addEventListener('change', refresh);
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                var payload = getPayload();
                var eventId = getEventId();
                if (!payload || payload === '{}') {
                    showToast('Please fill in required fields', 'error'); return;
                }
                renderQR(qrImg, qrPreview, payload, eventId);
                showToast('QR Code generated!', 'success');
            });
            // Run once with default/pre-filled values
            setTimeout(refresh, 80);
        }
    };

    /* ─────────────────────────────────────────
       DOM READY
    ───────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', setActiveNav);

})();