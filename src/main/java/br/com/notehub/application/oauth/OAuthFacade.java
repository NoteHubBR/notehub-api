package br.com.notehub.application.oauth;

import br.com.notehub.application.dto.oauth.OAuthResponse;
import br.com.notehub.application.oauth.github.OAuthGitHubService;
import br.com.notehub.application.oauth.google.OAuthGoogleService;
import br.com.notehub.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class OAuthFacade {

    private final UserRepository userRepository;
    private final OAuthGitHubService gitHubService;
    private final OAuthGoogleService googleService;

    public String resolveUniqueUsername(String id, String base) {
        int attempt = 0;
        String username = base.toLowerCase();
        while (attempt < 12 && userRepository.existsByUsername(username)) {
            username = base.toLowerCase() + (new Random().nextInt(9000) + 1000);
            attempt++;
        }
        if (userRepository.existsByUsername(username)) username = base + id.substring(0, 4);
        return username;
    }

    public OAuthResponse getGitHubUser(String code) {
        return gitHubService.getUser(code);
    }

    public OAuthResponse getGoogleUser(String token) {
        return googleService.getUser(token);
    }

}