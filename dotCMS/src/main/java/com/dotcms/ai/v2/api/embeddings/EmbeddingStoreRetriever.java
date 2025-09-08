package com.dotcms.ai.v2.api.embeddings;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Retriever adapter that delegates search to an EmbeddingStore<TextSegment>.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Embed the query prompt using the provided EmbeddingModel</li>
 *   <li>Query the underlying EmbeddingStore with an overfetch factor</li>
 *   <li>Apply metadata filters (site/host, contentType[], language, identifier)</li>
 *   <li>Apply a similarity threshold on the score (assuming the store returns a higher-is-better score)</li>
 *   <li>Apply paging (limit/offset)</li>
 * </ul>
 *
 * Notes:
 * <ul>
 *   <li>Index selection & operator are typically configured in the EmbeddingStore itself (e.g., DotPgVectorEmbeddingStore.builder().indexName(...).operator(...)).</li>
 *   <li>Because EmbeddingSearchRequest does not carry arbitrary filters, this adapter filters matches in-memory.</li>
 *   <li>Use a reasonable overfetch to compensate filtering without missing top-K.</li>
 * </ul>
 */
public final class EmbeddingStoreRetriever implements Retriever {

    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;
    private final int defaultLimit;
    private final int maxLimit;
    private final int overfetchFactor; // e.g., 3 → fetch 3×limit from store, then filter
    private final Double defaultThreshold; // nullable

    private EmbeddingStoreRetriever(final Builder builder) {
        this.store = Objects.requireNonNull(builder.store, "store is required");
        this.embeddingModel = Objects.requireNonNull(builder.embeddingModel, "embeddingModel is required");
        this.defaultLimit = builder.defaultLimit <= 0 ? 8 : builder.defaultLimit;
        this.maxLimit = builder.maxLimit <= 0 ? 64 : builder.maxLimit;
        this.overfetchFactor = builder.overfetchFactor <= 0 ? 3 : builder.overfetchFactor;
        this.defaultThreshold = builder.defaultThreshold;
    }

    /** Builder */
    public static final class Builder {
        private EmbeddingStore<TextSegment> store;
        private EmbeddingModel embeddingModel;
        private int defaultLimit = 8;
        private int maxLimit = 64;
        private int overfetchFactor = 3;
        private Double defaultThreshold;

        public Builder store(EmbeddingStore<TextSegment> s) { this.store = s; return this; }
        public Builder embeddingModel(EmbeddingModel m) { this.embeddingModel = m; return this; }
        public Builder defaultLimit(int v) { this.defaultLimit = v; return this; }
        public Builder maxLimit(int v) { this.maxLimit = v; return this; }
        public Builder overfetchFactor(int v) { this.overfetchFactor = v; return this; }
        public Builder defaultThreshold(Double t) { this.defaultThreshold = t; return this; }
        public EmbeddingStoreRetriever build() { return new EmbeddingStoreRetriever(this); }
    }
    public static Builder builder() { return new Builder(); }

    @Override
    public List<RetrievedChunk> search(final RetrievalQuery retrievalQuery) {

        Objects.requireNonNull(retrievalQuery, "RetrievalQuery is required");

        final int limit = clamp(retrievalQuery.getLimit() > 0 ? retrievalQuery.getLimit() : defaultLimit, 1, maxLimit);
        final int offset = Math.max(0, retrievalQuery.getOffset());
        final Double threshold = (retrievalQuery.getThreshold() > 0 ? retrievalQuery.getThreshold() : defaultThreshold);

        // 1) Embed the prompt
        final Embedding queryEmbedding = embeddingModel.embed(retrievalQuery.getPrompt()).content();

        // 2) Ask the store with overfetch to survive filtering
        final int storeFetch = Math.min(limit * overfetchFactor, maxLimit * overfetchFactor);
        final EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(storeFetch)
                .build();

        final EmbeddingSearchResult<TextSegment> embeddingSearchResult = store.search(req);
        final List<EmbeddingMatch<TextSegment>> matches = embeddingSearchResult.matches();
        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }

        // 3) Build predicate for metadata filters
        final Predicate<Map<String, Object>> metaFilter = buildMetaFilter(retrievalQuery);

        // 4) Filter & threshold
        final List<EmbeddingMatch<TextSegment>> filtered = matches.stream()
                .filter(m -> threshold == null || m.score() >= threshold)
                .filter(m -> {
                    final Map<String, Object> metadataMap = m.embedded().metadata().toMap();
                    return metaFilter.test(metadataMap);
                })
                .collect(Collectors.toList());

        // 5) Apply paging (offset/limit) over filtered results
        final int from = Math.min(offset, filtered.size());
        final int to = Math.min(from + limit, filtered.size());
        final List<EmbeddingMatch<TextSegment>> filteredPage = filtered.subList(from, to);

        // 6) Map to RetrievedChunk
        final List<RetrievedChunk> out = new ArrayList<>(filteredPage.size());
        for (final EmbeddingMatch<TextSegment> embeddingMatch : filteredPage) {

            final TextSegment seg = embeddingMatch.embedded();
            final Map<String, Object> metadataMap = seg.metadata().toMap();

            final RetrievedChunk.Builder retrievedChunkBuilder =  RetrievedChunk.builder();
            retrievedChunkBuilder.docId(String.valueOf(metadataMap.getOrDefault("metadata_id", ""))); // not always present
            retrievedChunkBuilder.title(str(metadataMap.get("title")));
            retrievedChunkBuilder.url(null); // todo: check this later
            final String contentTypeVarName = str(metadataMap.get("contentType"));
            retrievedChunkBuilder.contentType(contentTypeVarName);
            if (contentTypeVarName == null) {
                retrievedChunkBuilder.contentType(str(metadataMap.get("content_type"))) ;
            }
            retrievedChunkBuilder.identifier(str(metadataMap.get("identifier")));
            retrievedChunkBuilder.languageId(str(metadataMap.get("language")));
            retrievedChunkBuilder.fieldVar(null); // not in the current scheme
            retrievedChunkBuilder.chunkIndex(0);
            retrievedChunkBuilder.text(seg.text());
            retrievedChunkBuilder.score(embeddingMatch.score());

            out.add(retrievedChunkBuilder.build());
        }
        return out;
    }

    // ----------------- helpers -----------------

    private static String str(final Object o) { return o == null ? null : String.valueOf(o); }

    private static int clamp(final int value, final int lo, final int hi) { return Math.max(lo, Math.min(hi, value)); }

    /**
     * Builds a predicate that checks requested filters against segment metadata.
     * Expected metadata keys in TextSegment.metadata():
     * host/site -> "host", contentType -> "contentType" or "content_type", language -> "language", identifier -> "identifier".
     */
    private static Predicate<Map<String, Object>> buildMetaFilter(RetrievalQuery retrievalQuery) {

        final List<Predicate<Map<String, Object>>> predicateList = new ArrayList<>();

        if (retrievalQuery.getSite() != null && !retrievalQuery.getSite().trim().isEmpty()) {
            predicateList.add(md -> retrievalQuery.getSite().equals(String.valueOf(md.getOrDefault("host", ""))));
        }
        if (retrievalQuery.getContentTypes() != null && retrievalQuery.getContentTypes().size() > 0) {

            final Set<String> allowed = new HashSet<>();
            for (final String contentTypeVarName : retrievalQuery.getContentTypes()) {
                if (contentTypeVarName != null) {
                    allowed.add(contentTypeVarName);
                }
            }
            predicateList.add(md -> {
                Object v = md.get("contentType");
                if (v == null) {
                    v = md.get("content_type");
                }
                return v != null && allowed.contains(String.valueOf(v));
            });
        }
        if (retrievalQuery.getLanguageId() != null && !retrievalQuery.getLanguageId().trim().isEmpty()) {
            predicateList.add(md -> retrievalQuery.getLanguageId().equals(String.valueOf(md.get("language"))));
        }
        /*if (retrievalQuery.identifier != null && !retrievalQuery.identifier.trim().isEmpty()) {
            predicateList.add(md -> retrievalQuery.identifier.equals(String.valueOf(md.get("identifier"))));
        }*/

        if (predicateList.isEmpty()) {
            return md -> true;
        }

        return md -> {
            for (Predicate<Map<String, Object>> predicate : predicateList) {
                if (!predicate.test(md)) {
                    return false;
                }
            }
            return true;
        };
    }
}

