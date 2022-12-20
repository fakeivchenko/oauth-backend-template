package com.github.sbooster.templates.oauthbackend.core.example.controller;

import com.github.sbooster.templates.oauthbackend.core.example.service.CredentialsService;
import com.github.sbooster.templates.oauthbackend.core.example.model.Credentials;
import com.github.sbooster.templates.oauthbackend.core.example.model.CredentialsToken;
import com.github.sbooster.templates.oauthbackend.core.example.model.oauth.OAuth;
import com.github.sbooster.templates.oauthbackend.core.example.model.oauth.OAuthRegistration;
import com.github.sbooster.templates.oauthbackend.core.example.service.OAuthRegistrationService;
import com.github.sbooster.templates.oauthbackend.util.OAuthUtils;
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
public class FacebookOAuthController {
    private final WebClient webClient;
    private final CredentialsService credentialsService;
    private final OAuthRegistrationService oAuthRegistrationService;

    @Value("${sbooster.oauth.facebook.clientid}")
    private String clientId;
    @Value("${sbooster.oauth.facebook.clientsecret}")
    private String clientSecret;
    @Value("${sbooster.oauth.facebook.redirecturi}")
    private String redirectUri;

    public FacebookOAuthController(WebClient webClient, CredentialsService credentialsService, OAuthRegistrationService oAuthRegistrationService) {
        this.webClient = webClient;
        this.credentialsService = credentialsService;
        this.oAuthRegistrationService = oAuthRegistrationService;
    }

    @MessageMapping("continueWithFacebook")
    @PreAuthorize("isAnonymous()")
    public Mono<CredentialsToken> continueWithFacebook(@RequestBody OAuth.OAuthRequest request) {
        OAuth.TokenRequest tokenRequest =
                new OAuth.TokenRequest(clientId, clientSecret, request.code(), redirectUri);
        WebClient.ResponseSpec responseSpec = webClient.get()
                .uri(URI.create("https://graph.facebook.com/v15.0/oauth/access_token" + tokenRequest.uriQuery()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(OAuth.TokenResponse.class)
                .flatMap(tokenResponse -> {
                    return webClient.get()
                            .uri("https://graph.facebook.com/v15.0/me?fields=email")
                            .header("Authorization", "Bearer " + tokenResponse.accessToken())
                            .retrieve()
                            .bodyToMono(FacebookUserData.class);
                })
                .flatMap(this::processFacebookUserData);
    }

    private Mono<CredentialsToken> processFacebookUserData(FacebookUserData facebookUserData) {
        return oAuthRegistrationService.getByProviderId(facebookUserData.id, OAuthRegistration.Provider.FACEBOOK)
                .onErrorResume(ignored -> {
                    return credentialsService.getByUsername(facebookUserData.email)
                            .onErrorResume(e -> credentialsService.create(facebookUserData.email, OAuthUtils.generateRandomPassword()))
                            .flatMap(credentials -> oAuthRegistrationService.create(credentials.getId(), facebookUserData.id, OAuthRegistration.Provider.FACEBOOK));
                })
                .flatMap(oAuthRegistration -> credentialsService.getById(oAuthRegistration.getCredentialsId()))
                .map(Credentials::generateToken);
    }

    public record FacebookUserData(
            String id,
            String email
    ) {

    }
}
