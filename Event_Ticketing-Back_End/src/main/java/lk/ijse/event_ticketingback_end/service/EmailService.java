package lk.ijse.event_ticketingback_end.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendBookingConfirmation(
            String toEmail,
            String userName,
            int    bookingId,
            String eventName,
            String eventDate,
            String eventLocation,
            String seatNumbers,
            double totalAmount,
            String qrUrl
    ) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("🎫 Booking Confirmed — " + eventName + " | EventHub");

        String html =
                "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#f8f8fc;padding:32px;border-radius:16px;'>" +
                        "  <div style='background:linear-gradient(135deg,#7C3AED,#EC4899);padding:28px;border-radius:12px;text-align:center;'>" +
                        "    <h1 style='color:#fff;margin:0;font-size:24px;'>🎫 Booking Confirmed!</h1>" +
                        "    <p style='color:rgba(255,255,255,0.85);margin:8px 0 0;'>Your e-ticket is ready</p>" +
                        "  </div>" +
                        "  <div style='background:#fff;border-radius:12px;padding:28px;margin-top:16px;'>" +
                        "    <p style='color:#1a1a2e;font-size:16px;'>Hi <strong>" + userName + "</strong>,</p>" +
                        "    <p style='color:#555;'>Your booking has been confirmed. Please find your e-ticket details below.</p>" +
                        "    <table style='width:100%;border-collapse:collapse;margin:20px 0;'>" +
                        "      <tr style='background:#f3f4f8;'>" +
                        "        <td style='padding:12px 16px;color:#7a8899;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;border-radius:8px 0 0 8px;'>Booking ID</td>" +
                        "        <td style='padding:12px 16px;color:#1a1a2e;font-weight:600;border-radius:0 8px 8px 0;'>#" + bookingId + "</td>" +
                        "      </tr>" +
                        "      <tr>" +
                        "        <td style='padding:12px 16px;color:#7a8899;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;'>Event</td>" +
                        "        <td style='padding:12px 16px;color:#1a1a2e;font-weight:600;'>" + eventName + "</td>" +
                        "      </tr>" +
                        "      <tr style='background:#f3f4f8;'>" +
                        "        <td style='padding:12px 16px;color:#7a8899;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;border-radius:8px 0 0 8px;'>Date</td>" +
                        "        <td style='padding:12px 16px;color:#1a1a2e;border-radius:0 8px 8px 0;'>" + eventDate + "</td>" +
                        "      </tr>" +
                        "      <tr>" +
                        "        <td style='padding:12px 16px;color:#7a8899;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;'>Location</td>" +
                        "        <td style='padding:12px 16px;color:#1a1a2e;'>" + eventLocation + "</td>" +
                        "      </tr>" +
                        "      <tr style='background:#f3f4f8;'>" +
                        "        <td style='padding:12px 16px;color:#7a8899;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;border-radius:8px 0 0 8px;'>Seats</td>" +
                        "        <td style='padding:12px 16px;color:#1a1a2e;border-radius:0 8px 8px 0;'>" + seatNumbers + "</td>" +
                        "      </tr>" +
                        "      <tr>" +
                        "        <td style='padding:12px 16px;color:#7a8899;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;'>Total Paid</td>" +
                        "        <td style='padding:12px 16px;color:#7C3AED;font-weight:700;font-size:16px;'>LKR " + String.format("%,.2f", totalAmount) + "</td>" +
                        "      </tr>" +
                        "    </table>" +
                        "    <div style='text-align:center;margin:28px 0;'>" +
                        "      <p style='color:#555;font-size:13px;margin-bottom:12px;'>Scan this QR at the venue gate</p>" +
                        "      <img src='" + qrUrl + "' alt='E-Ticket QR Code' style='width:200px;height:200px;border:3px solid #7C3AED;border-radius:12px;padding:8px;'/>" +
                        "    </div>" +
                        "    <div style='background:#f3f4f8;border-radius:10px;padding:16px;text-align:center;margin-top:16px;'>" +
                        "      <p style='color:#7a8899;font-size:12px;margin:0;'>Booking status is <strong>Pending</strong> until payment is completed.</p>" +
                        "      <p style='color:#7a8899;font-size:12px;margin:6px 0 0;'>This is an automated email from EventHub. Please do not reply.</p>" +
                        "    </div>" +
                        "  </div>" +
                        "</div>";

        helper.setText(html, true);
        mailSender.send(message);
    }
}