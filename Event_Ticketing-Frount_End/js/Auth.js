var AUTH = (function () {

    var API_BASE = 'http://localhost:8080/api/v1/auth';

    var SWAL_THEME = {
        confirmButtonColor: '#7C3AED',
        background: '#ffffff',
        color: '#1a1a2e',
        customClass: {
            popup:         'eh-swal-popup',
            confirmButton: 'eh-swal-btn',
            cancelButton:  'eh-swal-btn-cancel'
        }
    };

    function alertError(title, text) {
        Swal.fire(Object.assign({}, SWAL_THEME, {
            icon:  'error',
            title: title,
            text:  text
        }));
    }

    function alertSuccess(title, text, callback) {
        Swal.fire(Object.assign({}, SWAL_THEME, {
            icon:              'success',
            title:             title,
            text:              text,
            timer:             2200,
            timerProgressBar:  true,
            showConfirmButton: false
        })).then(function () {
            if (callback) callback();
        });
    }

    function alertNetworkError() {
        alertError('Server Unreachable', 'Cannot connect to the server. Make sure the backend is running on port 8080.');
    }

    function setErr(inputEl, errId, fail) {
        if (!inputEl) return false;
        inputEl.classList.toggle('is-error', fail);
        var errEl = document.getElementById(errId);
        if (errEl) errEl.classList.toggle('on', fail);
        return fail;
    }

    function makeEye(btnId, inputId) {
        var btn = document.getElementById(btnId);
        if (!btn) return;
        btn.addEventListener('click', function () {
            var inp = document.getElementById(inputId);
            if (!inp) return;
            var ico = this.querySelector('i');
            inp.type      = inp.type === 'password' ? 'text' : 'password';
            ico.className = inp.type === 'text' ? 'far fa-eye-slash' : 'far fa-eye';
        });
    }

    function clearOnInput(inputId, errId) {
        var el = document.getElementById(inputId);
        if (!el) return;
        el.addEventListener('input', function () {
            this.classList.remove('is-error');
            var errEl = document.getElementById(errId);
            if (errEl) errEl.classList.remove('on');
        });
    }

    function storeToken(token, username, rememberMe) {
        var storage = rememberMe ? localStorage : sessionStorage;
        storage.setItem('eh_token', token);
        storage.setItem('eh_user',  username);
    }

    function initSignIn() {

        makeEye('si-eye-btn', 'si-pwd');
        clearOnInput('si-email', 'err-email');
        clearOnInput('si-pwd',   'err-pwd');

        var form = document.getElementById('signinForm');
        if (!form) return;
// sign in
        form.addEventListener('submit', function (e) {
            e.preventDefault();

            var emailEl = document.getElementById('si-email');
            var pwdEl   = document.getElementById('si-pwd');
            var btn     = document.getElementById('signinBtn');
            var valid   = true;

            if (setErr(emailEl, 'err-email', !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailEl.value.trim()))) valid = false;
            if (setErr(pwdEl,   'err-pwd',   pwdEl.value.length < 6))                                    valid = false;
            if (!valid) return;

            btn.classList.add('loading');

            var payload = {
                username: emailEl.value.trim(),
                password: pwdEl.value
            };

            fetch(API_BASE + '/signIn', {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify(payload)
            })
                .then(function (res) { return res.json(); })
                .then(function (body) {
                    btn.classList.remove('loading');

                    if (body.status === 200 && body.data && body.data.access_token) {

                        var rememberEl = document.getElementById('si-remember');
                        storeToken(
                            body.data.access_token,
                            payload.username,
                            rememberEl ? rememberEl.checked : false
                        );

                        var role = body.data.role || 'USER';
                        var redirectUrl = (role === 'ADMIN')
                            ? '../Pages/Event.html'
                            : '../index.html';

                        alertSuccess(
                            'Welcome back! 👋',
                            'Signed in successfully. Redirecting…',
                            function () { window.location.href = redirectUrl; }
                        );

                    } else {
                        var msg = 'Sign in failed. Please try again.';
                        if (body.status === 404) msg = 'No account found for this email address.';
                        if (body.status === 401) msg = 'Incorrect email or password. Please try again.';
                        alertError('Sign In Failed', msg);
                    }
                })
                .catch(function () {
                    btn.classList.remove('loading');
                    alertNetworkError();
                });
        });
    }

    function initSignUp() {

        document.querySelectorAll('.type-card').forEach(function (card) {
            card.addEventListener('click', function () {
                document.querySelectorAll('.type-card').forEach(function (c) { c.classList.remove('selected'); });
                this.classList.add('selected');
                var radio = this.querySelector('input[type="radio"]');
                if (radio) radio.checked = true;
            });
        });

        makeEye('su-eye-btn',         'su-pwd');
        makeEye('su-confirm-eye-btn', 'su-confirm');

        var pwdInput = document.getElementById('su-pwd');
        if (pwdInput) {
            pwdInput.addEventListener('input', function () {
                _updateStrength(this.value);
                this.classList.remove('is-error');
                var errEl = document.getElementById('err-pwd');
                if (errEl) errEl.classList.remove('on');
            });
        }

        var clearMap = {
            'su-first':   'err-first',
            'su-last':    'err-last',
            'su-email':   'err-email',
            'su-confirm': 'err-confirm'
        };
        Object.keys(clearMap).forEach(function (id) {
            clearOnInput(id, clearMap[id]);
        });

        var form = document.getElementById('signupForm');
        if (!form) return;
//sign up
        form.addEventListener('submit', function (e) {
            e.preventDefault();

            var first   = document.getElementById('su-first');
            var last    = document.getElementById('su-last');
            var email   = document.getElementById('su-email');
            var pwd     = document.getElementById('su-pwd');
            var confirm = document.getElementById('su-confirm');
            var terms   = document.getElementById('su-terms');
            var btn     = document.getElementById('signupBtn');
            var valid   = true;

            if (setErr(first,   'err-first',   !first.value.trim()))                                     valid = false;
            if (setErr(last,    'err-last',    !last.value.trim()))                                      valid = false;
            if (setErr(email,   'err-email',   !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()))) valid = false;
            if (setErr(pwd,     'err-pwd',     pwd.value.length < 8))                                    valid = false;
            if (setErr(confirm, 'err-confirm', pwd.value !== confirm.value))                             valid = false;

            if (terms && !terms.checked) {
                terms.style.outline = '2px solid var(--danger)';
                valid = false;
            } else if (terms) {
                terms.style.outline = '';
            }

            if (!valid) {
                Swal.fire(Object.assign({}, SWAL_THEME, {
                    icon:  'warning',
                    title: 'Check your details',
                    text:  'Please fix the highlighted fields before continuing.'
                }));
                return;
            }

            _advanceProgress();
            btn.classList.add('loading');

            var payload = {
                username: email.value.trim(),
                password: pwd.value,
                role:     'USER'
            };

            fetch(API_BASE + '/signUp', {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify(payload)
            })
                .then(function (res) { return res.json(); })
                .then(function (body) {
                    btn.classList.remove('loading');

                    if (body.status === 200) {
                        Swal.fire(Object.assign({}, SWAL_THEME, {
                            icon:               'success',
                            title:              'Account Created! 🎉',
                            html:               'Welcome, <strong>' + first.value.trim() + '</strong>!<br>You can now sign in and explore events.',
                            confirmButtonText:  'Sign In Now',
                            confirmButtonColor: '#7C3AED'
                        })).then(function () {
                            window.location.href = 'Signin.html';
                        });

                    } else {
                        _resetProgress();
                        alertError('Registration Failed', body.data || body.message || 'Something went wrong. Please try again.');
                    }
                })
                .catch(function () {
                    btn.classList.remove('loading');
                    _resetProgress();
                    alertNetworkError();
                });
        });
    }

    function _advanceProgress() {
        var ps1 = document.getElementById('ps1');
        var ps2 = document.getElementById('ps2');
        var ps3 = document.getElementById('ps3');
        if (!ps1 || !ps2 || !ps3) return;
        ps1.classList.add('done');
        ps1.classList.remove('active');
        ps2.classList.add('active');
        setTimeout(function () {
            ps2.classList.add('done');
            ps2.classList.remove('active');
            ps3.classList.add('active');
        }, 700);
    }

    function _resetProgress() {
        var ps1 = document.getElementById('ps1');
        var ps2 = document.getElementById('ps2');
        var ps3 = document.getElementById('ps3');
        if (!ps1 || !ps2 || !ps3) return;
        ps1.classList.remove('done');
        ps1.classList.add('active');
        ps2.classList.remove('done', 'active');
        ps3.classList.remove('done', 'active');
    }

    function _updateStrength(val) {
        var wrap = document.getElementById('pwdStrength');
        var lbl  = document.getElementById('pwdLbl');
        if (!wrap || !lbl) return;
        if (!val) { wrap.style.display = 'none'; return; }
        wrap.style.display = 'block';

        var score = 0;
        if (val.length >= 8)           score++;
        if (/[A-Z]/.test(val))         score++;
        if (/[0-9]/.test(val))         score++;
        if (/[^A-Za-z0-9]/.test(val))  score++;

        var cls  = ['', 'weak', 'fair', 'fair', 'strong'][score];
        var text = ['Too short', 'Weak', 'Fair', 'Good', 'Strong'][score];

        [1, 2, 3, 4].forEach(function (i) {
            var bar = document.getElementById('pb' + i);
            if (bar) bar.className = 'pwd-bar' + (i <= score && cls ? ' ' + cls : '');
        });
        lbl.className   = 'pwd-lbl' + (cls ? ' ' + cls : '');
        lbl.textContent = text;
    }

    return {
        initSignIn: initSignIn,
        initSignUp: initSignUp
    };

})();