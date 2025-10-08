package br.com.notehub.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    private static final String[] PUBLIC_ALL_ROUTES = {
            "/h2-console", "/h2-console/**",
            "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
    };

    private static final String[] PUBLIC_POST_ROUTES = {
            "/api/v1/users/register", "/api/v1/auth/**"
    };

    private static final String[] PUBLIC_DELETE_ROUTES = {
            "/api/v1/auth/**"
    };

    private static final String[] PUBLIC_GET_ROUTES = {
            "/", "/docs",
            "/api/v1/auth/refresh",
            "/api/v1/users", "/api/v1/users/**",
            "/api/v1/notes", "/api/v1/notes/**",
            "/api/v1/flames", "/api/v1/flames/**",
            "/api/v1/replies", "/api/v1/replies/**",
            "/api/v1/mail", "/api/v1/mail/**"
    };

    private static final String[] PRIVATE_GET_ROUTES = {
            "/api/v1/users/activate",
            "/api/v1/notes/private", "/api/v1/notes/private/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.frameOptions(frameOptionsConfig -> frameOptionsConfig.disable()))
                .sessionManagement((sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(PUBLIC_ALL_ROUTES).permitAll();
                    req.requestMatchers(HttpMethod.POST, PUBLIC_POST_ROUTES).permitAll();
                    req.requestMatchers(HttpMethod.DELETE, PUBLIC_DELETE_ROUTES).permitAll();
                    req.requestMatchers(HttpMethod.GET, PUBLIC_GET_ROUTES).permitAll();
                    req.requestMatchers(HttpMethod.GET, PRIVATE_GET_ROUTES).hasRole("BASIC");
                    req.anyRequest().hasRole("BASIC");
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}