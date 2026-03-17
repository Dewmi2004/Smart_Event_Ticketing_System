var BOOKING_API = 'http://localhost:8080/api/v1/booking';

function getToken() {
    return localStorage.getItem('eh_token') || sessionStorage.getItem('eh_token') || '';
}
function authHeaders() {
    return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + getToken() };
}

var SWAL = {
    success: function(t,x,cb){ return Swal.fire({ icon:'success', title:t, text:x, timer:2200, timerProgressBar:true, showConfirmButton:false, background:'#ffffff', color:'#1a1a2e', iconColor:'#7C3AED' }).then(function(){ if(cb) cb(); }); },
    error:   function(t,x)   { return Swal.fire({ icon:'error',   title:t, text:x, confirmButtonColor:'#7C3AED', background:'#ffffff', color:'#1a1a2e' }); },
    warning: function(t,x)   { return Swal.fire({ icon:'warning', title:t, text:x, confirmButtonText:'Got it', confirmButtonColor:'#7C3AED', background:'#ffffff', color:'#1a1a2e' }); },
    loading: function(t,x)   { Swal.fire({ title:t, text:x, allowOutsideClick:false, allowEscapeKey:false, showConfirmButton:false, background:'#ffffff', color:'#1a1a2e', didOpen:function(){ Swal.showLoading(); } }); },
    toast:   function(t,i)   { Swal.fire({ toast:true, position:'top-end', icon:i||'info', title:t, showConfirmButton:false, timer:2500, timerProgressBar:true, background:'#ffffff', color:'#1a1a2e', iconColor:'#7C3AED' }); }
};

function clearBookingForm() {
    $('#f_bookingId').val('');
    $('#f_userId').val('');
    $('#f_userEmail').val('');
    $('#f_eventId').val('');
    $('#f_eventName').val('');
    $('#f_eventLocation').val('');
    $('#f_bookingDate').val('');
    $('#f_totalAmount').val('');
    $('#f_couponCode').val('');
    $('#f_seatIds').val('');
    $('#f_seatNumbers').val('');
    $('#f_status').val('Pending');
    sessionStorage.removeItem('pendingBooking');
}

function fillFromSession() {
    var raw = sessionStorage.getItem('pendingBooking');
    if (!raw) return;
    try {
        var data = JSON.parse(raw);
        $('#f_eventId').val(data.eventId        || '');
        $('#f_eventName').val(data.event_name     || '');
        $('#f_eventLocation').val(data.event_location || '');
        $('#f_bookingDate').val(data.event_date   || new Date().toISOString().split('T')[0]);
        $('#f_totalAmount').val(data.total_amount || 0);
        $('#f_couponCode').val(data.couponCode   || '');
        $('#f_seatIds').val((data.seat_ids || []).join(', '));
        $('#f_seatNumbers').val(data.seat_numbers || '');
        $('#f_status').val('Pending');

        var user = localStorage.getItem('eh_user') || sessionStorage.getItem('eh_user') || '';
        $('#f_userEmail').val(user);

        SWAL.toast('Booking details loaded from seat selection', 'success');
    } catch(e) {
        console.error('Failed to parse pendingBooking:', e);
    }
}

function bookNow() {
    var userId    = parseInt($('#f_userId').val()) || 0;
    var userEmail = $('#f_userEmail').val().trim();
    var eventId   = parseInt($('#f_eventId').val()) || 0;
    var total     = parseFloat($('#f_totalAmount').val()) || 0;
    var coupon    = $('#f_couponCode').val().trim();
    var seatIdsRaw = $('#f_seatIds').val().trim();

    if (!userId)     { SWAL.warning('Missing User ID',  'Please enter your User ID.'); return; }
    if (!userEmail)  { SWAL.warning('Missing Email',    'Please enter your email address to receive the e-ticket.'); return; }
    if (!eventId)    { SWAL.warning('Missing Event',    'No event selected. Please go back to Seat selection.'); return; }
    if (!seatIdsRaw) { SWAL.warning('No Seats',         'No seats selected. Please go back to Seat selection.'); return; }

    var seatIds = seatIdsRaw.split(',')
        .map(function(s){ return parseInt(s.trim()); })
        .filter(function(n){ return !isNaN(n) && n > 0; });

    if (seatIds.length === 0) { SWAL.warning('Invalid Seats', 'Could not read seat IDs. Please go back and reselect.'); return; }

    SWAL.loading('Creating Booking…', 'Locking your seats and sending confirmation email…');

    /*
     * BookingDto fields (camelCase — matches the entity):
     *   userId, userEmail, eventId, totalAmount, couponCode, seatIds, status
     */
    var payload = {
        userId:      userId,
        userEmail:   userEmail,
        eventId:     eventId,
        totalAmount: total,
        couponCode:  coupon || null,
        seatIds:     seatIds,
        status:      'Pending'
    };

    $.ajax({
        url: BOOKING_API, method: 'POST', headers: authHeaders(),
        data: JSON.stringify(payload),
        success: function(res) {
            var booking = res.data || {};
            Swal.fire({
                icon:               'success',
                title:              'Booking Created! 🎉',
                html:               '<p>Booking <strong>#' + booking.bookingId + '</strong> is confirmed.</p>' +
                    '<p style="color:#7a8899;font-size:13px;margin-top:8px;">Status: <strong>Pending</strong> — complete payment to confirm entry.</p>' +
                    '<p style="color:#7a8899;font-size:12px;margin-top:4px;">A confirmation email with your QR e-ticket has been sent to <strong>' + userEmail + '</strong>.</p>',
                confirmButtonText:  'Go to Payment',
                confirmButtonColor: '#7C3AED',
                background:         '#ffffff',
                color:              '#1a1a2e'
            }).then(function() {
                sessionStorage.removeItem('pendingBooking');
                window.location.href = 'Payment.html';
            });
        },
        error: function(err) {
            var msg = err.responseJSON ? (err.responseJSON.data || err.responseJSON.message) : 'Server error while creating booking.';
            SWAL.error('Booking Failed', msg);
        }
    });
}

function getAllBookings() {
    $('#bookingsTableBody').empty();

    $.ajax({
        url: BOOKING_API, method: 'GET', headers: authHeaders(),
        success: function(res) {
            var bookings = Array.isArray(res) ? res : (res.data || []);
            if (bookings.length === 0) {
                $('#bookingsTableBody').append('<tr><td colspan="8" class="tbl-empty">No bookings found.</td></tr>');
                return;
            }
            bookings.forEach(function(b) {
                var statusCls = b.status === 'Confirmed' ? 'badge-active'
                    : b.status === 'Pending'   ? 'badge-upcoming'
                        : b.status === 'Cancelled' ? 'badge-cancelled'
                            : 'badge-completed';
                var row =
                    '<tr' +
                    ' data-id="'    + b.bookingId   + '"' +
                    ' data-userid="'+ b.userId      + '"' +
                    ' data-eventid="'+ b.eventId    + '"' +
                    ' data-amount="'+ b.totalAmount + '"' +
                    ' data-coupon="'+ (b.couponCode||'') + '"' +
                    ' data-status="'+ b.status      + '"' +
                    ' data-seats="' + (b.seatIds||[]).join(',') + '"' +
                    ' style="cursor:pointer;">' +
                    '<td class="muted">#' + b.bookingId   + '</td>' +
                    '<td class="muted">'  + b.userId      + '</td>' +
                    '<td><strong>'        + (b.eventName || b.eventId) + '</strong></td>' +
                    '<td class="muted">'  + formatDate(b.bookingDate) + '</td>' +
                    '<td>LKR '            + Number(b.totalAmount).toLocaleString() + '</td>' +
                    '<td class="muted">'  + (b.couponCode || '—') + '</td>' +
                    '<td><span class="status-badge ' + statusCls + '">' + b.status + '</span></td>' +
                    '<td><div class="tbl-actions">' +
                    '<button class="tbl-btn tbl-btn-delete" onclick="deleteBookingRow(this)">🗑 Cancel</button>' +
                    '</div></td>' +
                    '</tr>';
                $('#bookingsTableBody').append(row);
            });
        },
        error: function(err) {
            if (err.status === 401 || err.status === 403) return;
            SWAL.error('Load Failed', 'Could not load bookings from server.');
        }
    });
}

function deleteBookingRow(btn) {
    var tr = $(btn).closest('tr');
    var id = tr.data('id');
    Swal.fire({
        icon:'warning', title:'Cancel Booking?',
        text:'This will release the seats and cannot be undone.',
        showCancelButton:true, confirmButtonColor:'#e53e3e',
        cancelButtonColor:'#7C3AED', confirmButtonText:'Yes, Cancel',
        cancelButtonText:'Keep It', reverseButtons:true,
        background:'#ffffff', color:'#1a1a2e'
    }).then(function(result) {
        if (!result.isConfirmed) return;
        $.ajax({
            url: BOOKING_API + '/' + id, method: 'DELETE', headers: authHeaders(),
            success: function() {
                SWAL.success('Cancelled', 'Booking #' + id + ' cancelled and seats released.');
                getAllBookings();
            },
            error: function(err) {
                var msg = err.responseJSON ? (err.responseJSON.data || err.responseJSON.message) : 'Error cancelling booking.';
                SWAL.error('Cancel Failed', msg);
            }
        });
    });
}

function formatDate(d) {
    if (!d) return '—';
    var parts = String(d).split('-');
    if (parts.length < 3) return d;
    return parts[2] + '/' + parts[1] + '/' + parts[0];
}

$(document).ready(function() {
    if (!getToken()) {
        Swal.fire({
            icon:'warning', title:'Not Signed In',
            text:'You need to sign in before accessing this page.',
            confirmButtonText:'Go to Sign In', confirmButtonColor:'#7C3AED',
            allowOutsideClick:false, background:'#ffffff', color:'#1a1a2e'
        }).then(function(){ window.location.href = '../login/Signin.html'; });
        return;
    }
    fillFromSession();
    getAllBookings();
    $('#btnBookNow').click(bookNow);
});