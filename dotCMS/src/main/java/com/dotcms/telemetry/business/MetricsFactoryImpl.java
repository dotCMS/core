package com.dotcms.telemetry.business;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class provides the default implementation of the {@link MetricsFactory} interface.
 */
@ApplicationScoped
public class MetricsFactoryImpl implements MetricsFactory {

    @Override
    public Optional<Object> getValue(final String sqlQuery) throws DotDataException {
        final List<Map<String, Object>> loadObjectResults = new DotConnect().setSQL(sqlQuery).loadObjectResults();
        if (loadObjectResults.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadObjectResults.get(0).get("value"));
    }

    @Override
    public Optional<List<String>> getList(final String sqlQuery) throws DotDataException {
        final List<Map<String, Object>> loadObjectResults = new DotConnect().setSQL(sqlQuery).loadObjectResults();
        if (loadObjectResults.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(loadObjectResults.stream()
                .map(item -> item.get("value").toString()).collect(Collectors.toList()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getSchemaDBVersion() throws DotDataException {
        final ArrayList<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT max(db_version) AS version FROM db_version")
                .loadResults();
        return Integer.parseInt(results.get(0).get("version").toString());
    }

}
