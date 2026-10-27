package com.kaushik.ChildShield.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaushik.ChildShield.user.dto.UserDTO;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private Map<String, Object> parseBodyOrParams(HttpServletRequest request) {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                StringBuilder sb = new StringBuilder();
                String line;
                try (var reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                if (sb.length() > 0) {
                    return objectMapper.readValue(sb.toString(), Map.class);
                }
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }

    @PostMapping({"/login", "/api/auth/login"})
    public ResponseEntity<?> login(HttpServletRequest request, HttpSession session) {
        Map<String, Object> body = parseBodyOrParams(request);
        String username = (String) body.getOrDefault("username", request.getParameter("username"));
        String password = (String) body.getOrDefault("password", request.getParameter("password"));

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Username and password required"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled() ||
                !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid credentials"));
        }

        User user = userOpt.get();
        session.setAttribute("currentUser", user);

        Map<String, Object> userMap = Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "enabled", user.isEnabled()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", userMap
        ));
    }

    @PostMapping({"/register", "/api/auth/register"})
    public ResponseEntity<?> register(HttpServletRequest request) {
        Map<String, Object> body = parseBodyOrParams(request);
        String fullName = (String) body.getOrDefault("fullName", request.getParameter("fullName"));
        String username = (String) body.getOrDefault("username", request.getParameter("username"));
        String password = (String) body.getOrDefault("password", request.getParameter("password"));
        String roleStr = (String) body.getOrDefault("role", request.getParameter("role"));

        if (username == null || password == null || fullName == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Full name, username and password are required"));
        }

        if (userRepository.existsByUsername(username.trim())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username already exists"));
        }

        // Only Health Worker role can be registered publicly.
        // Mothers / Guardians are enrolled strictly by Health Workers during field visits.
        User.Role role = User.Role.HEALTH_WORKER;

        UserDTO dto = UserDTO.builder()
                .fullName(fullName.trim())
                .username(username.trim())
                .password(password)
                .role(role)
                .enabled(true)
                .build();

        User created = userService.create(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping({"/logout", "/api/auth/logout"})
    public ResponseEntity<?> logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}

