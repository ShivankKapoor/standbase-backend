package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.User;
import com.shivankkapoor.standbase.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final AuthService authService;

    private final SessionService sessionService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService, SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.sessionService = sessionService;
    }

    public String login(String username, String password, String ip) {
        Optional<User> attemptedUser = userRepository.findByUsername(username);
        if (attemptedUser.isEmpty()) {
            log.warn("No user found for login request {}", username);
            return null;
        }
        User user = attemptedUser.get();
        if (!passwordEncoder.matches(password, user.password())) {
            log.warn("Invalid password attempt for user {}", username);
            return null;
        }
        return sessionService.createSession(user.id(), ip);
    }

    public void logout(String sessionToken) {
        authService.logout(sessionToken);
    }
}
