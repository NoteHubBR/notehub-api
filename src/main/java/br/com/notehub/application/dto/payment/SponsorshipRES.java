package br.com.notehub.application.dto.payment;

import lombok.Builder;

@Builder
public record SponsorshipRES(
        String status,
        String message,
        String sessionId,
        String sessionUrl,
        String uId
) {
}