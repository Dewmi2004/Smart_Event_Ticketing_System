/* ================================================================
   SMART EVENT TICKETING — Shared JavaScript (app.js)
   Handles: QR generation, form interactions, toasts, copy, nav
================================================================ */

(function () {
    'use strict';

    /* ── QR API ── */
    const QR_API = (data, size = 200) =>
        `https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&ecc=M&data=${encodeURIComponent(data)}`;

    /* ─────────────────────────────────────────
       TOAST SYSTEM
    ───────────────────────────────────────── */
    function showToast(message, type, duration) {
        type = type || 'info';
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
    ───────────────────────────────────────── */
    function buildQRPayload(fields) {
        var obj = {};
        fields.forEach(function (f) { if (f.value) obj[f.key] = f.value; });
        return JSON.stringify(obj);
    }

    function renderQR(imgEl, previewEl, payload) {
        if (!imgEl) return;
        if (!payload || payload === '{}') {
            imgEl.style.display = 'none';
            if (previewEl) previewEl.textContent = '— fill in form fields to generate QR —';
            return;
        }
        var url = QR_API(payload);
        imgEl.style.display = 'block';
        imgEl.src = url;
        imgEl.onerror = function () {
            imgEl.style.display = 'none';
            if (previewEl) previewEl.textContent = 'QR preview unavailable offline. Payload: ' + payload;
            showToast('QR preview needs internet connection', 'info');
        };
        if (previewEl) previewEl.textContent = payload;
    }

    window.renderQR    = renderQR;
    window.buildQRPayload = buildQRPayload;
    window.QR_API      = QR_API;

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
    ───────────────────────────────────────── */
    window.downloadQR = function (payload, filename) {
        if (!payload || payload === '{}') {
            showToast('Fill in required fields first', 'error'); return;
        }
        var url = QR_API(payload, 400);
        var a = document.createElement('a');
        a.href = url;
        a.download = filename || 'qr-code.png';
        a.target = '_blank';
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
       qrFields = array of input element IDs to encode into QR
    ───────────────────────────────────────── */
    window.initEntityForm = function (config) {
        var form      = document.getElementById(config.formId || 'entityForm');
        var qrImg     = document.getElementById('qrImage');
        var qrPreview = document.getElementById('qrPreview');
        var dlBtn     = document.getElementById('btnDownload');

        function getPayload() {
            var fields = (config.qrFields || []).map(function (id) {
                var el = document.getElementById(id);
                return { key: id.replace(/^f_/, ''), value: el ? el.value.trim() : '' };
            });
            return buildQRPayload(fields);
        }

        function refresh() {
            var payload = getPayload();
            renderQR(qrImg, qrPreview, payload);
            if (dlBtn) {
                dlBtn.onclick = function () {
                    downloadQR(payload, (config.filenamePrefix || 'entity') + '-qr.png');
                };
            }
        }

        if (form) {
            form.addEventListener('input',  refresh);
            form.addEventListener('change', refresh);
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                var payload = getPayload();
                if (!payload || payload === '{}') {
                    showToast('Please fill in required fields', 'error'); return;
                }
                renderQR(qrImg, qrPreview, payload);
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