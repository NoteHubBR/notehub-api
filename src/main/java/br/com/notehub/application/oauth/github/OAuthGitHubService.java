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
import java.util.Comparator;
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

        GitHubUserResponse userData;
        List<EmailResponse> emailsData;

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
            userData = mapper.readValue(userResponse.body(), GitHubUserResponse.class);

            HttpRequest emailsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/user/emails"))
                    .headers(
                            "Accept", "application/json",
                            "Authorization", "Bearer " + codeData.accessToken()
                    )
                    .build();
            HttpResponse<String> emailsResponse = client.send(emailsRequest, HttpResponse.BodyHandlers.ofString());

            emailsData = mapper.readValue(
                    emailsResponse.body(),
                    new TypeReference<List<EmailResponse>>() {
                    }
            );

        } catch (Exception e) {
            throw new JWTDecodeException("Código inválido.");
        }

        Optional<EmailResponse> opt = emailsData.stream()
                .filter(EmailResponse::verified)
                .max(Comparator.comparing(EmailResponse::primary));

        EmailResponse emailData = opt.orElseThrow(() -> new JWTDecodeException("Nenhum email verificado."));

        return new OAuthResponse(userData, emailData);

    }

}