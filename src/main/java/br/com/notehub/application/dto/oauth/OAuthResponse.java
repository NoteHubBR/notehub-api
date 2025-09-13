package br.com.notehub.application.dto.oauth;

import br.com.notehub.application.dto.oauth.github.EmailResponse;
import br.com.notehub.application.dto.oauth.github.GitHubUserResponse;
import br.com.notehub.application.dto.oauth.google.GoogleUserResponse;

public record OAuthResponse(
        String id,
        String email,
        String username,
        String displayName,
        String avatar
) {

    public OAuthResponse(GitHubUserResponse userResponse, EmailResponse emailResponse) {
        this(
                userResponse.id(),
                emailResponse.email(),
                userResponse.username(),
                userResponse.displayName(),
                userResponse.avatar()
        );
    }

    public OAuthResponse(GoogleUserResponse userResponse) {
        this(
                userResponse.id(),
                userResponse.email(),
                userResponse.username(),
                userResponse.displayName(),
                userResponse.avatar()
        );
    }

}