package br.com.notehub.application.implementation.token;

import br.com.notehub.application.dto.response.token.AuthRES;
import br.com.notehub.domain.token.Token;
import br.com.notehub.domain.token.TokenRepository;
import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import br.com.notehub.infra.exception.CustomExceptions;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    @Value("${api.server.security.token.secret}")
    private String secret;

    @Value("${oauth.github.client.id}")
    private String GHCI;

    @Value("${oauth.github.client.secret}")
    private String GHCS;

    private final TokenRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    private UUID validateDevice(HttpServletRequest request) {
        String xDeviceId = request.getHeader("X-Device-Id");
        if (xDeviceId == null) throw new CustomExceptions.MissingDeviceException();
        UUID device;
        try {
            return device = UUID.fromString(xDeviceId);
        } catch (IllegalArgumentException ex) {
            throw new CustomExceptions.InvalidDeviceException();
        }
    }

    private UUID validateRefreshToken(HttpServletRequest request) {
        String xRefreshToken = request.getHeader("X-Refresh-Token");
        if (xRefreshToken == null) throw new CustomExceptions.MissingRefreshToken();
        UUID rToken;
        try {
            return rToken = UUID.fromString(xRefreshToken);
        } catch (IllegalArgumentException ex) {
            throw new CustomExceptions.InvalidRefreshTokenException();
        }
    }

    private Token generateRefreshToken(HttpServletRequest request, User user) {
        String ip = request.getRemoteAddr();
        String agent = request.getHeader("User-Agent");
        UUID device = validateDevice(request);
        Instant expiresAt = getExpirationTime("refresh");
        return new Token(user, ip, agent, device, expiresAt);
    }

    private Map getUserInfoFromGoogle(String token) {
        String url = String.format("https://www.googleapis.com/oauth2/v1/userinfo?access_token=%s", token);
        ResponseEntity<Map> response = new RestTemplate().getForEntity(url, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            System.out.println(response.getBody());
            return response.getBody();
        } else {
            throw new JWTDecodeException("Token inválido");
        }
    }

    private Map getUserInfoFromGitHub(String code) {
        RestTemplate rt = new RestTemplate();
        ResponseEntity<Map> fResponse = rt.getForEntity(
                String.format("https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s", GHCI, GHCS, code),
                Map.class
        );
        if (!fResponse.getStatusCode().is2xxSuccessful() || fResponse.getBody() == null) throw new JWTDecodeException("Código inválido");
        ResponseEntity<Map> sResponse = rt.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders() {{
                    setBearerAuth((String) fResponse.getBody().get("access_token"));
                    set("Accept", "application/json");
                }}),
                Map.class
        );
        if (!sResponse.getStatusCode().is2xxSuccessful() || sResponse.getBody() == null) throw new JWTDecodeException("Token inválido");
        return sResponse.getBody();
    }

    private void validateHost(String host) {
        if (!Objects.equals(host, "NoteHub")) throw new CustomExceptions.HostNotAllowedException();
    }

    @Override
    public Instant getExpirationTime(String tokenType) {
        return switch (tokenType) {
            case "refresh" -> LocalDateTime.now().plusDays(30).toInstant(ZoneOffset.of("-03:00"));
            case "access" -> LocalDateTime.now().plusMinutes(30).toInstant(ZoneOffset.of("-03:00"));
            default -> null;
        };
    }

    @Override
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("NoteHub")
                    .withSubject(String.valueOf(user.getId()))
                    .withExpiresAt(getExpirationTime("access"))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new JWTCreationException("👀", exception);
        }
    }

    @Override
    public String generateActivationToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("NoteHub")
                    .withSubject(String.valueOf(user.getId()))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new JWTCreationException("👀", exception);
        }
    }

    @Override
    public String generatePasswordChangeToken(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        validateHost(user.getHost());
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("NoteHub")
                    .withSubject(email)
                    .withExpiresAt(getExpirationTime("access"))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new JWTCreationException("👀", exception);
        }
    }

    @Override
    public String generateEmailChangeToken(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        validateHost(user.getHost());
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("NoteHub")
                    .withSubject(email)
                    .withExpiresAt(getExpirationTime("access"))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new JWTCreationException("👀", exception);
        }
    }

    @Override
    public String validateToken(String accessToken) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("NoteHub")
                    .build()
                    .verify(accessToken)
                    .getSubject();
        } catch (JWTVerificationException ex) {
            throw new JWTVerificationException("Token inválido.", ex);
        }
    }

    @Transactional
    @Override
    public AuthRES auth(HttpServletRequest request, String username, String password) throws BadCredentialsException {

        User user = userRepository.findByUsername(username.toLowerCase()).orElseThrow(() -> new BadCredentialsException("username"));
        if (!user.isActive()) throw new DisabledException("Email não confirmado");

        boolean matches = encoder.matches(password, user.getPassword());
        if (!matches) throw new BadCredentialsException("password");

        Token token = generateRefreshToken(request, user);
        repository.findByDevice(token.getDevice()).ifPresent(repository::delete);
        repository.save(token);

        return new AuthRES(token, generateToken(user));

    }

    @Transactional
    @Override
    public AuthRES authWithGoogleAcc(HttpServletRequest request, String token) {
        try {

            Map info = getUserInfoFromGoogle(token);
            String id = (String) info.get("id");
            String email = (String) info.get("email");
            String givenName = (String) info.get("given_name");
            String displayName = (String) info.get("name");
            String username = String.format("%s%s", givenName, id.substring(0, 4));
            String avatar = (String) info.get("picture");
            User provided = new User(id, email, username, displayName, avatar);

            User user = userRepository.findByProviderId(id).orElseGet(() -> userRepository.save(provided));
            Token rToken = generateRefreshToken(request, user);
            repository.findByDevice(rToken.getDevice()).ifPresent(repository::delete);
            repository.save(rToken);

            return new AuthRES(rToken, generateToken(user));

        } catch (JWTDecodeException exception) {
            throw new JWTDecodeException("Formato inválido");
        }
    }

    @Transactional
    @Override
    public AuthRES authWithGitHubAcc(HttpServletRequest request, String code) {
        Map info = getUserInfoFromGitHub(code);
        Integer id = (Integer) info.get("id");
        String login = (String) info.get("login");
        String username = String.format("%s%s", login, id.toString().substring(0, 4));
        String displayName = (String) info.get("name");
        String avatar = (String) info.get("avatar_url");
        User provided = new User(id, username, displayName, avatar);

        User user = userRepository.findByProviderId(id.toString()).orElseGet(() -> userRepository.save(provided));
        Token token = generateRefreshToken(request, user);
        repository.findByDevice(token.getDevice()).ifPresent(repository::delete);
        repository.save(token);

        return new AuthRES(token, generateToken(user));

    }

    @Transactional
    @Override
    public AuthRES recreateToken(HttpServletRequest request) throws TokenExpiredException {

        UUID rToken = validateRefreshToken(request);
        Token currentToken = repository.findById(rToken).orElseThrow(EntityNotFoundException::new);

        Instant now = LocalDateTime.now().toInstant(ZoneOffset.of("-03:00"));
        if (currentToken.getExpiresAt().isBefore(now)) {
            throw new TokenExpiredException("Refresh Token expirado.", currentToken.getExpiresAt());
        }

        User user = currentToken.getUser();
        Token newToken = generateRefreshToken(request, user);
        repository.findByDevice(newToken.getDevice()).ifPresent(repository::delete);
        repository.save(newToken);

        return new AuthRES(newToken, generateToken(user));

    }

    @Transactional
    @Override
    public void logout(HttpServletRequest request) {
        UUID rToken = validateRefreshToken(request);
        Token token = repository.findById(rToken).orElseThrow(EntityNotFoundException::new);
        repository.delete(token);
    }

    @Override
    public void cleanExpiredTokens() {
        List<Token> expiredTokens = repository.findExpiredTokens(Instant.now());
        repository.deleteAll(expiredTokens);
    }

}