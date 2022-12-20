package com.github.sbooster.templates.oauthbackend.core.example.service;

import com.github.sbooster.templates.oauthbackend.core.example.exception.UserNotFoundException;
import com.github.sbooster.templates.oauthbackend.core.example.exception.UsernameAlreadyRegisteredException;
import com.github.sbooster.templates.oauthbackend.core.example.model.oauth.OAuthRegistration;
import com.github.sbooster.templates.oauthbackend.core.example.repository.OAuthRegistrationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OAuthRegistrationService {
    private final OAuthRegistrationRepository oAuthRegistrationRepository;

    public OAuthRegistrationService(OAuthRegistrationRepository oAuthRegistrationRepository) {
        this.oAuthRegistrationRepository = oAuthRegistrationRepository;
    }

    public Mono<OAuthRegistration> getByProviderId(String providerId, OAuthRegistration.Provider provider) {
        return oAuthRegistrationRepository.getByProviderIdAndProvider(providerId, provider)
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException("No user associated with specified oauth provider and provider id")));
    }

    public Mono<OAuthRegistration> create(Long credentialsId, String providerId, OAuthRegistration.Provider provider) {
        return oAuthRegistrationRepository.existsByCredentialsIdAndProvider(credentialsId, provider)
                .filter(value -> !value)
                .switchIfEmpty(Mono.error(() -> new UsernameAlreadyRegisteredException("Specified user already has registered with this oauth provider")))
                .flatMap(value -> oAuthRegistrationRepository.save(OAuthRegistration.builder()
                        .credentialsId(credentialsId)
                        .providerId(providerId)
                        .provider(provider)
                        .build()));
    }
}
