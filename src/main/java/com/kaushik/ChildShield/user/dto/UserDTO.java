package com.kaushik.ChildShield.user.dto;

import com.kaushik.ChildShield.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String fullName;
    private String username;
    private String password;
    private User.Role role;
    @Builder.Default
    private boolean enabled = true;
    @Builder.Default
    private Set<Long> villageIds = new HashSet<>();
}
