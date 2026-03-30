package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.NotificationDto;

import java.util.List;

public interface NotificationService {

    void saveNotification(NotificationDto notificationDto);

    void updateNotification(NotificationDto notificationDto);

    void deleteNotification(NotificationDto notificationDto);

    List<NotificationDto> getAllNotifications();

    List<NotificationDto> getNotificationsByUserId(int userId);
}