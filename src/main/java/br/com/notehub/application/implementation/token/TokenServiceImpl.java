package br.com.notehub.application.implementation.token;

import br.com.notehub.application.dto.oauth.OAuthResponse;
import br.com.notehub.application.dto.response.token.AuthRES;
import br.com.notehub.application.oauth.OAuthFacade;
import br.com.notehub.domain.token.Token;
import br.com.notehub.domain.token.TokenRepository;
import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.Host;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import br.com.notehub.infra.exception.CustomExceptions;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    @Value("${api.server.security.token.secret}")
    private String secret;

    private final OAuthFacade oAuthFacade;
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

    private void validateInternalHost(Host host) {
        if (Objects.equals(host, Host.NOTEHUB)) throw new CustomExceptions.HostNotAllowedException();
    }

    private void validateExternalHost(Host host) {
        if (!Objects.equals(host, Host.NOTEHUB)) throw new CustomExceptions.HostNotAllowedException();
    }

    private User findOrCreateUserFromOAuthInfo(OAuthResponse info, Host host) {
        return userRepository.findByProviderId(info.id()).orElseGet(() -> {
            if (userRepository.existsByEmail(info.email())) throw new DataIntegrityViolationException("email");
            String username = oAuthFacade.resolveUniqueUsername(info.id(), info.username());
            User provided = new User(info.id(), host, info.email(), username, info.displayName(), info.avatar());
            return userRepository.save(provided);
        });
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
        validateExternalHost(user.getHost());
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("NoteHub")
                    .withSubject(email)
                    .withClaim("scope", "password")
                    .withExpiresAt(getExpirationTime("access"))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new JWTCreationException("👀", exception);
        }
    }

    @Override
    public String generateEmailChangeToken(String email) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("NoteHub")
                    .withSubject(email)
                    .withClaim("scope", "email")
                    .withExpiresAt(getExpirationTime("access"))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new JWTCreationException("👀", exception);
        }
    }

    @Override
    public String generateSecretKey(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        validateInternalHost(user.getHost());
        SecureRandom secureRandom = new SecureRandom();
        Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
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
        OAuthResponse info = oAuthFacade.getGoogleUser(token);
        User user = findOrCreateUserFromOAuthInfo(info, Host.GOOGLE);
        Token rToken = generateRefreshToken(request, user);
        repository.findByDevice(rToken.getDevice()).ifPresent(repository::delete);
        repository.save(rToken);
        return new AuthRES(rToken, generateToken(user));
    }

    @Transactional
    @Override
    public AuthRES authWithGitHubAcc(HttpServletRequest request, String code) {
        OAuthResponse info = oAuthFacade.getGitHubUser(code);
        User user = findOrCreateUserFromOAuthInfo(info, Host.GITHUB);
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