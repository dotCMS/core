package com.dotcms.analytics.attributes;

import com.dotcms.util.JsonUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JDBC-based implementation of {@link CustomAttributeFactory} using {@link DotConnect} to persist
 * custom attribute mappings in the analytic_custom_attributes table.
 */
@ApplicationScoped
public class CustomAttributeFactoryImpl implements CustomAttributeFactory {

    final String INSERT_STATEMENT = "INSERT INTO analytic_custom_attributes VALUES(?, ?)";
    final String UPDATE_STATEMENT = "UPDATE analytic_custom_attributes SET custom_attribute = ? WHERE event_type = ?";
    final String GET_ALL_QUERY = "SELECT * from analytic_custom_attributes";
    final String EXISTS_EVENT_TYPE = "SELECT count(*) from analytic_custom_attributes WHERE event_type = ?";

    /** {@inheritDoc} */
    @Override
    public void save(String eventTypeName, Map<String, String> attributes) throws DotDataException {
        Logger.debug(CustomAttributeFactoryImpl.class, () -> "Saving attributes for eventType='" + eventTypeName + "' with " + (attributes != null ? attributes.size() : 0) + " attribute(s)");

        if (exists(eventTypeName)) {
            Logger.debug(CustomAttributeFactoryImpl.class, () -> "Existing mapping found for eventType='" + eventTypeName + "'. Updating...");
            update(eventTypeName, attributes);
        } else {
            Logger.debug(CustomAttributeFactoryImpl.class, () -> "No mapping found for eventType='" + eventTypeName + "'. Creating new record...");
            create(eventTypeName, attributes);
        }
    }

    private boolean exists(String eventTypeName) throws DotDataException {
        Logger.debug(CustomAttributeFactoryImpl.class, () -> "Checking existence of mapping for eventType='" + eventTypeName + "'");

        long count = Long.parseLong(
                new DotConnect().setSQL(EXISTS_EVENT_TYPE)
                    .addParam(eventTypeName)
                    .loadObjectResults()
                    .get(0)
                    .get("count").toString());

        Logger.debug(CustomAttributeFactoryImpl.class, () -> "Existence check for eventType='" + eventTypeName + "' returned count=" + count);
        return count > 0;
    }

    private void create(String eventTypeName, Map<String, String> attributes) throws DotDataException {
        Logger.debug(CustomAttributeFactoryImpl.class, () -> "Creating mapping for eventType='" + eventTypeName + "' with " + (attributes != null ? attributes.size() : 0) + " attribute(s)");

        new DotConnect().setSQL(INSERT_STATEMENT)
                .addParam(eventTypeName)
                .addJSONParam(attributes)
                .loadResult();

        Logger.info(CustomAttributeFactoryImpl.class, () -> "Insert complete for eventType='" + eventTypeName + "'");
    }

    private void update(String eventTypeName, Map<String, String> attributes) throws DotDataException {
        Logger.debug(CustomAttributeFactoryImpl.class, () -> "Updating mapping for eventType='" + eventTypeName + "' with " + (attributes != null ? attributes.size() : 0) + " attribute(s)");

        new DotConnect().setSQL(UPDATE_STATEMENT)
                .addJSONParam(attributes)
                .addParam(eventTypeName)
                .loadResult();

        Logger.info(CustomAttributeFactoryImpl.class, () -> "Update complete for eventType='" + eventTypeName + "'");
    }


    /** {@inheritDoc} */
    @Override
    public Map<String, Map<String, String>> getAll() throws DotDataException {
        Logger.debug(CustomAttributeFactoryImpl.class, () -> "Fetching all custom attribute mappings from database");

        final java.util.List<java.util.Map<String, Object>> rows = new DotConnect().setSQL(GET_ALL_QUERY).loadObjectResults();

        Logger.info(CustomAttributeFactoryImpl.class, () -> "Retrieved " + rows.size() + " row(s) from analytic_custom_attributes");

        return rows.stream()
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
            Logger.error(CustomAttributeFactoryImpl.class, "Failed to parse custom_attribute JSON for event_type row: " + item, e);
            throw new RuntimeException(e);
        }
    }
}
