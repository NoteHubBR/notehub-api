package br.com.notehub.application.dto.oauth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserResponse(
        @JsonProperty("id") String id,
        @JsonProperty("email") String email,
        @JsonProperty("given_name") String username,
        @JsonProperty("name") String displayName,
        @JsonProperty("picture") String avatar
) {
}