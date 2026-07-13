package br.com.notehub.application.implementation.token;

import br.com.notehub.application.dto.response.auth.AuthRES;
import br.com.notehub.application.geoip.GeoIpService;
import br.com.notehub.domain.token.Token;
import br.com.notehub.domain.token.TokenRepository;
import br.com.notehub.domain.token.TokenService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    @Value("${api.server.security.token.secret}")
    private String secret;

    private final GeoIpService geoService;
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

    private boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip;
        ip = request.getHeader("Fly-Client-IP");
        if (isValidIp(ip)) return ip;
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) return ip.split(",")[0].trim();
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) return ip;
        return request.getRemoteAddr();
    }

    public Instant getExpirationTime(String tokenType) {
        return switch (tokenType) {
            case "refresh" -> LocalDateTime.now().plusDays(30).toInstant(ZoneOffset.of("-03:00"));
            case "access" -> LocalDateTime.now().plusMinutes(30).toInstant(ZoneOffset.of("-03:00"));
            default -> null;
        };
    }

    private Token generateRefreshToken(HttpServletRequest request, User user) {
        String ip = getClientIp(request);
        String agent = request.getHeader("User-Agent");
        UUID device = validateDevice(request);
        Instant expiresAt = getExpirationTime("refresh");
        Token token = new Token(user, ip, agent, device, expiresAt);
        geoService.enrichToken(token, agent, ip);
        return token;
    }

    private Token persistRefreshToken(Token token) {
        repository.findByDevice(token.getDevice()).ifPresent(repository::delete);
        repository.save(token);
        return token;
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
    public UUID validateRefreshToken(HttpServletRequest request) {
        String xRefreshToken = request.getHeader("X-Refresh-Token");
        if (xRefreshToken == null) throw new CustomExceptions.MissingRefreshToken();
        UUID rToken;
        try {
            return rToken = UUID.fromString(xRefreshToken);
        } catch (IllegalArgumentException ex) {
            throw new CustomExceptions.InvalidRefreshTokenException();
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
    public AuthRES issueSession(HttpServletRequest request, User user) {
        Token token = persistRefreshToken(generateRefreshToken(request, user));
        return new AuthRES(token, generateToken(user));
    }

    @Transactional
    @Override
    public AuthRES recreateSession(HttpServletRequest request) throws TokenExpiredException {

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

    @Override
    public List<Token> getAllSessions(UUID uId, String password) {
        User user = userRepository.findById(uId).orElseThrow(EntityNotFoundException::new);
        boolean matches = encoder.matches(password, user.getPassword());
        if (!matches) throw new BadCredentialsException("password");
        return repository.findAllByUserId(uId);
    }

    @Override
    public void disconnect(UUID id) {
        Token token = repository.findById(id).orElseThrow(EntityNotFoundException::new);
        repository.delete(token);
    }

    @Override
    public void disconnectAll(HttpServletRequest request, boolean keepCurrentSession, String email) {
        List<Token> connections = repository.findAllByUserEmail(email);
        UUID device = validateDevice(request);
        if (keepCurrentSession) connections = connections.stream().filter(t -> !t.getDevice().equals(device)).toList();
        repository.deleteAll(connections);
    }

    @Override
    public void cleanExpiredTokens() {
        List<Token> expiredTokens = repository.findExpiredTokens(Instant.now());
        repository.deleteAll(expiredTokens);
    }

}