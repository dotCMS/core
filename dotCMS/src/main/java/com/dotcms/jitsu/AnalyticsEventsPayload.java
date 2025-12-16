package com.dotcms.jitsu;

import com.dotmarketing.util.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is used only to override an undesired behavior in the dotCMS codebase which is based for experiemnts
 * @author jsanca
 */
public class AnalyticsEventsPayload extends EventsPayload {

    final List<Map<String, Serializable>> payload;

    public AnalyticsEventsPayload(final List<Map<String, Serializable>> payload) {
        super(Map.of()); // by now we run empty this
        this.payload = payload;
    }

    @Override
    public Collection<EventPayload> payloads() {

        final List<EventPayload> eventPayloads = new ArrayList<>();

        for (final var eventMap : payload) {

            eventPayloads.add(new EventPayload(new JSONObject(eventMap)));
        }

        return eventPayloads;
    }

}
