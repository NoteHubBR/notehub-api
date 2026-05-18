package br.com.notehub.domain.feed;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum FeedEvent {

    USER_FOLLOWED("User_Followed"),
    NOTE_CREATED("Note_Created"),
    NOTE_FLAMED("Note_Flamed"),
    NOTE_COMMENTED("Note_Commented");

    private final String event;

    FeedEvent(String event) {
        this.event = event;
    }

    @JsonValue
    public String getEvent() {
        return event;
    }

}