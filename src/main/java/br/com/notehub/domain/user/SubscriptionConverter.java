package br.com.notehub.domain.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SubscriptionConverter implements AttributeConverter<Subscription, String> {

    @Override
    public String convertToDatabaseColumn(Subscription subscription) {
        return subscription.getSubscription();
    }

    @Override
    public Subscription convertToEntityAttribute(String subscription) {
        if (subscription == null) return null;
        return Subscription.from(subscription);
    }

}