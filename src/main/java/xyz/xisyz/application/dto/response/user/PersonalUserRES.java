package xyz.xisyz.application.dto.response.user;

import java.util.UUID;

public record PersonalUserRES(
        UUID id,
        String created_at,
        String email,
        String username,
        String display_name,
        String avatar,
        String banner,
        String message,
        String host,
        boolean profile_private,
        boolean sponsor,
        Long score,
        int notes_count,
        int followers_count,
        int following_count,
        long notifications
) {
}