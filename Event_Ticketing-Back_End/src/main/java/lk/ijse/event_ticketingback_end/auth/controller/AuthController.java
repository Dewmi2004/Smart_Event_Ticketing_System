package lk.ijse.event_ticketingback_end.auth.controller;

import lk.ijse.event_ticketingback_end.auth.dto.AuthDTO;
import lk.ijse.event_ticketingback_end.auth.dto.RegisterDTO;
import lk.ijse.event_ticketingback_end.auth.service.AuthService;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@CrossOrigin   // kept for fallback; primary CORS is handled in SecurityConfig
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("signUp")
    public ResponseEntity<APIResponse> registerUser(@RequestBody RegisterDTO registerDTO) {
        return ResponseEntity.ok(new APIResponse(
                200, "OK", authService.register(registerDTO)));
    }

    @PostMapping("signIn")
    public ResponseEntity<APIResponse> loginUser(@RequestBody AuthDTO authDTO) {
        return ResponseEntity.ok(new APIResponse(
                200, "OK", authService.authenticate(authDTO)));
    }
}