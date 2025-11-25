package com.dotcms.jitsu;

import com.dotcms.util.JsonUtil;
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
    protected JSONObject jsonObject;
    final List<LiteExperiment> shortExperiments = new ArrayList<>();

    public EventsPayload(final Map<String, Object> payload) {
        jsonObject = new JSONObject(payload);
    }

    public void put(final String key, final String value) {
        jsonObject.put(key, value);
    }

    public void addExperiment(final Map<String, Object> experimentFromEvent){

        shortExperiments.add(new LiteExperiment(experimentFromEvent));
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
            experimentJsonPayload.put("isExperimentPage", shortExperiment.isExperimentPage);
            experimentJsonPayload.put("isTargetPage", shortExperiment.isTargetPage);

            eventPayloads.add(new EventPayload(experimentJsonPayload));
        }

        return eventPayloads;
    }

    public boolean isEmpty() {
        return shortExperiments.isEmpty();
    }

    public static class EventPayload {
        private JSONObject jsonObject;

        public EventPayload(final JSONObject jsonObject) {
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
        final boolean isExperimentPage;
        final boolean isTargetPage;

        public LiteExperiment(final Map<String, Object> experimentFromEvent) {

            this.name = experimentFromEvent.get("experiment").toString();
            this.runningId = experimentFromEvent.get("runningId").toString();
            this.variant =  experimentFromEvent.get("variant").toString();
            this.lookBackWindow = experimentFromEvent.get("lookBackWindow").toString();
            this.isExperimentPage = (Boolean) experimentFromEvent.get("isExperimentPage");
            this.isTargetPage = (Boolean) experimentFromEvent.get("isTargetPage");

        }
    }

}
