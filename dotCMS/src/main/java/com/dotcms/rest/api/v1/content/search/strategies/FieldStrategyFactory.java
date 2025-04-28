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
        // Here, we're associating each Field Handler with its corresponding Field Strategy for the
        // System-Searchable fields/attributes. It's worth noting that These are NOT actual Content
        // Type fields per se. But they allow users to query contents via Lucene
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.CONTENT_TYPE_IDS, new ContentTypesFieldStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.SITE_ID, new SiteAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.FOLDER_ID, new FolderAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.GLOBAL_SEARCH, new GlobalSearchAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.VARIANT, new VariantAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.LANGUAGE, new LanguageAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.WORKFLOW_SCHEME, new WorkflowSchemeAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.WORKFLOW_STEP, new WorkflowStepAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.ARCHIVED_CONTENT, new ArchivedContentAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.LOCKED_CONTENT, new LockedContentAttributeStrategy());
        SYSTEM_FIELD_STRATEGY_MAP.put(FieldHandlerId.LIVE_CONTENT, new UnpublishedContentAttributeStrategy());

        // Here, we're associating each Field Handler with its corresponding Field Strategy for the
        // group of actual Content Type fields that share the same query formatting, escaping and
        // processing for their specific values
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.TEXT, new TextFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.BINARY, new BinaryFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.DATE_TIME, new DateTimeFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.KEY_VALUE, new KeyValueFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.CATEGORY, new CategoryFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.RELATIONSHIP, new RelationshipFieldStrategy());
        SEARCHABLE_FIELD_STRATEGY_MAP.put(FieldHandlerId.TAG, new TagFieldStrategy());
    }

    /**
     * Adds a new Field Strategy to the group of Searchable Field Strategies. A Field Strategy
     * defines the way a specific type of field can be queried via Lucene.
     * <p>These types of strategies must always match one or more fields that can be added to a
     * Content Type's definition. For instance, the {@link FieldHandlerId#TEXT} related to the
     * {@link TextFieldStrategy} takes care of handling all sort of text-based fields: Text, Text
     * Area, WYSIWYG, Block Editor, Select, and so on. If, for any reason, one of those fields must
     * be handled in a particular way, a new Field Handler ID and Field Strategy
     * must be implemented.</p>
     *
     * @param strategyId The {@link FieldHandlerId} that represents the Field Strategy.
     * @param strategy   The {@link FieldStrategy} implementation that will be used to generate the
     *                   Lucene query.
     */
    public static void addFieldStrategy(final FieldHandlerId strategyId, final FieldStrategy strategy) {
        SEARCHABLE_FIELD_STRATEGY_MAP.put(strategyId, strategy);
    }

    /**
     * Adds a new Field Strategy to the group of System Field Strategies. A Field Strategy of this
     * type represents a filtering value that does NOT match a User Searchable field in a Content
     * Type. For instance, the Lucene term {@code +locked} -- which allows you to filter locked or
     * unlocked content -- is a type of System Field Strategy. They usually have their own Field
     * Handler ID and Field Strategy.
     *
     * @param strategyId The {@link FieldHandlerId} that represents the Field Strategy.
     * @param strategy   The {@link FieldStrategy} implementation that will be used to generate the
     *                   Lucene query.
     */
    public static void addSystemFieldStrategy(final FieldHandlerId strategyId, final FieldStrategy strategy) {
        SYSTEM_FIELD_STRATEGY_MAP.put(strategyId, strategy);
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
