package br.com.notehub.domain.feed;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class FeedEventParamConverter implements Converter<String, FeedEvent> {

    @Override
    public FeedEvent convert(@NonNull String source) {
        for (FeedEvent fe : FeedEvent.values()) {
            if (fe.getEvent().equals(source) || fe.name().equals(source)) return fe;
        }
        throw new IllegalArgumentException("Unknown source: " + source);
    }

}