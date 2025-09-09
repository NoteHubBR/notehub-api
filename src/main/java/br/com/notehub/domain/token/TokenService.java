package br.com.notehub.domain.token;

import br.com.notehub.application.dto.response.token.AuthRES;
import br.com.notehub.domain.user.User;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public interface TokenService {

    Instant getExpirationTime(String tokenType);

    String generateToken(User user);

    String generateActivationToken(User user);

    String generatePasswordChangeToken(String email);

    String generateEmailChangeToken(String email);

    String validateToken(String accessToken);

    AuthRES auth(HttpServletRequest request, String username, String password) throws BadCredentialsException;

    AuthRES authWithGoogleAcc(HttpServletRequest request, String token);

    AuthRES authWithGitHubAcc(HttpServletRequest request, String code);

    AuthRES recreateToken(HttpServletRequest request) throws TokenExpiredException;

    void logout(HttpServletRequest request);

    void cleanExpiredTokens();

}