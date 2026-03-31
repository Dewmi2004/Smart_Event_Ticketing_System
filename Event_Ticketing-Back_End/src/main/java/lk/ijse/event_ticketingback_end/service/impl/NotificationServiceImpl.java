package lk.ijse.event_ticketingback_end.service.impl;

import jakarta.mail.MessagingException;
import lk.ijse.event_ticketingback_end.dto.NotificationDto;
import lk.ijse.event_ticketingback_end.entity.Notification;
import lk.ijse.event_ticketingback_end.exception.EventNotFoundException;
import lk.ijse.event_ticketingback_end.repository.NotificationRepository;
import lk.ijse.event_ticketingback_end.service.EmailService;
import lk.ijse.event_ticketingback_end.service.NotificationService;
import lk.ijse.event_ticketingback_end.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper            modelMapper;
    private final EmailService           emailService;
    private final SmsService             smsService;

    @Override
    public void saveNotification(NotificationDto dto) {
        dto.setNotificationId(0);
        if (dto.getSentAt() == null) dto.setSentAt(LocalDateTime.now());

        String deliveryStatus = dispatch(dto);
        dto.setStatus(deliveryStatus);

        Notification notification = modelMapper.map(dto, Notification.class);
        notificationRepository.save(notification);
    }

    private String dispatch(NotificationDto dto) {
        String channel = dto.getChannel();
        if (channel == null || channel.isBlank()) {
            log.warn("[Notification] No channel set — skipping dispatch for userId={}", dto.getUserId());
            return "Pending";
        }

        boolean emailOk = false;
        boolean smsOk   = false;

        if (channel.equalsIgnoreCase("Email") || channel.equalsIgnoreCase("Both")) {
            emailOk = sendEmail(dto);
        }
        if (channel.equalsIgnoreCase("SMS") || channel.equalsIgnoreCase("Both")) {
            smsOk = sendSms(dto);
        }

        return switch (channel.toLowerCase()) {
            case "both"  -> (emailOk && smsOk) ? "Delivered" : "Failed";
            case "email" -> emailOk ? "Delivered" : "Failed";
            case "sms"   -> smsOk   ? "Delivered" : "Failed";
            default      -> "Pending";
        };
    }

    private boolean sendEmail(NotificationDto dto) {
        if (dto.getToEmail() == null || dto.getToEmail().isBlank()) {
            log.warn("[Notification] Email channel requested but toEmail is blank — userId={}", dto.getUserId());
            return false;
        }
        try {
            switch (dto.getType()) {
                case "BookingConfirmation" -> emailService.sendBookingConfirmation(
                        dto.getToEmail(),
                        nullSafe(dto.getUserName()),
                        nullSafeInt(dto.getBookingId()),
                        nullSafe(dto.getEventName()),
                        nullSafe(dto.getEventDate()),
                        nullSafe(dto.getEventLocation()),
                        nullSafe(dto.getSeatNumbers()),
                        nullSafeDouble(dto.getTotalAmount()),
                        nullSafe(dto.getQrUrl())
                );
                default -> emailService.sendGenericNotification(
                        dto.getToEmail(),
                        nullSafe(dto.getUserName()),
                        dto.getType(),
                        dto.getMessage()
                );
            }
            log.info("[Notification] Email dispatched — type={} to={}", dto.getType(), dto.getToEmail());
            return true;
        } catch (MessagingException e) {
            log.error("[Notification] Email failed — type={} to={} error={}", dto.getType(), dto.getToEmail(), e.getMessage());
            return false;
        }
    }

    private boolean sendSms(NotificationDto dto) {
        if (dto.getToPhone() == null || dto.getToPhone().isBlank()) {
            log.warn("[Notification] SMS channel requested but toPhone is blank — userId={}", dto.getUserId());
            return false;
        }
        try {
            switch (dto.getType()) {
                case "BookingConfirmation" -> smsService.sendBookingConfirmation(
                        dto.getToPhone(),
                        nullSafeInt(dto.getBookingId()),
                        nullSafe(dto.getEventName()),
                        nullSafe(dto.getEventDate()),
                        nullSafe(dto.getSeatNumbers()),
                        nullSafeDouble(dto.getTotalAmount())
                );
                case "PaymentFailed" -> smsService.sendPaymentFailed(
                        dto.getToPhone(),
                        nullSafeInt(dto.getBookingId()),
                        nullSafe(dto.getEventName())
                );
                default -> smsService.sendGenericNotification(
                        dto.getToPhone(),
                        dto.getMessage()
                );
            }
            log.info("[Notification] SMS dispatched — type={} to={}", dto.getType(), dto.getToPhone());
            return true;
        } catch (Exception e) {
            log.error("[Notification] SMS failed — type={} to={} error={}", dto.getType(), dto.getToPhone(), e.getMessage());
            return false;
        }
    }

    @Override
    public void updateNotification(NotificationDto dto) {
        Notification existing = notificationRepository
                .findById(dto.getNotificationId())
                .orElseThrow(() -> new EventNotFoundException(
                        "Notification not found with ID: " + dto.getNotificationId()));

        existing.setUserId(dto.getUserId());
        existing.setType(dto.getType());
        existing.setChannel(dto.getChannel());
        existing.setMessage(dto.getMessage());
        existing.setStatus(dto.getStatus());
        existing.setSentAt(dto.getSentAt());

        notificationRepository.saveAndFlush(existing);
    }

    @Override
    public void deleteNotification(NotificationDto dto) {
        Notification existing = notificationRepository
                .findById(dto.getNotificationId())
                .orElseThrow(() -> new EventNotFoundException(
                        "Notification not found with ID: " + dto.getNotificationId()));

        notificationRepository.delete(existing);
        notificationRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getAllNotifications() {
        return modelMapper.map(
                notificationRepository.findAll(),
                new TypeToken<List<NotificationDto>>() {}.getType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByUserId(int userId) {
        return modelMapper.map(
                notificationRepository.findAllByUserId(userId),
                new TypeToken<List<NotificationDto>>() {}.getType());
    }

    private String nullSafe(String v)       { return v != null ? v : ""; }
    private int    nullSafeInt(Integer v)   { return v != null ? v : 0;  }
    private double nullSafeDouble(Double v) { return v != null ? v : 0.0;}
}