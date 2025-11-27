package br.com.notehub.infra.security;

import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    @Lazy
    private TokenService service;

    @Autowired
    private UserRepository repository;

    private void throwFilterException(HttpServletResponse response, String field, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"field\": \"%s\", \"message\": \"%s\"}", field, message);
        response.getWriter().write(json);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        return header.replace("Bearer ", "");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = getToken(request);
        if (request.getServletPath().contains("/payment/stripe/sponsorship/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (accessToken != null) {
            try {
                UUID id = UUID.fromString(service.validateToken(accessToken));
                User user = repository.findById(id).orElseThrow(EntityNotFoundException::new);
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException | EntityNotFoundException ex) {
                try {
                    String email = service.validateToken(accessToken);
                    User user = repository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (EntityNotFoundException exception) {
                    throwFilterException(response, "user", "Usuário não encontrado.");
                    return;
                }
            } catch (JWTVerificationException exception) {
                throwFilterException(response, "token", exception.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

}