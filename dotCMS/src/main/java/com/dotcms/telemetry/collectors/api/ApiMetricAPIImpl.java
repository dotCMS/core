package com.dotcms.telemetry.collectors.api;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

@ApplicationScoped
public class ApiMetricAPIImpl implements ApiMetricAPI {

    final RequestHashCalculator requestHashCalculator = new RequestHashCalculator();
    final ApiMetricFactory apiMetricFactory;

    @Inject
    public ApiMetricAPIImpl(final ApiMetricFactory apiMetricFactory) {
        this.apiMetricFactory = apiMetricFactory;
    }

    @CloseDBIfOpened
    @Override
    public Collection<Map<String, Object>> getMetricTemporaryTableData() {
        return this.apiMetricFactory.getMetricTemporaryTableData();
    }

    @Override
    public void save(final ApiMetricType apiMetricType,
                     final ApiMetricWebInterceptor.RereadInputStreamRequest request) {
        final String requestHash = requestHashCalculator.calculate(apiMetricType, request);
        final ApiMetricRequest metricAPIHit = new ApiMetricRequest.Builder()
                .setMetric(apiMetricType.getMetric())
                .setTime(Instant.now())
                .setHash(requestHash)
                .build();
        ApiMetricFactorySubmitter.INSTANCE.saveAsync(metricAPIHit);
    }

    @WrapInTransaction
    @Override
    public void startCollecting() {
        try {
            this.apiMetricFactory.saveStartEvent();
        } catch (final Exception e) {
            Logger.debug(this, "Error saving start event", e);
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void flushTemporaryTable() {
        try {
            this.apiMetricFactory.flushTemporaryTable();
            startCollecting();
        } catch (final Exception e) {
            Logger.debug(this, "Error flushing the temporary table", e);
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void createTemporaryTable() {
        try {
            this.apiMetricFactory.createTemporaryTable();
        } catch (final Exception e) {
            Logger.debug(this, "Error creating the temporary table", e);
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void dropTemporaryTable() {
        try {
            this.apiMetricFactory.dropTemporaryTable();
        } catch (final Exception e) {
            Logger.debug(this, "Error dropping the temporary table", e);
            throw new DotRuntimeException(e);
        }
    }

}
