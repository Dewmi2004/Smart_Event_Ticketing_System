var API_URL = 'http://localhost:8080/api/v1/event';

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
        eventId:      parseInt($('#f_eventId').val()) || 0,
        event_name:   $('#f_title').val().trim(),
        location:     $('#f_location').val().trim(),
        date:         $('#f_date').val(),
        time:         $('#f_time').val(),
        ticket_price: parseFloat($('#f_ticketPrice').val()) || 0,
        total_seats:  parseInt($('#f_totalSeats').val()) || 0,
        status:       $('#f_status').val()
    };
}

function clearEventForm() {
    $('#f_eventId').val('');
    $('#f_title').val('');
    $('#f_location').val('');
    $('#f_date').val('');
    $('#f_time').val('');
    $('#f_ticketPrice').val('');
    $('#f_totalSeats').val('');
    $('#f_status').val('Active');
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

function saveEvent() {
    var data = getFormValues();

    if (!data.event_name || !data.location || !data.date) {
        SWAL.warning('Missing Fields', 'Event Name, Location and Date are required before saving.');
        return;
    }

    SWAL.loading('Saving Event…', 'Please wait while we save the event.');

    $.ajax({
        url:     API_URL,
        method:  'POST',
        headers: authHeaders(),
        data:    JSON.stringify(data),
        success: function (res) {
            SWAL.success('Event Saved! 🎉', '"' + data.event_name + '" has been saved successfully.');
            clearEventForm();
            getAllEvents();
        },
        error: function (err) {
            console.error('saveEvent error:', err);
            if (handleAuthError(err.status)) return;
            var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while saving event.';
            SWAL.error('Save Failed', msg);
        }
    });
}

function updateEvent() {
    var data = getFormValues();

    // ✅ Fixed: was checking data.event_id (always undefined), now checks data.eventId
    if (!data.eventId) {
        SWAL.warning('No Event Selected', 'Please click ✏️ Edit on a row in the table below to select an event to update.');
        return;
    }

    SWAL.loading('Updating Event…', 'Applying your changes…');

    $.ajax({
        url:     API_URL,
        method:  'PUT',
        headers: authHeaders(),
        data:    JSON.stringify(data),
        success: function (res) {
            SWAL.success('Event Updated! ✏️', '"' + data.event_name + '" has been updated successfully.');
            clearEventForm();
            getAllEvents();
        },
        error: function (err) {
            console.error('updateEvent error:', err);
            if (handleAuthError(err.status)) return;
            var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while updating event.';
            SWAL.error('Update Failed', msg);
        }
    });
}

function deleteEvent() {
    var data = getFormValues();

    // ✅ Fixed: was checking data.event_id (always undefined), now checks data.eventId
    if (!data.eventId) {
        SWAL.warning('No Event Selected', 'Please click ✏️ Edit on a row in the table below to select an event to delete.');
        return;
    }

    SWAL.confirm(
        'Delete Event?',
        'You are about to permanently delete "' + $('#f_title').val() + '". This action cannot be undone.',
        '🗑 Yes, Delete'
    ).then(function (result) {
        if (!result.isConfirmed) return;

        SWAL.loading('Deleting…', 'Removing the event from the system.');

        // ✅ Fixed: ID now in URL path, no request body needed
        $.ajax({
            url:     API_URL + '/' + data.eventId,
            method:  'DELETE',
            headers: authHeaders(),
            success: function (res) {
                SWAL.success('Deleted!', '"' + data.event_name + '" has been removed successfully.');
                clearEventForm();
                getAllEvents();
            },
            error: function (err) {
                console.error('deleteEvent error:', err);
                if (handleAuthError(err.status)) return;
                var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while deleting event.';
                SWAL.error('Delete Failed', msg);
            }
        });
    });
}

function getAllEvents() {
    $('#eventsTableBody').empty();

    $.ajax({
        url:     API_URL,
        method:  'GET',
        headers: authHeaders(),
        success: function (res) {
            console.log('getAllEvents response:', res);

            var events = Array.isArray(res) ? res : (res.data || []);

            if (events.length === 0) {
                $('#eventsTableBody').append(
                    '<tr><td colspan="9"><div class="tbl-empty">No events found. Fill the form and click 💾 Save Event.</div></td></tr>'
                );
                return;
            }

            for (var i = 0; i < events.length; i++) {
                var e   = events[i];
                var row =
                    '<tr' +
                    ' data-id="'       + e.eventId      + '"' +
                    ' data-name="'     + e.event_name   + '"' +
                    ' data-location="' + e.location     + '"' +
                    ' data-date="'     + e.date         + '"' +
                    ' data-time="'     + e.time         + '"' +
                    ' data-price="'    + e.ticket_price + '"' +
                    ' data-seats="'    + e.total_seats  + '"' +
                    ' data-status="'   + e.status       + '"' +
                    ' style="cursor:pointer;">' +
                    '<td class="muted">' + e.eventId    + '</td>' +
                    '<td><strong>'       + e.event_name + '</strong></td>' +
                    '<td class="muted">' + e.location   + '</td>' +
                    '<td class="muted">' + formatDate(e.date) + '</td>' +
                    '<td class="muted">' + e.time       + '</td>' +
                    '<td>LKR '           + Number(e.ticket_price).toLocaleString() + '</td>' +
                    '<td class="muted">' + Number(e.total_seats).toLocaleString()  + '</td>' +
                    '<td><span class="status-badge ' + statusClass(e.status) + '">' + e.status + '</span></td>' +
                    '<td><div class="tbl-actions">' +
                    '<button class="tbl-btn tbl-btn-edit"   onclick="editRow(this)">✏️ Edit</button>' +
                    '<button class="tbl-btn tbl-btn-delete" onclick="deleteRowBtn(this)">🗑 Delete</button>' +
                    '</div></td>' +
                    '</tr>';
                $('#eventsTableBody').append(row);
            }
        },
        error: function (err) {
            console.error('getAllEvents error:', err);
            if (handleAuthError(err.status)) return;
            SWAL.error('Failed to Load Events', 'Could not retrieve events from the server. Make sure the backend is running on port 8080.');
        }
    });
}

function editRow(btn) {
    var tr = $(btn).closest('tr');
    $('#f_eventId').val(tr.data('id'));
    $('#f_title').val(tr.data('name'));
    $('#f_location').val(tr.data('location'));
    $('#f_date').val(tr.data('date'));
    $('#f_time').val(tr.data('time'));
    $('#f_ticketPrice').val(tr.data('price'));
    $('#f_totalSeats').val(tr.data('seats'));
    $('#f_status').val(tr.data('status'));
    $('#btnUpdate').prop('disabled', false);
    $('#btnDelete').prop('disabled', false);

    SWAL.toast('Editing: ' + tr.data('name'), 'info');
    $('html, body').animate({ scrollTop: 0 }, 400);
}

function deleteRowBtn(btn) {
    var tr   = $(btn).closest('tr');
    var id   = tr.data('id');
    var name = tr.data('name');

    SWAL.confirm(
        'Delete Event?',
        'You are about to permanently delete "' + name + '". This action cannot be undone.',
        '🗑 Yes, Delete'
    ).then(function (result) {
        if (!result.isConfirmed) return;

        SWAL.loading('Deleting…', 'Removing the event from the system.');

        // ✅ Fixed: ID in URL path, removed broken JSON body with wrong key
        $.ajax({
            url:     API_URL + '/' + id,
            method:  'DELETE',
            headers: authHeaders(),
            success: function (res) {
                SWAL.success('Deleted!', '"' + name + '" has been removed successfully.');
                clearEventForm();
                getAllEvents();
            },
            error: function (err) {
                console.error('deleteRowBtn error:', err);
                if (handleAuthError(err.status)) return;
                var msg = err.responseJSON ? err.responseJSON.message : 'Server or network error while deleting event.';
                SWAL.error('Delete Failed', msg);
            }
        });
    });
}

function statusClass(s) {
    var map = { Active: 'badge-active', Upcoming: 'badge-upcoming', Completed: 'badge-completed', Cancelled: 'badge-cancelled' };
    return map[s] || 'badge-completed';
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

    getAllEvents();

    $('#btnSave').click(saveEvent);
    $('#btnUpdate').click(updateEvent);
    $('#btnDelete').click(deleteEvent);
});