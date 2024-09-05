package com.dotcms.jitsu;

import com.dotmarketing.util.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used only to override an undesired behavior in the dotCMS codebase which is based for experiemnts
 * @author jsanca
 */
public class AnalyticsEventsPayload extends EventsPayload {

    public AnalyticsEventsPayload(final Map<String, Object> payload) {
        super(payload);
    }

    @Override
    public Iterable<EventPayload> payloads() {

        final String jsonObjectString = jsonObject.toString();
        final List<EventPayload> eventPayloads = new ArrayList<>();
        final JSONObject analyticsJsonPayload = new JSONObject(jsonObjectString);
        eventPayloads.add(new EventPayload(analyticsJsonPayload));

        return eventPayloads;
    }

}
