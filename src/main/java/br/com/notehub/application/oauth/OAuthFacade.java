package br.com.notehub.application.oauth;

import br.com.notehub.application.dto.oauth.OAuthResponse;
import br.com.notehub.application.oauth.github.OAuthGitHubService;
import br.com.notehub.application.oauth.google.OAuthGoogleService;
import br.com.notehub.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class OAuthFacade {

    private final UserRepository userRepository;
    private final OAuthGitHubService gitHubService;
    private final OAuthGoogleService googleService;

    private static final String FALLBACK_USERNAME = "user";
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 24;
    private static final int SUFFIX_LENGTH = 4;
    private static final int ID_SUFFIX_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 12;

    private static String sanitizeUsername(String raw) {
        if (raw == null) return FALLBACK_USERNAME;
        String s = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9_.-]", "")
                .replaceAll("([._-])\\1+", "$1")
                .replaceAll("^[._-]+|[._-]+$", "");
        if (s.isEmpty()) return FALLBACK_USERNAME;
        if (s.length() < MIN_LENGTH) return s + FALLBACK_USERNAME;
        return s;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max).replaceAll("[._-]+$", "");
    }

    public String resolveUniqueUsername(String id, String rawBase) {
        String clean = sanitizeUsername(rawBase);
        String username = truncate(clean, MAX_LENGTH);
        String suffixBase = truncate(clean, MAX_LENGTH - SUFFIX_LENGTH);
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS && userRepository.existsByUsername(username)) {
            username = suffixBase + ThreadLocalRandom.current().nextInt(1000, 10000);
            attempt++;
        }
        if (userRepository.existsByUsername(username)) {
            String safeId = sanitizeUsername(id);
            String idPart = safeId.substring(Math.max(0, safeId.length() - ID_SUFFIX_LENGTH));
            String fallbackBase = truncate(clean, MAX_LENGTH - idPart.length() - 1);
            username = fallbackBase + "-" + idPart;
        }
        return username;
    }

    public OAuthResponse getGitHubUser(String code) {
        return gitHubService.getUser(code);
    }

    public OAuthResponse getGoogleUser(String token) {
        return googleService.getUser(token);
    }

}