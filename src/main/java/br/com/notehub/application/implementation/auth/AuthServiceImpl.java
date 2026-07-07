package br.com.notehub.application.implementation.auth;

import br.com.notehub.application.dto.oauth.OAuthResponse;
import br.com.notehub.application.dto.response.token.AuthRES;
import br.com.notehub.application.oauth.OAuthFacade;
import br.com.notehub.domain.auth.AuthService;
import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.*;
import br.com.notehub.infra.exception.CustomExceptions;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final OAuthFacade oAuthFacade;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final PasswordEncoder encoder;

    private User createUserIdentity(User user, Host host, OAuthResponse info) {
        UserIdentity identity = UserIdentity.signin(user, host, info.id(), info.email());
        userIdentityRepository.save(identity);
        return user;
    }

    private User createUserWithIdentity(Host host, OAuthResponse info) {
        String username = oAuthFacade.resolveUniqueUsername(info.id(), info.username());
        User user = User.oauthSignup(info.email(), username, info.displayName(), info.avatar());
        userRepository.save(user);
        return createUserIdentity(user, host, info);
    }

    private User findOrCreateUserFromOAuthInfo(Host host, OAuthResponse info) {
        Optional<UserIdentity> existingIdentity = userIdentityRepository.findByHostAndProviderId(host, info.id());
        if (existingIdentity.isPresent()) return existingIdentity.get().getUser();
        Optional<User> existingUser = userRepository.findByEmail(info.email());
        return existingUser
                .map(user -> createUserIdentity(user, host, info))
                .orElseGet(() -> createUserWithIdentity(host, info));
    }

    private void validateHasNoExternalIdentity(User user) {
        if (userIdentityRepository.existsByUser(user)) return;
        throw new CustomExceptions.UserHasNoExternalIdentity();
    }

    @Override
    public String generateSecretKey(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        validateHasNoExternalIdentity(user);
        SecureRandom secureRandom = new SecureRandom();
        Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
    }

    @Transactional
    @Override
    public AuthRES auth(HttpServletRequest request, String identifier, String password) throws BadCredentialsException {

        User user = (identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUsername(identifier))
                .orElseThrow(() -> new BadCredentialsException("identifier"));
        if (!user.isActive()) throw new DisabledException("Email não confirmado");

        boolean matches = encoder.matches(password, user.getPassword());
        if (!matches) throw new BadCredentialsException("password");

        return tokenService.issueSession(request, user);

    }

    @Transactional
    @Override
    public AuthRES authWithGoogleAcc(HttpServletRequest request, String token) {
        OAuthResponse info = oAuthFacade.getGoogleUser(token);
        User user = findOrCreateUserFromOAuthInfo(Host.GOOGLE, info);
        return tokenService.issueSession(request, user);
    }

    @Transactional
    @Override
    public AuthRES authWithGitHubAcc(HttpServletRequest request, String code) {
        OAuthResponse info = oAuthFacade.getGitHubUser(code);
        User user = findOrCreateUserFromOAuthInfo(Host.GITHUB, info);
        return tokenService.issueSession(request, user);
    }

    @Transactional
    @Override
    public void logout(HttpServletRequest request) {
        UUID rToken = tokenService.validateRefreshToken(request);
        tokenService.disconnect(rToken);
    }

}