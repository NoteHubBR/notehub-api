package br.com.notehub.domain.token;

import br.com.notehub.application.dto.response.auth.AuthRES;
import br.com.notehub.domain.user.User;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface TokenService {

    String generateToken(User user);

    String generateActivationToken(User user);

    String generatePasswordChangeToken(String email);

    String generateEmailChangeToken(String email);

    UUID validateRefreshToken(HttpServletRequest request);

    String validateToken(String accessToken);

    AuthRES issueSession(HttpServletRequest request, User user);

    AuthRES recreateSession(HttpServletRequest request) throws TokenExpiredException;

    List<Token> getAllSessions(UUID uId, String password);

    void disconnect(UUID id);

    void disconnectAll(HttpServletRequest request, boolean keepCurrentSession, String email);

    void cleanExpiredTokens();

}