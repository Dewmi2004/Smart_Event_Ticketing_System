/**
 * forgot-password.js
 * Handles the 4-step forgot-password wizard for EventHub.
 *
 * Step 1 → POST /api/v1/auth/forgot-password   (send OTP)
 * Step 2 → POST /api/v1/auth/verify-otp        (verify OTP → get resetToken)
 * Step 3 → POST /api/v1/auth/reset-password    (set new password)
 */

const API_BASE = 'http://localhost:8080';   // ← change in production

/* ─── State ─────────────────────────────────────────────── */
let currentStep   = 1;
let resetToken    = null;        // JWT returned after OTP verification
let timerInterval = null;
let timerSeconds  = 60;

/* ─── Step navigation ───────────────────────────────────── */
function goToStep(n) {
    // Hide current
    document.getElementById('step' + currentStep).classList.remove('active');

    // Update progress bar
    for (let i = 1; i <= 4; i++) {
        const ps = document.getElementById('ps' + i);
        ps.classList.remove('active', 'done');
        if (i < n)  ps.classList.add('done');
        if (i === n) ps.classList.add('active');
    }

    currentStep = n;
    document.getElementById('step' + n).classList.add('active');

    // Hide progress bar on success step
    document.getElementById('progressBar').style.display = n === 4 ? 'none' : '';
}

/* ─── Utility helpers ───────────────────────────────────── */
function setLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    btn.classList.toggle('loading', loading);
}

function showAlert(stepNum, msg, type = 'error') {
    const el = document.getElementById('alert' + stepNum);
    el.className = 'alert-banner ' + type;
    el.innerHTML = `<i class="fa-solid ${type === 'error' ? 'fa-circle-exclamation' : 'fa-circle-check'}"></i> ${msg}`;
}

function clearAlert(stepNum) {
    const el = document.getElementById('alert' + stepNum);
    el.className = 'alert-banner';
    el.innerHTML = '';
}

function showFieldErr(id, msg) {
    const el = document.getElementById(id);
    el.querySelector('span').textContent = msg;
    el.style.display = 'flex';
    // Mark input error
    const field = el.previousElementSibling;
    const input = field?.querySelector?.('.auth-input') ?? field;
    input?.classList?.add('is-error');
}

function clearFieldErr(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.style.display = 'none';
}

function getChannel() {
    return document.querySelector('input[name="channel"]:checked').value;
}

/* ─── Channel selector cards ────────────────────────────── */
document.querySelectorAll('.channel-card').forEach(card => {
    card.addEventListener('click', () => {
        document.querySelectorAll('.channel-card').forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');
        card.querySelector('input[type="radio"]').checked = true;

        const isSms = card.id === 'ccSms';
        const pf = document.getElementById('phoneField');
        if (isSms) {
            pf.classList.add('show');
            pf.classList.remove('auth-field');
            pf.style.display = 'flex';
            pf.style.flexDirection = 'column';
            pf.style.gap = '7px';
        } else {
            pf.style.display = 'none';
        }
    });
});

/* ─── STEP 1: Send OTP ──────────────────────────────────── */
async function sendOtp() {
    clearAlert(1);
    clearFieldErr('emailErr');
    clearFieldErr('phoneErr');

    const email   = document.getElementById('emailInput').value.trim();
    const channel = getChannel();
    const phone   = document.getElementById('phoneInput').value.trim();

    // Validate
    if (!email) { showFieldErr('emailErr', 'Email is required'); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { showFieldErr('emailErr', 'Enter a valid email address'); return; }
    if (channel === 'SMS' && !phone) { showFieldErr('phoneErr', 'Phone number is required for SMS'); return; }

    setLoading('btn1', true);

    try {
        const body = { email, channel };
        if (channel === 'SMS') body.phone = phone;

        const res  = await fetch(`${API_BASE}/api/v1/auth/forgot-password`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body),
        });

        const data = await res.json().catch(() => ({}));

        if (res.ok) {
            // Update OTP subtext
            document.getElementById('otpSubText').textContent =
                channel === 'SMS'
                    ? `We sent a 6-digit code to ${phone}.`
                    : `We sent a 6-digit code to ${email}.`;
            goToStep(2);
            startTimer();
        } else {
            showAlert(1, data.message || 'Failed to send OTP. Please try again.');
        }
    } catch (err) {
        showAlert(1, 'Network error. Please check your connection.');
    } finally {
        setLoading('btn1', false);
    }
}

/* ─── STEP 2: OTP boxes ─────────────────────────────────── */

// Auto-advance and backspace handling
document.querySelectorAll('.otp-box').forEach((box, idx) => {
    box.addEventListener('input', e => {
        const v = e.target.value.replace(/\D/g, '');
        e.target.value = v.slice(-1);
        e.target.classList.toggle('filled', !!v);
        e.target.classList.remove('is-error');
        clearFieldErr('otpErr');
        if (v && idx < 5) document.getElementById('otp' + (idx + 1)).focus();
    });

    box.addEventListener('keydown', e => {
        if (e.key === 'Backspace' && !box.value && idx > 0) {
            document.getElementById('otp' + (idx - 1)).focus();
        }
    });

    // Paste support — spread digits across boxes
    box.addEventListener('paste', e => {
        e.preventDefault();
        const paste = (e.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6);
        paste.split('').forEach((ch, i) => {
            const target = document.getElementById('otp' + i);
            if (target) {
                target.value = ch;
                target.classList.add('filled');
            }
        });
        const lastFilled = Math.min(paste.length, 5);
        document.getElementById('otp' + lastFilled).focus();
    });
});

function getOtpValue() {
    return [0,1,2,3,4,5].map(i => document.getElementById('otp' + i).value).join('');
}

function shakeOtpBoxes() {
    document.querySelectorAll('.otp-box').forEach(b => {
        b.classList.remove('is-error');
        void b.offsetWidth;   // reflow to restart animation
        b.classList.add('is-error');
    });
}

/* ─── STEP 2: Verify OTP ────────────────────────────────── */
async function verifyOtp() {
    clearAlert(2);
    clearFieldErr('otpErr');

    const otp   = getOtpValue();
    const email = document.getElementById('emailInput').value.trim();

    if (otp.length < 6) {
        shakeOtpBoxes();
        showFieldErr('otpErr', 'Enter all 6 digits');
        return;
    }

    setLoading('btn2', true);

    try {
        const res  = await fetch(`${API_BASE}/api/v1/auth/verify-otp`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ email, otp }),
        });

        const data = await res.json().catch(() => ({}));

        if (res.ok && data.resetToken) {
            resetToken = data.resetToken;
            stopTimer();
            goToStep(3);
        } else {
            shakeOtpBoxes();
            showAlert(2, data.message || 'Invalid or expired OTP.');
        }
    } catch (err) {
        showAlert(2, 'Network error. Please check your connection.');
    } finally {
        setLoading('btn2', false);
    }
}

/* ─── Countdown timer ───────────────────────────────────── */
function startTimer() {
    timerSeconds = 60;
    document.getElementById('timerCount').textContent = timerSeconds;
    document.getElementById('timerLabel').style.display  = '';
    document.getElementById('resendBtn').classList.remove('visible');

    clearInterval(timerInterval);
    timerInterval = setInterval(() => {
        timerSeconds--;
        document.getElementById('timerCount').textContent = timerSeconds;
        if (timerSeconds <= 0) {
            stopTimer();
            document.getElementById('timerLabel').style.display = 'none';
            document.getElementById('resendBtn').classList.add('visible');
        }
    }, 1000);
}

function stopTimer() { clearInterval(timerInterval); }

async function resendOtp() {
    clearAlert(2);
    const email   = document.getElementById('emailInput').value.trim();
    const channel = getChannel();
    const phone   = document.getElementById('phoneInput').value.trim();

    const btn = document.getElementById('resendBtn');
    btn.textContent = 'Sending…';
    btn.disabled = true;

    try {
        const body = { email, channel };
        if (channel === 'SMS') body.phone = phone;

        const res = await fetch(`${API_BASE}/api/v1/auth/forgot-password`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body),
        });

        if (res.ok) {
            showAlert(2, 'A new OTP has been sent!', 'success');
            // Clear boxes
            [0,1,2,3,4,5].forEach(i => {
                const b = document.getElementById('otp' + i);
                b.value = '';
                b.classList.remove('filled');
            });
            document.getElementById('otp0').focus();
            startTimer();
        } else {
            const data = await res.json().catch(() => ({}));
            showAlert(2, data.message || 'Failed to resend OTP.');
            btn.classList.remove('visible');
            document.getElementById('timerLabel').style.display = '';
            startTimer();
        }
    } catch {
        showAlert(2, 'Network error.');
    } finally {
        btn.textContent = 'Resend OTP';
        btn.disabled = false;
    }
}

/* ─── STEP 3: Password strength meter ──────────────────────*/
function updateStrength(val) {
    let score = 0;
    if (val.length >= 8)            score++;
    if (/[A-Z]/.test(val))         score++;
    if (/[0-9]/.test(val))         score++;
    if (/[^A-Za-z0-9]/.test(val))  score++;

    const labels = ['', 'Weak', 'Fair', 'Strong', 'Very Strong'];
    const cls    = ['', 'weak', 'fair', 'strong', 'strong'];

    const lbl = document.getElementById('pwdLbl');
    lbl.textContent = val.length ? labels[score] : '';
    lbl.className   = 'pwd-lbl ' + (val.length ? cls[score] : '');

    for (let i = 0; i < 4; i++) {
        const bar = document.getElementById('bar' + i);
        bar.className = 'pwd-bar';
        if (i < score) bar.classList.add(cls[score]);
    }
}

/* ─── STEP 3: Reset password ────────────────────────────── */
async function resetPassword() {
    clearAlert(3);
    clearFieldErr('pwdErr');
    clearFieldErr('confirmErr');

    const newPwd     = document.getElementById('newPwd').value;
    const confirmPwd = document.getElementById('confirmPwd').value;

    if (!newPwd)              { showFieldErr('pwdErr', 'Password is required'); return; }
    if (newPwd.length < 8)    { showFieldErr('pwdErr', 'Must be at least 8 characters'); return; }
    if (newPwd !== confirmPwd){ showFieldErr('confirmErr', 'Passwords do not match'); return; }
    if (!resetToken)          { showAlert(3, 'Session expired. Please start over.'); return; }

    setLoading('btn3', true);

    try {
        const res = await fetch(`${API_BASE}/api/v1/auth/reset-password`, {
            method:  'POST',
            headers: {
                'Content-Type':  'application/json',
                'Authorization': 'Bearer ' + resetToken,
            },
            body: JSON.stringify({ newPassword: newPwd }),
        });

        const data = await res.json().catch(() => ({}));

        if (res.ok) {
            goToStep(4);
            startRedirectCountdown();
        } else {
            showAlert(3, data.message || 'Failed to reset password. Please try again.');
        }
    } catch (err) {
        showAlert(3, 'Network error. Please check your connection.');
    } finally {
        setLoading('btn3', false);
    }
}

/* ─── STEP 4: Auto-redirect countdown ──────────────────── */
function startRedirectCountdown() {
    let count = 5;
    const el  = document.getElementById('redirectCount');
    const iv  = setInterval(() => {
        count--;
        if (el) el.textContent = count;
        if (count <= 0) {
            clearInterval(iv);
            window.location.href = 'signin.html';
        }
    }, 1000);
}

/* ─── Show / hide password toggle ───────────────────────── */
function toggleEye(inputId, btn) {
    const input = document.getElementById(inputId);
    const isText = input.type === 'text';
    input.type = isText ? 'password' : 'text';
    btn.innerHTML = isText
        ? '<i class="fa-regular fa-eye"></i>'
        : '<i class="fa-regular fa-eye-slash"></i>';
}