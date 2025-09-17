package br.com.notehub.application.dto.response.user;

import br.com.notehub.domain.user.Host;

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
        Host host,
        boolean profile_private,
        boolean sponsor,
        Long score,
        int notes_count,
        int followers_count,
        int following_count,
        long notifications
) {
}