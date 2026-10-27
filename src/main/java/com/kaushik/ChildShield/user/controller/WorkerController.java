package com.kaushik.ChildShield.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaushik.ChildShield.user.dto.UserDTO;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping({"/api/admin/health-workers", "/healthcare-workers"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkerController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    private Map<String, Object> parseBody(HttpServletRequest request) {
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

    @GetMapping
    public ResponseEntity<List<User>> listWorkers() {
        return ResponseEntity.ok(userService.findByRole(User.Role.HEALTH_WORKER));
    }

    @PostMapping
    public ResponseEntity<User> createWorker(HttpServletRequest request) {
        Map<String, Object> body = parseBody(request);
        String fullName = (String) body.getOrDefault("fullName", request.getParameter("fullName"));
        String username = (String) body.getOrDefault("username", request.getParameter("username"));
        String password = (String) body.getOrDefault("password", request.getParameter("password"));

        Set<Long> villageIds = new HashSet<>();
        Object vIdsObj = body.get("villageIds");
        if (vIdsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number n) {
                    villageIds.add(n.longValue());
                } else if (item != null) {
                    try {
                        villageIds.add(Long.valueOf(item.toString()));
                    } catch (Exception ignored) {
                    }
                }
            }
        } else if (request.getParameterValues("villageIds") != null) {
            for (String s : request.getParameterValues("villageIds")) {
                try {
                    villageIds.add(Long.valueOf(s));
                } catch (Exception ignored) {
                }
            }
        }

        UserDTO dto = UserDTO.builder()
                .fullName(fullName)
                .username(username)
                .password(password)
                .role(User.Role.HEALTH_WORKER)
                .enabled(true)
                .villageIds(villageIds)
                .build();

        return ResponseEntity.ok(userService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getWorker(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateWorker(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> body = parseBody(request);
        String fullName = (String) body.getOrDefault("fullName", request.getParameter("fullName"));
        String username = (String) body.getOrDefault("username", request.getParameter("username"));
        String password = (String) body.getOrDefault("password", request.getParameter("password"));

        Set<Long> villageIds = new HashSet<>();
        Object vIdsObj = body.get("villageIds");
        if (vIdsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number n) {
                    villageIds.add(n.longValue());
                }
            }
        }

        UserDTO dto = UserDTO.builder()
                .id(id)
                .fullName(fullName)
                .username(username)
                .password(password)
                .role(User.Role.HEALTH_WORKER)
                .enabled(true)
                .villageIds(villageIds)
                .build();

        return ResponseEntity.ok(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

