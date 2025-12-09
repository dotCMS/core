package com.dotcms.ai.api;

import java.util.Objects;

/**
 * Similarity operators typically used in ANN/vector search.
 * @author jsanca
 */
public enum SimilarityOperator {
    COSINE,          // higher is better ([-1..1], usually normalized to [0..1]) **<=>**
    INNER_PRODUCT,   // higher is better **<#>**
    EUCLIDEAN;        // lower distance is better (implementations may invert to a score) **<->**

    public static SimilarityOperator fromString(final String similarityOperator) {

        final String operator = Objects.isNull(similarityOperator) ? "<=>" : similarityOperator;

        switch (operator) {
            case "<#>":
                return SimilarityOperator.INNER_PRODUCT;
            case "<->":
                return SimilarityOperator.EUCLIDEAN;
        }

        return COSINE;
    }
}
