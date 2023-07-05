package com.dotcms.jitsu;

import com.dotmarketing.util.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Payload for a JITSU Event
 *
 * @see EventLogWebInterceptor
 * @see EventLogSubmitter
 * @see EventLogRunnable
 */
public class EventsPayload {
    private JSONObject jsonObject;
    final List<LiteExperiment> shortExperiments = new ArrayList<>();

    public EventsPayload(final Map<String, Object> payload) {
        jsonObject = new JSONObject(payload);
    }

    public void put(final String key, final String value) {
        jsonObject.put(key, value);
    }

    public void addExperiment(final String name, final String runningId, final String variant, final String lookBackWindow){
        shortExperiments.add(new LiteExperiment(name, runningId, variant, lookBackWindow));
    }

    public Iterable<EventPayload> payloads() {
        final String jsonObjectString = jsonObject.toString();
        final List<EventPayload> eventPayloads = new ArrayList<>();

        for (LiteExperiment shortExperiment : shortExperiments) {
            final JSONObject experimentJsonPayload = new JSONObject(jsonObjectString);

            experimentJsonPayload.put("experiment", shortExperiment.name);
            experimentJsonPayload.put("runningId", shortExperiment.runningId);
            experimentJsonPayload.put("variant", shortExperiment.variant);
            experimentJsonPayload.put("lookBackWindow", shortExperiment.lookBackWindow);

            eventPayloads.add(new EventPayload(experimentJsonPayload));
        }

        return eventPayloads;
    }

    public static class EventPayload {
        private JSONObject jsonObject;

        private EventPayload(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        @Override
        public String toString() {
            return jsonObject.toString();
        }
    }

    private static class LiteExperiment {
        final String name;
        final String variant;
        final String lookBackWindow;
        final String runningId;

        public LiteExperiment(final String name, final String runningId, final String variant,
                final String lookBackWindow) {
            this.name = name;
            this.variant = variant;
            this.lookBackWindow = lookBackWindow;
            this.runningId = runningId;
        }
    }

}
