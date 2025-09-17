package br.com.notehub.application.dto.response.user;

import br.com.notehub.domain.user.Host;
import br.com.notehub.domain.user.User;

import java.util.UUID;

public record CreateUserRES(
        UUID id,
        String email,
        String username,
        String display_name,
        Host host,
        boolean profile_private,
        boolean sponsor,
        Long score
) {
    public CreateUserRES(User user) {
        this(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getHost(),
                user.isProfilePrivate(),
                user.isSponsor(),
                user.getScore()
        );
    }
}