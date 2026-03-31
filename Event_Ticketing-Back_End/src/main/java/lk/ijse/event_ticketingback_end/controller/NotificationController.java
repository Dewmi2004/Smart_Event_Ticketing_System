package lk.ijse.event_ticketingback_end.controller;

import lk.ijse.event_ticketingback_end.dto.NotificationDto;
import lk.ijse.event_ticketingback_end.service.NotificationService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/notification")
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<String>> saveNotification(
            @RequestBody NotificationDto notificationDto) {

        notificationService.saveNotification(notificationDto);
        return new ResponseEntity<>(
                new APIResponse<>(201, "Notification Saved", null),
                HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<String>> updateNotification(
            @RequestBody NotificationDto notificationDto) {

        notificationService.updateNotification(notificationDto);
        return new ResponseEntity<>(
                new APIResponse<>(200, "Notification Updated", null),
                HttpStatus.OK);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<String>> deleteNotification(
            @RequestBody NotificationDto notificationDto) {

        notificationService.deleteNotification(notificationDto);
        return new ResponseEntity<>(
                new APIResponse<>(200, "Notification Deleted", null),
                HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<NotificationDto>>> getAllNotifications() {
        List<NotificationDto> notifications = notificationService.getAllNotifications();
        return new ResponseEntity<>(
                new APIResponse<>(200, "Success", notifications),
                HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<List<NotificationDto>>> getByUser(
            @PathVariable int userId) {

        List<NotificationDto> notifications = notificationService.getNotificationsByUserId(userId);
        return new ResponseEntity<>(
                new APIResponse<>(200, "Success", notifications),
                HttpStatus.OK);
    }
}