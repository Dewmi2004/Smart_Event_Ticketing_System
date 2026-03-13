package lk.ijse.event_ticketingback_end.auth.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String role;
}