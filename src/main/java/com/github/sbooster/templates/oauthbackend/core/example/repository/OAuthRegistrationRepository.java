package com.github.sbooster.templates.oauthbackend.core.example.repository;

import com.github.sbooster.templates.oauthbackend.core.example.model.oauth.OAuthRegistration;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OAuthRegistrationRepository extends ReactiveMongoRepository<OAuthRegistration, Long> {
    Mono<OAuthRegistration> getByProviderIdAndProvider(String providerId, OAuthRegistration.Provider provider);

    Mono<Boolean> existsByCredentialsIdAndProvider(Long credentialsId, OAuthRegistration.Provider provider);
}
