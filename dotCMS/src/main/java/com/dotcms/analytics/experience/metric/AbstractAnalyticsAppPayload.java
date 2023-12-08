package com.dotcms.analytics.experience.metric;

import com.dotcms.analytics.app.AnalyticsApp;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.io.Serializable;

/**
 * Payload to be sent to analytics with resolved {@link AnalyticsApp}.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = AnalyticsAppPayload.class)
@JsonDeserialize(as = AnalyticsAppPayload.class)
public interface AbstractAnalyticsAppPayload<P extends Serializable> {

    AnalyticsApp analyticsApp();

    P payload();

}
