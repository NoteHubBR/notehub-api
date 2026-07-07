package br.com.notehub.domain.auth;

import br.com.notehub.application.dto.response.token.AuthRES;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {

    String generateSecretKey(String email);

    AuthRES auth(HttpServletRequest request, String identifier, String password) throws BadCredentialsException;

    AuthRES authWithGoogleAcc(HttpServletRequest request, String token);

    AuthRES authWithGitHubAcc(HttpServletRequest request, String code);

    void logout(HttpServletRequest request);

}