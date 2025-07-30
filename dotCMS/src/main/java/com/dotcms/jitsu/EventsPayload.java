package com.dotcms.jitsu;

import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotcms.analytics.track.collectors.Collector.SESSION_NEW;

/**
 * Payload for a JITSU Event
 *
 * @see EventLogWebInterceptor
 * @see EventLogSubmitter
 * @see EventLogRunnable
 */
public abstract class EventsPayload {
    protected JSONObject jsonObject;

    public EventsPayload(final Map<String, Object> payload) {
        jsonObject = new JSONObject(payload);
    }

    public void put(final String key, final String value) {
        jsonObject.put(key, value);
    }


    public abstract Iterable<EventPayload> payloads();

    public static class EventPayload {
        private JSONObject jsonObject;

        public EventPayload(final JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        @Override
        public String toString() {
            return jsonObject.toString();
        }

        public Object get(String key) {
            return jsonObject.get(key);
        }
        public Object remove(String key) {
            return jsonObject.remove(key);
        }

        public boolean contains(String key) {
            return jsonObject.containsKey(key);
        }
    }

}
