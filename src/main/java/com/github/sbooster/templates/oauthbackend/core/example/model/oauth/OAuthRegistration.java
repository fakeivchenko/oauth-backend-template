package com.github.sbooster.templates.oauthbackend.core.example.model.oauth;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Objects;

@Data
@Builder
@Document("oauth-registrations")
public class OAuthRegistration {
    @MongoId(targetType = FieldType.INT64)
    @Builder.Default
    private Long id = (long) Math.abs(Objects.hashCode(ObjectId.get()));
    private Long credentialsId;
    private String providerId;
    private Provider provider;

    public enum Provider {
        GOOGLE, FACEBOOK
    }
}
