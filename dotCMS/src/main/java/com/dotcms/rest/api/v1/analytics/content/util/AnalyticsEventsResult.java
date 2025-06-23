package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize(using = AnalyticsEventsResultSerializer.class)
public class AnalyticsEventsResult {
    private final ResponseStatus status;
    private final List<AnalyticsValidatorUtil.Error> errors;
    private final long success;
    private final long failed;

    public AnalyticsEventsResult(
            final ResponseStatus status,
            final List<AnalyticsValidatorUtil.Error> errors,
            final long success,
            final long failed) {

        this.status = status;
        this.errors = errors;
        this.success = success;
        this.failed = failed;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public List<AnalyticsValidatorUtil.Error> getErrors() {
        return errors;
    }

    public long getSuccess() {
        return success;
    }

    public long getFailed() {
        return failed;
    }

    public enum ResponseStatus{
        SUCCESS,
        PARTIAL_SUCCESS,
        ERROR;
    }

    public static class Builder {
        private List<AnalyticsValidatorUtil.Error> globalErrors;
        private List<AnalyticsValidatorUtil.Error> eventsErrors;
        private  int totalEvents;

        public Builder addGlobalErrors(final List<AnalyticsValidatorUtil.Error> errors){
            this.globalErrors = errors;
            return this;
        }

        public Builder addEventsErrors(final List<AnalyticsValidatorUtil.Error> errors){
            this.eventsErrors = errors;
            return this;
        }

        public Builder addTotalEvents(final int totalEvents){
            this.totalEvents = totalEvents;
            return this;
        }

        public AnalyticsEventsResult build() {
            final long failedEventsCount = getFailedEventsCount();
            final long success = totalEvents - failedEventsCount;

            if (globalErrors!= null && !globalErrors.isEmpty()) {
                return new AnalyticsEventsResult(ResponseStatus.ERROR, globalErrors, -1, -1);
            }

            if (success == 0) {
                return new AnalyticsEventsResult(ResponseStatus.ERROR, eventsErrors, 0, failedEventsCount);
            }

            final ResponseStatus status = failedEventsCount == 0 ? ResponseStatus.SUCCESS :
                    ResponseStatus.PARTIAL_SUCCESS;


            return new AnalyticsEventsResult(status, eventsErrors, success, failedEventsCount);
        }

        private long getFailedEventsCount() {
            return (eventsErrors == null || eventsErrors.isEmpty()) ? 0 :
                    eventsErrors.stream().map(AnalyticsValidatorUtil.Error::getEventIndex).distinct().count();
        }
    }

}
