package com.dotcms.ai.v2.api;


import java.util.Map;

/**
 * Normalizes and validates CompletionRequest defaults for safer downstream usage.
 * @author jsanca
 */
public final class CompletionNormalizer {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 64;

    private CompletionNormalizer() {}

    /**
     * Normalize null/invalid fields to sensible defaults.
     *
     * @param completionRequest Original request (may contain null/invalid values).
     * @return Same instance modified or a copy with normalized fields.
     */
    public static CompletionRequest normalize(final CompletionRequest completionRequest) {

        if (completionRequest == null) {

            return null;
        }

        final CompletionRequest.Builder normalizedCompletionRequestBuilder = CompletionRequest.of(completionRequest);

        if (completionRequest.getSearchLimit() == null || completionRequest.getSearchLimit() <= 0) {
            normalizedCompletionRequestBuilder.searchLimit(DEFAULT_LIMIT) ;
        }
        if (completionRequest.getSearchLimit() > MAX_LIMIT) {
            normalizedCompletionRequestBuilder.searchLimit(MAX_LIMIT);
        }

        if (completionRequest.getSearchOffset() == null || completionRequest.getSearchOffset() < 0) {
            normalizedCompletionRequestBuilder.searchOffset(0);
        }

        if (completionRequest.getTemperature() == null) {
            normalizedCompletionRequestBuilder.temperature(0.2f);
        }
        if (completionRequest.getTemperature() < 0f) {
            normalizedCompletionRequestBuilder.temperature(0f);
        }
        if (completionRequest.getTemperature() > 2f) {
            normalizedCompletionRequestBuilder.temperature(2f);
        }

        if (completionRequest.getMaxTokens() == null || completionRequest.getMaxTokens() <= 0) {
            normalizedCompletionRequestBuilder.maxTokens(512);
        }
        if (completionRequest.getStream() == null) {
            normalizedCompletionRequestBuilder.stream(Boolean.FALSE);
        }

        // Operator mapping: default to COSINE
        if (completionRequest.getOperator() == null || completionRequest.getOperator().trim().isEmpty()) {
            normalizedCompletionRequestBuilder.operator("cosine");
        }

        if (completionRequest.getThreshold() == null) {
            normalizedCompletionRequestBuilder.threshold(0.75d);
        }
        if (completionRequest.getThreshold() < 0d) {
            normalizedCompletionRequestBuilder.threshold(0d);
        }
        if (completionRequest.getThreshold() > 1d) {
            normalizedCompletionRequestBuilder.threshold(1d);
        }

        return normalizedCompletionRequestBuilder.build();
    }

    /**
     * Normalize null/invalid fields to sensible defaults.
     *
     * @param completionRequest Original request (may contain null/invalid values).
     * @return Same instance modified or a copy with normalized fields.
     */
    public static SummarizeRequest normalize(final SummarizeRequest completionRequest) {

        if (completionRequest == null) {

            return null;
        }

        final SummarizeRequest.Builder normalizedCompletionRequestBuilder = SummarizeRequest.of(completionRequest);

        if (completionRequest.getSearchLimit() == null || completionRequest.getSearchLimit() <= 0) {
            normalizedCompletionRequestBuilder.searchLimit(DEFAULT_LIMIT) ;
        }
        if (completionRequest.getSearchLimit() > MAX_LIMIT) {
            normalizedCompletionRequestBuilder.searchLimit(MAX_LIMIT);
        }

        if (completionRequest.getSearchOffset() == null || completionRequest.getSearchOffset() < 0) {
            normalizedCompletionRequestBuilder.searchOffset(0);
        }

        if (completionRequest.getTemperature() == null) {
            normalizedCompletionRequestBuilder.temperature(0.2f);
        }
        if (completionRequest.getTemperature() < 0f) {
            normalizedCompletionRequestBuilder.temperature(0f);
        }
        if (completionRequest.getTemperature() > 2f) {
            normalizedCompletionRequestBuilder.temperature(2f);
        }

        if (completionRequest.getMaxTokens() == null || completionRequest.getMaxTokens() <= 0) {
            normalizedCompletionRequestBuilder.maxTokens(512);
        }
        if (completionRequest.getStream() == null) {
            normalizedCompletionRequestBuilder.stream(Boolean.FALSE);
        }

        // Operator mapping: default to COSINE
        if (completionRequest.getOperator() == null || completionRequest.getOperator().trim().isEmpty()) {
            normalizedCompletionRequestBuilder.operator("cosine");
        }

        if (completionRequest.getThreshold() == null) {
            normalizedCompletionRequestBuilder.threshold(0.75d);
        }
        if (completionRequest.getThreshold() < 0d) {
            normalizedCompletionRequestBuilder.threshold(0d);
        }
        if (completionRequest.getThreshold() > 1d) {
            normalizedCompletionRequestBuilder.threshold(1d);
        }

        return normalizedCompletionRequestBuilder.build();
    }

    private final static Map<String, SimilarityOperator> STRING_SIMILARITY_OPERATOR_MAP =
            Map.of("cosine", SimilarityOperator.COSINE,
                    "innerproduct", SimilarityOperator.INNER_PRODUCT,
                    "distance", SimilarityOperator.EUCLIDEAN);

    /**
     * Map a user/operator string to an enum.
     *
     * @param operator "cosine" | "innerProduct" | "distance"
     * @return Enum value for internal usage.
     */
    public static SimilarityOperator toEnum(final String operator) {

        return operator == null? SimilarityOperator.COSINE:
                STRING_SIMILARITY_OPERATOR_MAP.getOrDefault(operator.trim().toLowerCase(), SimilarityOperator.COSINE);

    }
}
