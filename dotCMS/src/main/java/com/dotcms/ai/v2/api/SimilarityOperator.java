package com.dotcms.ai.v2.api;

/**
 * Similarity operators typically used in ANN/vector search.
 * @author jsanca
 */
public enum SimilarityOperator {
    COSINE,          // higher is better ([-1..1], usually normalized to [0..1])
    INNER_PRODUCT,   // higher is better
    EUCLIDEAN        // lower distance is better (implementations may invert to a score)
}
