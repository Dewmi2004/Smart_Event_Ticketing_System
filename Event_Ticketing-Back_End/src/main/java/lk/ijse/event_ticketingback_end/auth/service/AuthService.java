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
        User user = userRepository.findByUsername(authDTO.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(authDTO.getUsername()));

        if (!passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(authDTO.getUsername());
        }

        String token = jwtUtil.generateToken(authDTO.getUsername());
        return new AuthResponseDTO(token, user.getRole().name());
    }

    public String register(RegisterDTO registerDTO) {
        if (userRepository.findByUsername(registerDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already in use");
        }

        Role role;
        try {
            role = Role.valueOf(registerDTO.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + registerDTO.getRole()
                    + ". Accepted values: USER, ADMIN");
        }

//        if (role == Role.ADMIN) {
//            throw new RuntimeException(
//                    "ADMIN accounts cannot be self-registered. Contact a system administrator.");
//        }

        User user = User.builder()
                .username(registerDTO.getUsername())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }
}