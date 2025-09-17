package br.com.notehub.domain.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class HostConverter implements AttributeConverter<Host, String> {

    @Override
    public String convertToDatabaseColumn(Host host) {
        return host.getHost();
    }

    @Override
    public Host convertToEntityAttribute(String host) {
        if (host == null) return null;
        for (Host h : Host.values()) {
            if (h.getHost().equals(host)) {
                return h;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + host);
    }

}