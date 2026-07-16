package com.dotcms.rest.api.v1.content.search.strategies;

/**
 * This Enum allows you to reference the different types of Field Handlers that provide a Lucene
 * query format for a given group of searchable fields in dotCMS. Such field groups share the same
 * query formatting rules, which allow them to be reusable.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public enum FieldHandlerId {

    GLOBAL_SEARCH("globalSearch"),
    CONTENT_TYPE_IDS("contentTypeIds"),
    // Even though the same field in the UI is used to select the Site or the Folder, they are
    // separate handlers here because Lucene uses a different search term for each of them
    SITE_ID("siteId"),
    FOLDER_ID("folderId"),
    TEXT("text"),
    BINARY("binary"),
    DATE_TIME("dateTime"),
    KEY_VALUE("keyValue"),
    CATEGORY("category"),
    RELATIONSHIP("relationship"),
    TAG("tag"),
    VARIANT("variant"),
    LANGUAGE("language"),
    WORKFLOW_SCHEME("workflowScheme"),
    WORKFLOW_STEP("workflowStep"),
    ARCHIVED_CONTENT("archivedContent"),
    LOCKED_CONTENT("lockedContent"),
    LIVE_CONTENT("liveContent"),
    DEFAULT("default");

    private final String id;

    /**
     * Enum constructor.
     *
     * @param id The unique identifier for the Field Handler.
     */
    FieldHandlerId(final String id) {
        this.id = id;
    }

    /**
     * Returns the Field Handler based on the unique identifier.
     *
     * @param id The unique identifier for the Field Handler.
     *
     * @return The {@link FieldHandlerId} based on the unique identifier.
     */
    public static FieldHandlerId fromId(final String id) {
        for (final FieldHandlerId strategyId : FieldHandlerId.values()) {
            if (strategyId.id().equals(id)) {
                return strategyId;
            }
        }
        throw new IllegalArgumentException("No strategy found for id: " + id);
    }

    /**
     * Returns the unique identifier for the Field Handler.
     *
     * @return The unique identifier for the Field Handler.
     */
    public String id() {
        return id;
    }

}
