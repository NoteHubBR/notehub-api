package br.com.notehub.application.dto.oauth.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmailResponse(
        @JsonProperty("email") String email,
        @JsonProperty("primary") Boolean primary,
        @JsonProperty("verified") Boolean verified,
        @JsonProperty("visibility") String visibility
) {
}