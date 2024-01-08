package com.dotcms.analytics.experience.metric.collector;

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.Optional;

public interface MetricCollector<T> {

    default Optional<Object> collect(final User user) {
        try {
            return Optional.of(innerCollect(user));
        } catch (Exception e) {
            Logger.error(this.getClass(), e);
            return Optional.empty();
        }
    }

    T innerCollect(final User user) throws Exception;
}
