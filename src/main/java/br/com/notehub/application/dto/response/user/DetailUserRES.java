package br.com.notehub.application.dto.response.user;

import br.com.notehub.domain.user.User;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record DetailUserRES(
        String created_at,
        String username,
        String display_name,
        String avatar,
        String banner,
        String message,
        int notes_count,
        int followers_count,
        int following_count,
        boolean profile_private,
        boolean sponsor,
        boolean blocked
) {
    public DetailUserRES(User user) {
        this(
                user.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("d/M/yy", Locale.of("pt-BR"))),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatar(),
                user.getBanner(),
                user.getMessage(),
                user.getNotesCount(),
                user.getFollowersCount(),
                user.getFollowingCount(),
                user.isProfilePrivate(),
                user.isSponsor(),
                user.isBlocked()
        );
    }
}