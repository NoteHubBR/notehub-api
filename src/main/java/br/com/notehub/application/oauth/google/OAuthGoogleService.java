package br.com.notehub.application.oauth.google;

import br.com.notehub.application.dto.oauth.OAuthResponse;
import br.com.notehub.application.dto.oauth.google.GoogleUserResponse;
import br.com.notehub.application.oauth.OAuthService;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor
public class OAuthGoogleService implements OAuthService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    @Override
    public OAuthResponse getUser(String token) {
        try {
            HttpRequest codeRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v1/userinfo"))
                    .headers("Accept", "application/json", "Authorization", "Bearer " + token)
                    .build();
            HttpResponse<String> tokenResponse = client.send(codeRequest, HttpResponse.BodyHandlers.ofString());
            GoogleUserResponse userData = mapper.readValue(tokenResponse.body(), GoogleUserResponse.class);
            return new OAuthResponse(userData);
        } catch (Exception e) {
            throw new JWTDecodeException("Token inválido.");
        }
    }

}