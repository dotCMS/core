package com.dotcms.ai.v2.api.embeddings;

import com.dotcms.ai.v2.api.embeddings.extractor.ContentExtractor;
import com.dotcms.ai.v2.api.embeddings.extractor.ExtractedContent;
import com.dotcms.ai.v2.api.provider.ModelProviderFactory;
import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import com.dotcms.cdi.CDIUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ingest service for dotAI RAG:
 * - Extracts content via {@link ContentExtractor}
 * - Chunks text via {@link TextChunker}
 * - Embeds with {@link EmbeddingModel}
 * - Persists with {@link DotPgVectorEmbeddingStore} using batch addAll
 */
@ApplicationScoped
public class RagIngestAPIImpl implements RagIngestAPI {

    private final ContentExtractor contentExtractor;
    private final TextChunker textChunker;
    private final ModelProviderFactory modelProviderFactory;

    @Inject
    public RagIngestAPIImpl(final ContentExtractor contentExtractor,
                            final ModelProviderFactory modelProviderFactory) {

       this(contentExtractor, new FixedSizeChunker(1200, 200), // todo see this should be configurable)
                        modelProviderFactory);
    }

    public RagIngestAPIImpl(final ContentExtractor contentExtractor,
                            final TextChunker textChunker,
                            final ModelProviderFactory modelProviderFactory) {

        this.contentExtractor = contentExtractor;
        this.textChunker = textChunker;
        this.modelProviderFactory = modelProviderFactory;
    }

    /** PoC: Use local ONNX by default if no model injection. */
    public static EmbeddingModel defaultOnnxModel() {

        return CDIUtils.getBeanThrows(EmbeddingModel.class, "onnx");
    }

    /**
     * Indexes a single contentlet (identifier + language).
     * Uses single-row persistence (not batched).
     *
     * @return number of chunks indexed
     */
    @Override
    public int indexOne(final SingleRagIndexRequest ragIndexRequest) throws Exception {

        final String identifier = ragIndexRequest.getIdentifier();
        final long languageId   = ragIndexRequest.getLanguageId();
        final String embeddingProviderKey = ragIndexRequest.getEmbeddingProviderKey();
        final String indexName = Objects.nonNull(ragIndexRequest.getIndexName())?ragIndexRequest.getIndexName():"default";
        final ModelConfig modelConfig = ragIndexRequest.getModelConfig();
        final EmbeddingModel embeddingModel = Objects.nonNull(embeddingProviderKey)?
                this.modelProviderFactory.getEmbedding(embeddingProviderKey, modelConfig):defaultOnnxModel();
        // todo: this could be eventually cached by factory by index and model
        final DotPgVectorEmbeddingStore embeddingStore = DotPgVectorEmbeddingStore.builder()
                         .indexName(indexName)
                         .dimension(embeddingModel.dimension())
                         .build();
        final ExtractedContent extractedContent = this.contentExtractor.extract(identifier, languageId);
        if (extractedContent == null) {
            return 0;
        }

        final String fullText = normalize(extractedContent);
        if (fullText == null || fullText.isEmpty()) {
            return 0;
        }

        final String textHash = sha256(fullText); // optional idempotency helper
        final List<String> chunks = textChunker.chunk(fullText);
        if (chunks.isEmpty()) {
            return 0;
        }

        int chunksIndexed = 0;
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            final String textChunk = chunks.get(chunkIndex);
            final TextSegment segment = buildSegment(extractedContent, textChunk, textHash, chunkIndex);
            embeddingStore.add(embeddingModel.embed(segment).content(), segment);
            chunksIndexed++;
        }
        return chunksIndexed;
    }

    /**
     * Indexes all content of a content type (optionally filtered by host and language),
     * using the ContentExtractor iterator (hides pagination) and batching persistence via addAll.
     *
     * @param  contentTypeRagIndexRequest ContentTypeRagIndexRequest
     * @param host        optional site/host filter
     * @param contentType required content type
     * @param languageId  optional language filter
     * @param pageSize    batch size for extractor paging (e.g., 50 or 100)
     * @param batchSize   how many text segments to persist per DB batch (e.g., 64, 128, 256)
     * @return total chunks indexed
     */
    @Override
    public int indexContentType(final ContentTypeRagIndexRequest contentTypeRagIndexRequest) {

        final String host = contentTypeRagIndexRequest.getHost();
        final String contentType = contentTypeRagIndexRequest.getContentType();
        final Long languageId = contentTypeRagIndexRequest.getLanguageId().orElse(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        final int pageSize = contentTypeRagIndexRequest.getPageSize();
        final int batchSize = contentTypeRagIndexRequest.getBatchSize();
        final String embeddingProviderKey = contentTypeRagIndexRequest.getEmbeddingProviderKey();
        final String indexName = Objects.nonNull(contentTypeRagIndexRequest.getIndexName())?contentTypeRagIndexRequest.getIndexName():"default";
        final ModelConfig modelConfig = contentTypeRagIndexRequest.getModelConfig();
        final EmbeddingModel embeddingModel = Objects.nonNull(embeddingProviderKey)?
                this.modelProviderFactory.getEmbedding(embeddingProviderKey, modelConfig):defaultOnnxModel();
        // todo: this could be eventually cached by factory by index and model
        final DotPgVectorEmbeddingStore embeddingStore = DotPgVectorEmbeddingStore.builder()
                .indexName(indexName)
                .dimension(embeddingModel.dimension())
                .build();
        final int safePageSize = Math.max(1, pageSize);
        final int safeBatchSize = Math.max(1, batchSize);
        final Iterator<ExtractedContent> iterator =
                contentExtractor.iterator(host, contentType, languageId, safePageSize);

        final List<TextSegment> segmentBuffer = new ArrayList<>(safeBatchSize);
        int totalChunksIndexed = 0;

        while (iterator.hasNext()) {
            final ExtractedContent extractedContent = iterator.next();

            try {
                final String fullText = normalize(extractedContent);
                if (fullText == null || fullText.isEmpty()) {
                    continue;
                }

                final String textHash = sha256(fullText);
                final List<String> chunks = textChunker.chunk(fullText);
                if (chunks.isEmpty()) {
                    continue;
                }

                for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                    final String textChunk = chunks.get(chunkIndex);
                    final TextSegment segment = buildSegment(extractedContent, textChunk, textHash, chunkIndex);
                    segmentBuffer.add(segment);

                    if (segmentBuffer.size() >= safeBatchSize) {
                        totalChunksIndexed += flushBatch(segmentBuffer,
                                embeddingStore, embeddingModel);
                    }
                }

            } catch (Exception exception) {
                // Policy: skip on error, continue with next content
                Logger.error(this,  exception.getMessage(), exception);
            }
        }

        // Flush any remaining segments
        if (!segmentBuffer.isEmpty()) {
            totalChunksIndexed += flushBatch(segmentBuffer,
                    embeddingStore, embeddingModel);
        }

        return totalChunksIndexed;
    }

    // ---------- helpers ----------

    private int flushBatch(final List<TextSegment> segmentBuffer, final DotPgVectorEmbeddingStore embeddingStore,
                           final EmbeddingModel embeddingModel) {
        try {
            // Prefer batched embedding if model supports it; fallback to per-segment
            final List<Embedding> embeddings = embedAllSafely(segmentBuffer, embeddingModel);
            embeddingStore.addAll(embeddings, segmentBuffer);
            final int batchSize = segmentBuffer.size();
            segmentBuffer.clear();
            return batchSize;
        } catch (Exception exception) {
            // If batch fails, attempt to index individually to salvage progress
            int salvaged = 0;
            for (TextSegment segment : new ArrayList<>(segmentBuffer)) {
                try {
                    embeddingStore.add(embeddingModel.embed(segment).content(), segment);
                    salvaged++;
                } catch (Exception ignored) {
                    // skip individual failure
                }
            }
            segmentBuffer.clear();
            return salvaged;
        }
    }

    private List<Embedding> embedAllSafely(final List<TextSegment> segments,
                                           final EmbeddingModel embeddingModel) {
        try {
            // Many EmbeddingModel implementations provide embedAll()
            return embeddingModel.embedAll(segments).content();
        } catch (Throwable unsupported) {
            // Fallback: per-segment embedding
            final List<Embedding> embeddings = new ArrayList<>(segments.size());
            for (TextSegment segment : segments) {
                embeddings.add(embeddingModel.embed(segment).content());
            }
            return embeddings;
        }
    }

    /** Combine title & body to slightly boost recall for short titles. */
    private static String normalize(final ExtractedContent extractedContent) {
        final String title = safe(extractedContent.getTitle());
        final String body  = safe(extractedContent.getText());

        if (title.isEmpty()) {
            return body;
        }
        if (body.isEmpty()) {
            return title;
        }
        return title + "\n\n" + body;
    }

    private static String sha256(final String text) { // todo: we already have something on dotCMS todo this
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            final byte[] digest = messageDigest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(final String value) {
        return (value == null) ? "" : value.trim();
    }

    private static TextSegment buildSegment(final ExtractedContent extractedContent,
                                            final String textChunk,
                                            final String textHash,
                                            final int chunkIndex) {
        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("identifier",  extractedContent.getIdentifier());
        metadata.put("host",        extractedContent.getHost());
        metadata.put("variant",     extractedContent.getVariant());
        metadata.put("contentType", extractedContent.getContentType());
        metadata.put("language",    extractedContent.getLanguage());
        metadata.put("title",       extractedContent.getTitle());
        metadata.put("chunkIndex",  chunkIndex);
        metadata.put("textHash",    textHash);
        metadata.put("modelName",   "all-minilm-l6-v2");

        return TextSegment.from(textChunk, new dev.langchain4j.data.document.Metadata(metadata));
    }
}
