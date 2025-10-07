package br.com.notehub.domain.user;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Subscription {

    RELEASE("Release"),
    MAINTENANCE("Maintenance");

    private final String subscription;

    Subscription(String subscription) {
        this.subscription = subscription;
    }

    @JsonValue
    public String getSubscription() {
        return subscription;
    }

    public static Subscription from(String subscription) {
        for (Subscription sub : Subscription.values())
            if (sub.getSubscription().equals(subscription) || sub.getSubscription().equalsIgnoreCase(subscription))
                return sub;
        throw new IllegalArgumentException("Unknown value: " + subscription);
    }

}