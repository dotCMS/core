package com.dotcms.jitsu;

import com.dotmarketing.util.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotcms.analytics.track.collectors.Collector.SESSION_NEW;

/**
 * Payload for a JITSU Event when an Experiment is running
 *
 * @see EventLogWebInterceptor
 * @see EventLogSubmitter
 * @see EventLogRunnable
 */
public class ExperimentEventsPayload extends EventsPayload {
    final List<LiteExperiment> shortExperiments = new ArrayList<>();

    public ExperimentEventsPayload(final Map<String, Object> payload) {
        super(new JSONObject(payload));
    }

    public void addExperiment(final Map<String, Object> experimentFromEvent){

        shortExperiments.add(new LiteExperiment(experimentFromEvent));
    }

    @Override
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

            experimentJsonPayload.put(SESSION_NEW, false);

            eventPayloads.add(new EventPayload(experimentJsonPayload));
        }

        return eventPayloads;
    }

    public boolean isEmpty() {
        return shortExperiments.isEmpty();
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
