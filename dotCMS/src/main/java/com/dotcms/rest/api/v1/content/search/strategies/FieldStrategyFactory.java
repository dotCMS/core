package com.dotcms.rest.api.v1.content.search.strategies;

import java.util.HashMap;
import java.util.Map;

/**
 * This Factory class is responsible for loading the different Field Strategies that generate the
 * Lucene queries. System Field Strategies represent Lucene terms that do not belong to Content
 * Type fields, whereas User Searchable Field Strategies represent the Lucene terms that belong to
 * actual fields in a Contentlet.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class FieldStrategyFactory {

    private static final Map<FieldHandlerId, FieldStrategy> SYSTEM_FIELD_STRATEGY_MAP = new HashMap<>();
    private static final Map<FieldHandlerId, FieldStrategy> SEARCHABLE_FIELD_STRATEGY_MAP = new HashMap<>();

    static {
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.CONTENT_TYPE_IDS, new ContentTypesFieldStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.SITE_ID, new SiteFieldStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.GLOBAL_SEARCH, new GlobalSearchFieldStrategy());

        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.TEXT, new TextFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.BINARY, new BinaryFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.DATE_TIME, new DateTimeFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.KEY_VALUE, new KeyValueFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.CATEGORY, new CategoryFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.RELATIONSHIP, new RelationshipFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.TAG, new TagFieldStrategy());
    }

    /**
     * Returns the appropriate Field Strategy instance based on the Field Handler type; i.e., the
     * type of searchable field that must be used in the resulting Lucene query.
     *
     * @param strategyId The {@link FieldHandlerId} that represents the Field Strategy.
     *
     * @return The {@link FieldStrategy} instance based on the Field Handler type.
     */
    public static FieldStrategy getStrategy(final FieldHandlerId strategyId) {
        if (SYSTEM_FIELD_STRATEGY_MAP.containsKey(strategyId)) {
            return SYSTEM_FIELD_STRATEGY_MAP.get(strategyId);
        } else if (SEARCHABLE_FIELD_STRATEGY_MAP.containsKey(strategyId)) {
            return SEARCHABLE_FIELD_STRATEGY_MAP.get(strategyId);
        } else {
            return new DefaultFieldStrategy();
        }
    }

}
