package br.com.notehub.domain.user;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Host {

    GOOGLE("Google"),
    GITHUB("GitHub");

    private final String host;

    Host(String host) {
        this.host = host;
    }

    @JsonValue
    public String getHost() {
        return host;
    }

}