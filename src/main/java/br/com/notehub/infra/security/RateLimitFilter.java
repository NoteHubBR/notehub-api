package br.com.notehub.infra.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int MAX_CACHE_SIZE = 10_000;
    private static final int REQUESTS_PER_MINUTE = 60;
    private static final int PENALTY_THRESHOLD = 10;
    private static final long PENALTY_DURATION_MS = Duration.ofMinutes(5).toMillis();

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final Map<String, PenaltyEntry> penalties = new ConcurrentHashMap<>();

    private static class BucketEntry {

        final Bucket bucket;
        long lastAccessTime;
        int violationCount;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessTime = System.currentTimeMillis();
            this.violationCount = 0;
        }

    }

    private record PenaltyEntry(long blockedUntil, int totalViolations) {

        boolean isStillBlocked() {
            return System.currentTimeMillis() < blockedUntil;
        }

        long remainingSeconds() {
            return (blockedUntil - System.currentTimeMillis()) / 1000;
        }

    }

    private Bucket createNewBucket() {
        return Bucket.builder().addLimit(Bandwidth.simple(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (request.getServletPath().contains("/api/v1/payment/stripe/sponsorship/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        PenaltyEntry penalty = penalties.get(clientIp);

        if (penalty != null && penalty.isStillBlocked()) {
            logger.warn(
                    "IP {} está bloqueado temporariamente. Restam {} segundos. Total de violações: {}",
                    clientIp, penalty.remainingSeconds(), penalty.totalViolations
            );
            sendPenaltyResponse(response, penalty.remainingSeconds());
            return;
        }

        if (penalty != null && !penalty.isStillBlocked()) {
            penalties.remove(clientIp);
            logger.info("IP {} liberado após cumprir penalty", clientIp);
        }

        cleanupIfNeeded();

        BucketEntry entry = buckets.computeIfAbsent(clientIp, ip -> new BucketEntry(createNewBucket()));
        entry.lastAccessTime = System.currentTimeMillis();

        if (entry.bucket.tryConsume(1)) {
            entry.violationCount = Math.max(0, entry.violationCount - 1);
            filterChain.doFilter(request, response);
        } else {
            entry.violationCount++;
            logger.warn(
                    "Rate limit excedido para IP: {} no endpoint: {} (violação #{})",
                    clientIp, request.getRequestURI(), entry.violationCount
            );
            if (entry.violationCount >= PENALTY_THRESHOLD) {
                long blockedUntil = System.currentTimeMillis() + PENALTY_DURATION_MS;
                penalties.put(clientIp, new PenaltyEntry(blockedUntil, entry.violationCount));
                logger.error(
                        "IP {} BLOQUEADO por 5 minutos após {} violações consecutivas",
                        clientIp, entry.violationCount
                );
                sendPenaltyResponse(response, PENALTY_DURATION_MS / 1000);
            } else sendRateLimitResponse(response, entry.violationCount);
        }

    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_CLIENT_IP"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void cleanupIfNeeded() {
        if (buckets.size() > MAX_CACHE_SIZE) {
            long now = System.currentTimeMillis();
            long tenMinutesAgo = now - Duration.ofMinutes(10).toMillis();
            buckets.entrySet().removeIf(entry ->
                    entry.getValue().lastAccessTime < tenMinutesAgo
            );
            logger.info("Cache de rate limit limpo. Tamanho atual: {}", buckets.size());
        }
        penalties.entrySet().removeIf(entry -> !entry.getValue().isStillBlocked());
    }

    private void sendRateLimitResponse(HttpServletResponse response, int violationCount) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {
                  "error": "rate_limit",
                  "message": "Muitas requisições. Tente novamente mais tarde.",
                  "limite": %d,
                  "periodo": "1 minuto",
                  "violacoes": %d,
                  "aviso": "Após %d violações consecutivas, você será bloqueado por 5 minutos"
                }
                """.formatted(REQUESTS_PER_MINUTE, violationCount, PENALTY_THRESHOLD));
    }

    private void sendPenaltyResponse(HttpServletResponse response, long remainingSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(remainingSeconds));
        response.getWriter().write("""
                {
                  "error": "temporarily_blocked",
                  "message": "Você foi temporariamente bloqueado devido a múltiplas violações de rate limit.",
                  "blocked_for_seconds": %d,
                  "retry_after": "%d minutos"
                }
                """.formatted(remainingSeconds, remainingSeconds / 60));
    }

}