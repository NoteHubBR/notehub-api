package br.com.notehub.application.dto.response.token;

import br.com.notehub.domain.token.Token;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public record SessionRES(
        UUID id,
        UUID device,
        String createdAt,
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
                token.getDevice(),
                token.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("d/M/yy HH:mm", Locale.of("pt-BR"))),
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