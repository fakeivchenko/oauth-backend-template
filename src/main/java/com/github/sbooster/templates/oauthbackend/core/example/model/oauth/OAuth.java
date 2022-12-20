package com.github.sbooster.templates.oauthbackend.core.example.model.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.reactive.function.BodyInserters;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuth {
    public record OAuthRequest(
            String code
    ) {

    }

    public record TokenRequest(
            String clientId,
            String clientSecret,
            String code,
            String grantType,
            String redirectUri
    ) {
        public TokenRequest(String clientId, String clientSecret, String code, String redirectUri) {
            this(clientId, clientSecret, code, "authorization_code", redirectUri);
        }

        public BodyInserters.FormInserter<String> formData() {
            return BodyInserters.fromFormData("client_id", clientId)
                    .with("client_secret", clientSecret)
                    .with("code", code)
                    .with("grant_type", grantType)
                    .with("redirectUri", redirectUri);
        }

        public String uriQuery() {
            return "?client_id=" + clientId +
                    "&redirect_uri=" + redirectUri +
                    "&client_secret=" + clientSecret +
                    "&code=" + code;
        }
    }

    public record TokenResponse(
            @JsonProperty("access_token")
            String accessToken,
            @JsonProperty("token_type")
            String tokenType
    ) {

    }
}
