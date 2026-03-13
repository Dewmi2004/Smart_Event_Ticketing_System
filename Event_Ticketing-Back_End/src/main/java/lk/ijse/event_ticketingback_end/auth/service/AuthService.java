package lk.ijse.event_ticketingback_end.auth.service;

import lk.ijse.event_ticketingback_end.auth.dto.AuthDTO;
import lk.ijse.event_ticketingback_end.auth.dto.AuthResponseDTO;
import lk.ijse.event_ticketingback_end.auth.dto.RegisterDTO;
import lk.ijse.event_ticketingback_end.auth.entity.Role;
import lk.ijse.event_ticketingback_end.auth.entity.User;
import lk.ijse.event_ticketingback_end.auth.repository.UserRepository;
import lk.ijse.event_ticketingback_end.auth.util.JwtUtill;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtill jwtUtil;

    public AuthResponseDTO authenticate(AuthDTO authDTO) {
        // Find user in DB
        User user = userRepository.findByUsername(authDTO.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(authDTO.getUsername()));

        // Match raw password against stored BCrypt hash
        if (!passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(authDTO.getUsername());
        }

        // Generate JWT
        String token = jwtUtil.generateToken(authDTO.getUsername());
        return new AuthResponseDTO(token);
    }

    public String register(RegisterDTO registerDTO) {
        // Prevent duplicate usernames
        if (userRepository.findByUsername(registerDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already in use");
        }

        // ✅ FIX: Validate role string before calling Role.valueOf() to avoid
        //         an unhelpful IllegalArgumentException.
        //         The frontend always sends "USER", but this guards against
        //         tampered requests trying to self-assign "ADMIN".
        Role role;
        try {
            role = Role.valueOf(registerDTO.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + registerDTO.getRole()
                    + ". Accepted values: USER, ADMIN");
        }

        // ✅ Extra guard: public registration is only allowed for USER role.
        //    ADMIN accounts must be created by an existing admin via a
        //    separate admin endpoint (not implemented here yet).
        if (role == Role.ADMIN) {
            throw new RuntimeException(
                    "ADMIN accounts cannot be self-registered. Contact a system administrator.");
        }

        User user = User.builder()
                .username(registerDTO.getUsername())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }
}