package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomAttributeFactoryImpl implements CustomAttributeFactory {

    final String INSERT_STATEMENT = "INSERT INTO analytic_custom_attributes VALUES(?, ?)";
    final String UPDATE_STATEMENT = "UPDATE analytic_custom_attributes SET custom_attribute = ? WHERE event_type = ?";
    final String GET_ALL_QUERY = "SELECT * from analytic_custom_attributes";
    final String EXISTS_EVENT_TYPE = "SELECT count(*) from analytic_custom_attributes WHERE event_type = ?";

    @Override
    public void save(String eventTypeName, Map<String, String> attributes) throws DotDataException {
        if (exists(eventTypeName)) {
            update(eventTypeName, attributes);
        } else {
            create(eventTypeName, attributes);
        }
    }

    private boolean exists(String eventTypeName) throws DotDataException {
        return Long.parseLong(
                new DotConnect().setSQL(EXISTS_EVENT_TYPE)
                    .addParam(eventTypeName)
                    .loadObjectResults()
                    .get(0)
                    .get("count").toString()) > 0;
    }

    private void create(String eventTypeName, Map<String, String> attributes) throws DotDataException {
        new DotConnect().setSQL(INSERT_STATEMENT)
                .addParam(eventTypeName)
                .addJSONParam(attributes)
                .loadResult();
    }

    private void update(String eventTypeName, Map<String, String> attributes) throws DotDataException {
        new DotConnect().setSQL(UPDATE_STATEMENT)
                .addJSONParam(attributes)
                .addParam(eventTypeName)
                .loadResult();
    }


    @Override
    public Map<String, Map<String, String>> getAll() throws DotDataException {
        return new DotConnect().setSQL(GET_ALL_QUERY).loadObjectResults().stream()
                .collect(Collectors.toMap(
                        item -> item.get("event_type").toString(),
                        item -> getCustomAttributes(item)
                ));

    }

    private Map<String, String> getCustomAttributes(Map<String, Object> item) {
        try {
            return JsonUtil.getJsonFromString(item.get("custom_attribute").toString())
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> String.valueOf(entry.getValue())
                    ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
