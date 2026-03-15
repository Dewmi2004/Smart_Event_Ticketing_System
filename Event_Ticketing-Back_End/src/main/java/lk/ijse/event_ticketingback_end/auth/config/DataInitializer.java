package lk.ijse.event_ticketingback_end.auth.config;

import lk.ijse.event_ticketingback_end.auth.entity.Role;
import lk.ijse.event_ticketingback_end.auth.entity.User;
import lk.ijse.event_ticketingback_end.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminUsername = "dew@gmail.com";

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode("Dew1234@"))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            System.out.println("Default ADMIN created: " + adminUsername);
        } else {
            System.out.println("Default ADMIN already exists: " + adminUsername);
        }
    }
}