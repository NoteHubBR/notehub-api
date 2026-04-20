package br.com.notehub.application.dto.response.token;

import br.com.notehub.domain.token.Token;

import java.time.Instant;
import java.util.UUID;

public record SessionRES(
        UUID id,
        Instant createdAt,
        String ip,
        String deviceType,
        String deviceBrand,
        String deviceModel,
        String os,
        String browser,
        String country,
        String region,
        String city
) {
    public SessionRES(Token token) {
        this(
                token.getId(),
                token.getCreatedAt(),
                token.getIp(),
                token.getDeviceType(),
                token.getDeviceBrand(),
                token.getDeviceModel(),
                token.getOs(),
                token.getBrowser(),
                token.getCountry(),
                token.getRegion(),
                token.getCity()
        );
    }
}