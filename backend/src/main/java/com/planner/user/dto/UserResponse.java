package com.planner.user.dto;

import com.planner.user.User;

public record UserResponse(Long id, String email, String displayName, String avatarUrl) {
    public static UserResponse from(User u) {
        String url = u.getAvatarPath() == null ? null
            : "/api/files/avatars/" + u.getAvatarPath();
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), url);
    }
}
