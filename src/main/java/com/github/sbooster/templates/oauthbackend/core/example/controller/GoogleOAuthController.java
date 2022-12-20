package com.github.sbooster.templates.oauthbackend.core.example.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.sbooster.templates.oauthbackend.core.example.model.Credentials;
import com.github.sbooster.templates.oauthbackend.core.example.model.CredentialsToken;
import com.github.sbooster.templates.oauthbackend.core.example.model.oauth.OAuthRegistration;
import com.github.sbooster.templates.oauthbackend.core.example.service.CredentialsService;
import com.github.sbooster.templates.oauthbackend.core.example.service.OAuthRegistrationService;
import com.github.sbooster.templates.oauthbackend.core.example.model.oauth.OAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Controller
public class GoogleOAuthController {
    private final WebClient webClient;
    private final CredentialsService credentialsService;
    private final OAuthRegistrationService oAuthRegistrationService;

    @Value("${sbooster.oauth.google.clientid}")
    private String clientId;
    @Value("${sbooster.oauth.google.clientsecret}")
    private String clientSecret;
    @Value("${sbooster.oauth.google.redirecturi}")
    private String redirectUri;

    public GoogleOAuthController(WebClient webClient, CredentialsService credentialsService, OAuthRegistrationService oAuthRegistrationService) {
        this.webClient = webClient;
        this.credentialsService = credentialsService;
        this.oAuthRegistrationService = oAuthRegistrationService;
    }

    @MessageMapping("continueWithGoogle")
    @PreAuthorize("isAnonymous()")
    public Mono<CredentialsToken> continueWithGoogle(@RequestBody OAuth.OAuthRequest request) {
        OAuth.TokenRequest tokenRequest =
                new OAuth.TokenRequest(clientId, clientSecret, request.code(), redirectUri);
        WebClient.ResponseSpec responseSpec = webClient.post()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(tokenRequest.formData())
                .retrieve();
        return responseSpec.bodyToMono(OAuth.TokenResponse.class)
                .flatMap(tokenResponse -> {
                    return webClient.get()
                            .uri("https://www.googleapis.com/userinfo/v2/me")
                            .header("Authorization", "Bearer " + tokenResponse.accessToken())
                            .retrieve()
                            .bodyToMono(GoogleUserData.class);
                })
                .flatMap(this::processGoogleUserData);
    }

    private Mono<CredentialsToken> processGoogleUserData(GoogleUserData googleUserData) {
        return oAuthRegistrationService.getByProviderId(googleUserData.id, OAuthRegistration.Provider.GOOGLE)
                .onErrorResume(ignored -> {
                    return credentialsService.getByUsername(googleUserData.email)
                            .onErrorResume(e -> credentialsService.create(googleUserData.email, "pass"))
                            .flatMap(credentials -> oAuthRegistrationService.create(credentials.getId(), googleUserData.id, OAuthRegistration.Provider.GOOGLE));
                })
                .flatMap(oAuthRegistration -> credentialsService.getById(oAuthRegistration.getCredentialsId()))
                .map(Credentials::generateToken);
    }

    public record GoogleUserData(
            String id,
            String email,
            @JsonProperty("verified_email")
            Boolean verifiedEmail,
            String name,
            @JsonProperty("given_name")
            String givenName,
            @JsonProperty("family_name")
            String familyName,
            String picture,
            String locale
    ) {

    }
}
