package com.dotcms.content.index.domain;

/**
 * Represents the relation of the total hits to the actual number of hits.
 * This enum mirrors the Elasticsearch TotalHits.Relation enum.
 *
 * @author Fabrizio Araya
 */
public enum Relation {
    /**
     * The total hit count is equal to the value.
     */
    EQUAL_TO,

    /**
     * The total hit count is greater than or equal to the value.
     * This occurs when the search engine stops counting after reaching a threshold.
     */
    GREATER_THAN_OR_EQUAL_TO;

    /**
     * Converts from Elasticsearch TotalHits.Relation to our domain Relation.
     *
     * @param esRelation the Elasticsearch relation
     * @return the corresponding domain relation
     */
    public static Relation from(org.apache.lucene.search.TotalHits.Relation esRelation) {
        if (esRelation == null) {
            return EQUAL_TO; // Default fallback
        }

        switch (esRelation) {
            case EQUAL_TO:
                return EQUAL_TO;
            case GREATER_THAN_OR_EQUAL_TO:
                return GREATER_THAN_OR_EQUAL_TO;
            default:
                return EQUAL_TO; // Fallback for unknown values
        }
    }
}