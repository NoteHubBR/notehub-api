package br.com.notehub.application.oauth.github;

import br.com.notehub.application.dto.oauth.OAuthResponse;
import br.com.notehub.application.dto.oauth.github.CodeResponse;
import br.com.notehub.application.dto.oauth.github.EmailResponse;
import br.com.notehub.application.dto.oauth.github.GitHubUserResponse;
import br.com.notehub.application.oauth.OAuthService;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuthGitHubService implements OAuthService {

    @Value("${oauth.github.client.id}")
    private String GHCI;

    @Value("${oauth.github.client.secret}")
    private String GHCS;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    @Override
    public OAuthResponse getUser(String key) {

        try {

            HttpRequest codeRequest = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(
                            "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s",
                            GHCI, GHCS, key)))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> codeResponse = client.send(codeRequest, HttpResponse.BodyHandlers.ofString());
            CodeResponse codeData = mapper.readValue(codeResponse.body(), CodeResponse.class);

            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/user"))
                    .headers(
                            "Accept", "application/json",
                            "Authorization", "Bearer " + codeData.accessToken()
                    )
                    .build();
            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            GitHubUserResponse userData = mapper.readValue(userResponse.body(), GitHubUserResponse.class);

            HttpRequest emailsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/user/emails"))
                    .headers(
                            "Accept", "application/json",
                            "Authorization", "Bearer " + codeData.accessToken()
                    )
                    .build();
            HttpResponse<String> emailsResponse = client.send(emailsRequest, HttpResponse.BodyHandlers.ofString());

            List<EmailResponse> emailsData = mapper.readValue(
                    emailsResponse.body(),
                    new TypeReference<List<EmailResponse>>() {
                    }
            );

            Optional<EmailResponse> opt = emailsData.stream().filter(e -> e.primary() && e.verified()).findFirst();
            if (opt.isEmpty()) opt = emailsData.stream().filter(e -> Boolean.TRUE.equals(e.primary())).findFirst();
            if (opt.isEmpty()) opt = emailsData.stream().filter(e -> Boolean.TRUE.equals(e.verified())).findFirst();

            EmailResponse emailData = opt.orElseGet(() -> emailsData.isEmpty() ? null : emailsData.getFirst());

            if (emailData == null) throw new JWTDecodeException("Email faltando.");

            return new OAuthResponse(userData, emailData);

        } catch (Exception e) {
            throw new JWTDecodeException("Código inválido.");
        }

    }

}