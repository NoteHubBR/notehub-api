package br.com.notehub.application.dto.payment;

public record SponsorshipStatusRES(
        String sessionId,
        String paymentStatus,
        String status,
        String locale,
        String currency,
        Long amountTotal
) {
}