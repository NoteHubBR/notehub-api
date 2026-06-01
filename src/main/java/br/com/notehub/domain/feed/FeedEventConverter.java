package br.com.notehub.domain.feed;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FeedEventConverter implements AttributeConverter<FeedEvent, String> {

    @Override
    public String convertToDatabaseColumn(FeedEvent event) {
        return event.getEvent();
    }

    @Override
    public FeedEvent convertToEntityAttribute(String event) {
        if (event == null) return null;
        for (FeedEvent fe : FeedEvent.values()) {
            if (fe.getEvent().equals(event)) {
                return fe;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + event);
    }

}