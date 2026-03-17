var API_URL = 'http://localhost:8080/api/v1/coupon';

function getToken() {
    return localStorage.getItem('eh_token') || sessionStorage.getItem('eh_token') || '';
}

function authHeaders() {
    return {
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + getToken()
    };
}

var SWAL = {
    success: function (title, text) {
        return Swal.fire({
            icon:              'success',
            title:             title,
            text:              text,
            timer:             2000,
            timerProgressBar:  true,
            showConfirmButton: false,
            background:        '#ffffff',
            color:             '#1a1a2e',
            iconColor:         '#7C3AED',
            customClass:       { popup: 'eh-swal-popup' }
        });
    },

    error: function (title, text) {
        return Swal.fire({
            icon:               'error',
            title:              title,
            text:               text,
            confirmButtonText:  'OK',
            confirmButtonColor: '#7C3AED',
            background:         '#ffffff',
            color:              '#1a1a2e',
            customClass:        { popup: 'eh-swal-popup', confirmButton: 'eh-swal-btn' }
        });
    },

    warning: function (title, text) {
        return Swal.fire({
            icon:               'warning',
            title:              title,
            text:               text,
            confirmButtonText:  'Got it',
            confirmButtonColor: '#7C3AED',
            background:         '#ffffff',
            color:              '#1a1a2e',
            customClass:        { popup: 'eh-swal-popup', confirmButton: 'eh-swal-btn' }
        });
    },

    confirm: function (title, text, confirmText) {
        return Swal.fire({
            icon:               'warning',
            title:              title,
            text:               text,
            showCancelButton:   true,
            confirmButtonText:  confirmText || 'Yes',
            confirmButtonColor: '#e53e3e',
            cancelButtonText:   'Cancel',
            cancelButtonColor:  '#7C3AED',
            background:         '#ffffff',
            color:              '#1a1a2e',
            reverseButtons:     true,
            customClass:        { popup: 'eh-swal-popup' }
        });
    },

    loading: function (title, text) {
        Swal.fire({
            title:             title,
            text:              text,
            allowOutsideClick: false,
            allowEscapeKey:    false,
            showConfirmButton: false,
            background:        '#ffffff',
            color:             '#1a1a2e',
            customClass:       { popup: 'eh-swal-popup' },
            didOpen:           function () { Swal.showLoading(); }
        });
    },

    toast: function (title, icon) {
        Swal.fire({
            toast:             true,
            position:          'top-end',
            icon:              icon || 'info',
            title:             title,
            showConfirmButton: false,
            timer:             2500,
            timerProgressBar:  true,
            background:        '#ffffff',
            color:             '#1a1a2e',
            iconColor:         '#7C3AED'
        });
    }
};

function getFormValues() {
    return {
        coupon_id:       parseInt($('#f_couponId').val()) || 0,
        couponCode:     $('#f_code').val().trim().toUpperCase(),
        discount_type:   $('#f_discountType').val(),
        discount_value:  parseInt($('#f_discountValue').val()) || 0,
        expiration_date: $('#f_expiryDate').val(),
        usage_limit:     parseInt($('#f_usageLimit').val()) || 0,
        used_count:      parseInt($('#f_usedCount').val()) || 0
    };
}

function clearCouponForm() {
    $('#f_couponId').val('');
    $('#f_code').val('');
    $('#f_discountType').val('Percentage');
    $('#f_discountValue').val('');
    $('#f_expiryDate').val('');
    $('#f_usageLimit').val('');
    $('#f_usedCount').val('');
    $('#btnUpdate').prop('disabled', true);
    $('#btnDelete').prop('disabled', true);
}

function handleAuthError(status) {
    if (status === 401 || status === 403) {
        SWAL.error(
            'Session Expired',
            'Your session has expired or you are not authorised. Please sign in again.'
        ).then(function () {
            window.location.href = '../login/Signin.html';
        });
        return true;
    }
    return false;
}

function saveCoupon() {
    var data = getFormValues();

    if (!data.couponCode || !data.expiration_date) {
        SWAL.warning('Missing Fields', 'Coupon Code and Expiry Date are required before saving.');
        return;
    }

    SWAL.loading('Saving Coupon…', 'Please wait while we save the coupon.');

    $.ajax({
        url:     API_URL,
        method:  'POST',
        headers: authHeaders(),
        data:    JSON.stringify(data),
        success: function (res) {
            SWAL.success('Coupon Saved! 🎉', '"' + data.couponCode + '" has been saved successfully.');
            clearCouponForm();
            getAllCoupons();
        },
        error: function (err) {
            console.error('saveCoupon error:', err);
            if (handleAuthError(err.status)) return;
            var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while saving coupon.';
            SWAL.error('Save Failed', msg);
        }
    });
}

function updateCoupon() {
    var data = getFormValues();

    if (!data.coupon_id) {
        SWAL.warning('No Coupon Selected', 'Please click ✏️ Edit on a row in the table to select a coupon to update.');
        return;
    }

    SWAL.loading('Updating Coupon…', 'Applying your changes…');

    $.ajax({
        url:     API_URL,
        method:  'PUT',
        headers: authHeaders(),
        data:    JSON.stringify(data),
        success: function (res) {
            SWAL.success('Coupon Updated! ✏️', '"' + data.couponCode + '" has been updated successfully.');
            clearCouponForm();
            getAllCoupons();
        },
        error: function (err) {
            console.error('updateCoupon error:', err);
            if (handleAuthError(err.status)) return;
            var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while updating coupon.';
            SWAL.error('Update Failed', msg);
        }
    });
}

function deleteCoupon() {
    var data = getFormValues();

    if (!data.coupon_id) {
        SWAL.warning('No Coupon Selected', 'Please click ✏️ Edit on a row in the table to select a coupon to delete.');
        return;
    }

    SWAL.confirm(
        'Delete Coupon?',
        'You are about to permanently delete "' + $('#f_code').val() + '". This cannot be undone.',
        '🗑 Yes, Delete'
    ).then(function (result) {
        if (!result.isConfirmed) return;

        SWAL.loading('Deleting…', 'Removing the coupon from the system.');

        $.ajax({
            url:     API_URL,
            method:  'DELETE',
            headers: authHeaders(),
            data:    JSON.stringify({ coupon_id: data.coupon_id }),
            success: function (res) {
                SWAL.success('Deleted!', '"' + data.couponCode + '" has been removed successfully.');
                clearCouponForm();
                getAllCoupons();
            },
            error: function (err) {
                console.error('deleteCoupon error:', err);
                if (handleAuthError(err.status)) return;
                var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while deleting coupon.';
                SWAL.error('Delete Failed', msg);
            }
        });
    });
}

function getAllCoupons() {
    $('#couponsTableBody').empty();

    $.ajax({
        url:     API_URL,
        method:  'GET',
        headers: authHeaders(),
        success: function (res) {
            console.log('getAllCoupons response:', res);

            var coupons = Array.isArray(res) ? res : (res.data || []);

            if (coupons.length === 0) {
                $('#couponsTableBody').append(
                    '<tr><td colspan="8"><div class="tbl-empty">No coupons found. Fill the form and click 💾 Save Record.</div></td></tr>'
                );
                return;
            }

            for (var i = 0; i < coupons.length; i++) {
                var c       = coupons[i];
                var st      = getCouponStatus(c.expiration_date, c.used_count, c.usage_limit);
                var pct     = getUsagePct(c.used_count, c.usage_limit);
                var typeCls = c.discount_type === 'Percentage' ? 'type-pct' : 'type-fixed';
                var typeLbl = c.discount_type === 'Percentage' ? '%' : 'LKR';

                var row =
                    '<tr' +
                    ' data-id="'       + c.coupon_id      + '"' +
                    ' data-code="'     + c.couponCode     + '"' +
                    ' data-type="'     + c.discount_type   + '"' +
                    ' data-value="'    + c.discount_value  + '"' +
                    ' data-expiry="'   + c.expiration_date + '"' +
                    ' data-limit="'    + c.usage_limit     + '"' +
                    ' data-used="'     + c.used_count      + '"' +
                    ' style="cursor:pointer;">' +
                    '<td class="muted">'  + c.coupon_id + '</td>' +
                    '<td><strong>'        + c.couponCode + '</strong></td>' +
                    '<td><span class="type-chip ' + typeCls + '">' + c.discount_type + '</span></td>' +
                    '<td><strong>'        + c.discount_value + ' ' + typeLbl + '</strong></td>' +
                    '<td class="muted">'  + formatDate(c.expiration_date) + '</td>' +
                    '<td>' +
                    '<div class="usage-bar-wrap">' +
                    '<div class="usage-bar"><div class="usage-fill" style="width:' + pct + '%"></div></div>' +
                    '<span class="usage-txt">' + c.used_count + ' / ' + c.usage_limit + '</span>' +
                    '</div>' +
                    '</td>' +
                    '<td><span class="cpn-badge ' + st.cls + '">' + st.label + '</span></td>' +
                    '<td><div class="tbl-actions">' +
                    '<button class="tbl-btn tbl-btn-edit" onclick="editRow(this)">✏️ Edit</button>' +
                    '<button class="tbl-btn tbl-btn-del"  onclick="deleteRowBtn(this)">🗑 Delete</button>' +
                    '</div></td>' +
                    '</tr>';
                $('#couponsTableBody').append(row);
            }
        },
        error: function (err) {
            console.error('getAllCoupons error:', err);
            if (handleAuthError(err.status)) return;
            SWAL.error('Failed to Load Coupons', 'Could not retrieve coupons from the server. Make sure the backend is running on port 8080.');
        }
    });
}

function editRow(btn) {
    var tr = $(btn).closest('tr');
    $('#f_couponId').val(tr.data('id'));
    $('#f_code').val(tr.data('code'));
    $('#f_discountType').val(tr.data('type'));
    $('#f_discountValue').val(tr.data('value'));
    $('#f_expiryDate').val(tr.data('expiry'));
    $('#f_usageLimit').val(tr.data('limit'));
    $('#f_usedCount').val(tr.data('used'));
    $('#btnUpdate').prop('disabled', false);
    $('#btnDelete').prop('disabled', false);

    SWAL.toast('Editing: ' + tr.data('code'), 'info');
    $('html, body').animate({ scrollTop: 0 }, 400);
}

function deleteRowBtn(btn) {
    var tr   = $(btn).closest('tr');
    var id   = tr.data('id');
    var code = tr.data('code');

    SWAL.confirm(
        'Delete Coupon?',
        'You are about to permanently delete "' + code + '". This cannot be undone.',
        '🗑 Yes, Delete'
    ).then(function (result) {
        if (!result.isConfirmed) return;

        SWAL.loading('Deleting…', 'Removing the coupon from the system.');

        $.ajax({
            url:     API_URL,
            method:  'DELETE',
            headers: authHeaders(),
            data:    JSON.stringify({ coupon_id: id }),
            success: function (res) {
                SWAL.success('Deleted!', '"' + code + '" has been removed successfully.');
                clearCouponForm();
                getAllCoupons();
            },
            error: function (err) {
                console.error('deleteRowBtn error:', err);
                if (handleAuthError(err.status)) return;
                var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while deleting coupon.';
                SWAL.error('Delete Failed', msg);
            }
        });
    });
}

function getCouponStatus(expiryDate, usedCount, usageLimit) {
    var today  = new Date(); today.setHours(0, 0, 0, 0);
    var expiry = new Date(expiryDate);
    if (expiry < today)              return { label: 'Expired',   cls: 'badge-expired' };
    if (usedCount >= usageLimit)     return { label: 'Maxed Out', cls: 'badge-maxed'   };
    return                                  { label: 'Active',    cls: 'badge-active'  };
}

function getUsagePct(usedCount, usageLimit) {
    if (!usageLimit) return 0;
    return Math.min(100, Math.round((usedCount / usageLimit) * 100));
}

function formatDate(d) {
    if (!d) return '—';
    var parts = String(d).split('-');
    if (parts.length < 3) return d;
    return parts[2] + '/' + parts[1] + '/' + parts[0];
}

$(document).ready(function () {
    if (!getToken()) {
        Swal.fire({
            icon:               'warning',
            title:              'Not Signed In',
            text:               'You need to sign in before accessing this page.',
            confirmButtonText:  'Go to Sign In',
            confirmButtonColor: '#7C3AED',
            allowOutsideClick:  false,
            background:         '#ffffff',
            color:              '#1a1a2e'
        }).then(function () {
            window.location.href = '../login/Signin.html';
        });
        return;
    }

    getAllCoupons();

    $('#btnSave').click(saveCoupon);
    $('#btnUpdate').click(updateCoupon);
    $('#btnDelete').click(deleteCoupon);
});