package com.dotcms.ai.api.embeddings;

import com.dotcms.ai.db.PgVectorDataSource;
import com.dotcms.ai.api.SimilarityOperator;
import com.dotcms.ai.api.embeddings.ContentMetadataDTO;
import com.dotcms.ai.api.embeddings.factory.ContentMetadataFactory;
import com.dotcms.ai.api.embeddings.factory.EmbeddingsFactory;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.PgVectorSql;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.commons.lang3.ArrayUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * EmbeddingStore implementation backed by dotCMS tables:
 * - dot_content_metadata
 * - dot_embeddings (pgvector)
 *
 * It maps TextSegment.text() to dot_content_metadata.extracted_text
 * and TextSegment.metadata() to business columns (identifier, site/host, language, etc.).
 *
 * Example:
 *  EmbeddingModel embeddingModel =  // your provider (OpenAI/Azure/Ollama/ONNX)
 *  DotPgVectorEmbeddingStore store = DotPgVectorEmbeddingStore.builder()
 *         .dataSource(myDataSource)
 *         .indexName("default")
 *         .dimension(embeddingModel.dimension())
 *         .operator(SimilarityOperator.COSINE)  // o INNER_PRODUCT/EUCLIDEAN
 *         .build();
 *
 * // Indexing
 * TextSegment s1 = TextSegment.from(
 *         "I like football.",
 *         Collections.singletonMap("identifier", "id-1")
 * );
 * Embedding e1 = embeddingModel.embed(s1).content();
 * store.add(e1, s1);
 *
 * // Search
 * Embedding query = embeddingModel.embed("What is your favourite sport?").content();
 * EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
 *         .queryEmbedding(query)
 *         .maxResults(1)
 *         .build();
 * EmbeddingSearchResult<TextSegment> result = store.search(req);
 * System.out.println(result.matches().get(0).embedded().text());
 *
 *
 * @author jsanca
 */
public class DotPgVectorEmbeddingStore implements EmbeddingStore<TextSegment> {

    // ==== Config ====
    private final DataSource dataSource;
    private final String indexName;
    private final int dimension;
    private final SimilarityOperator operator;

    // Columns mapping keys expected in TextSegment.metadata()
    private final String KEY_IDENTIFIER = "identifier";
    private final String KEY_LANGUAGE   = "language";   // long
    private final String KEY_HOST       = "host";       // site/host
    private final String KEY_VARIANT    = "variant";
    private final String KEY_CONTENT_TYPE = "contentType";
    private final String KEY_TITLE      = "title";
    private final String KEY_INODE      = "inode";

    // DAOs
    private final ContentMetadataFactory contentMetadataFactory = CDIUtils.getBeanThrows(ContentMetadataFactory.class);
    private final EmbeddingsFactory embeddingsFactory = CDIUtils.getBeanThrows(EmbeddingsFactory.class);

    private DotPgVectorEmbeddingStore(final Builder builder) {

        this.dataSource = builder.dataSource;
        this.indexName = builder.indexName == null ? "default" : builder.indexName;
        this.dimension = builder.dimension;
        this.operator = builder.operator == null ? SimilarityOperator.COSINE : builder.operator;
    }

    /** Builder */
    public static class Builder {
        private DataSource dataSource = PgVectorDataSource.datasource.get(); // we took the datasource from dotcms
        private String indexName;
        private int dimension;
        private SimilarityOperator operator = SimilarityOperator.COSINE;

        public Builder dataSource(DataSource ds) { this.dataSource = ds; return this; }
        public Builder indexName(String v) { this.indexName = v; return this; }
        public Builder dimension(int v) { this.dimension = v; return this; }
        public Builder operator(SimilarityOperator op) { this.operator = op; return this; }

        public DotPgVectorEmbeddingStore build() {
            Objects.requireNonNull(dataSource, "dataSource is required");
            if (dimension <= 0) {
                throw new IllegalArgumentException("dimension must be > 0");
            }

            return new DotPgVectorEmbeddingStore(this);
        }
    }

    public static Builder builder() { return new Builder(); }

    // ==== EmbeddingStore API ====

    @Override
    public String add(final Embedding embedding) {
        throw new UnsupportedOperationException(
                "This store requires a TextSegment with metadata (identifier/host/language). " +
                        "Use add(embedding, segment) or addAll(embeddings, segments).");
    }

    @Override
    public void add(final String id, final Embedding embedding) {
        throw new UnsupportedOperationException(
                "This store requires a TextSegment with metadata (identifier/host/language). " +
                        "Use add(embedding, segment) or addAll(embeddings, segments).");
    }

    @Override
    public String add(final Embedding embedding,
                      final TextSegment segment) {

        Logger.debug(this, ()-> "Adding the embedding for the segment: " + segment);
        final List<String> ids = addAll(Collections.singletonList(embedding), Collections.singletonList(segment));
        return ids.get(0);
    }

    @Override
    public List<String> addAll(final List<Embedding> embeddings) {

        throw new UnsupportedOperationException(
                "This store requires a TextSegment with metadata (identifier/host/language). " +
                        "Use add(embedding, segment) or addAll(embeddings, segments).");
    }

    @Override
    public List<String> addAll(final List<Embedding> embeddings, final List<TextSegment> segments) {

        if (embeddings == null || segments == null || embeddings.size() != segments.size()) {

            throw new IllegalArgumentException("embeddings and segments must be same size");
        }
        if (embeddings.isEmpty()) {
            return Collections.emptyList();
        }

        boolean autoCommit = true;
        int isolation = 0;
        Connection connection = null;
        try  {

            connection = dataSource.getConnection();
            autoCommit = connection.getAutoCommit();
            isolation = connection.getTransactionIsolation();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            final List<String> generatedIds = new ArrayList<>(embeddings.size());
            for (int i = 0; i < embeddings.size(); i++) {

                final Embedding embedding = embeddings.get(i);
                final TextSegment textSegment = segments.get(i);
                // 1) Upsert metadata row
                final ContentMetadataDTO meta = buildMetadataFromSegment(textSegment);
                final long metadataId = contentMetadataFactory.upsert(connection, meta);
                // 2) Insert/Upsert embedding row
                final float[] vec = ArrayUtils.toPrimitive(embedding.vectorAsList().toArray(new Float[]{}));
                if (vec.length != dimension) { // the model dimension and the vector should match
                    throw new IllegalArgumentException("Embedding dimension mismatch: " + vec.length +
                            " != " + dimension + " (model=" + modelNameFromSegment(textSegment) + ")");
                }
                final long embId = embeddingsFactory.upsert(connection, EmbeddingInput.of(metadataId, modelNameFromSegment(textSegment), dimension, vec));
                generatedIds.add(String.valueOf(embId));
            }

            connection.commit();  // batching
            return generatedIds;
        } catch (SQLException | DotDataException ex) {

            try { connection.rollback(); } catch (Exception ignore) {}
            throw new RuntimeException("Failed to add embeddings", ex);
        } finally {
            if (null != connection) {

                try {
                    connection.setAutoCommit(autoCommit);
                    connection.setTransactionIsolation(isolation);
                } catch (Exception ignore) {}
                CloseUtils.closeQuietly(connection);
            }
        }
    }

    // if we want to recover the embedding could to e.embedding::text AS emb_text and parse as a float

    private static final String BASE_SELECT =
            "SELECT e.id AS emb_id, %s AS score, " +
                    "m.id AS metadata_id, m.identifier, m.language, m.host, m.variant, m.content_type, " +
                    "m.index_name, m.title, m.extracted_text " +
                    "FROM dot_ai_embeddings e " +
                    "JOIN dot_ai_content_metadata m ON m.id = e.metadata_id " +
                    "WHERE m.index_name = ? ";

    private static final String ORDER_LIMIT = " ORDER BY score DESC LIMIT ? OFFSET ? ";

    @Override
    public EmbeddingSearchResult<TextSegment> search(final EmbeddingSearchRequest request) {
        // Build SQL against dot_embeddings + dot_content_metadata with pgvector operator
        // Normalize defaults
        final int maxResults = Math.max(request.maxResults() , 5);
        final int offset = 0; //(request.offset() == null || request.offset() < 0) ? 0 : request.offset(); // todo: this is not available

        Connection db = null;
        try {

            Logger.debug(this, ()-> "Searching for embeddings: offset: " + offset + ", maxResults: "
                    + maxResults + ", indexName: " + indexName + ", operator: " + operator);
            db = dataSource.getConnection();
            final String scoreExpr = scoreExpression(operator, "e.embedding", ":query");
            String sql = String.format(BASE_SELECT, scoreExpr) + ORDER_LIMIT;
            final DotConnect dc = new DotConnect();
            // Named param :query not supported by DotConnect; inlining literal vector:
            // Workaround: replace ':query' in SQL with the literal before setSQL
            final String queryVecLiteral = PgVectorSql.toVectorLiteral(toFloatArray(request.queryEmbedding()));
            sql = sql.replace(":query", "CAST('" + queryVecLiteral + "' AS vector)");
            dc.setSQL(sql);
            // Param 1: index name
            dc.addParam(indexName);
            // Param 2: limit
            dc.addParam(maxResults);
            dc.addParam(offset);
            Logger.debug(this,  "Embedding sql: " + sql);

            final List<Map<String, Object>> rows = dc.loadObjectResults(db); // extract the similar info from our db
            final List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

            for (final Map<String, Object> row : rows) {

                final double score = ((Number) row.get("score")).doubleValue();
                final TextSegment seg = TextSegment.from(
                        (String) row.get("extracted_text"),
                        buildMetadataMap(row)
                );
                // id de embedding como string
                final String embeddingId = String.valueOf(row.get("emb_id"));
                final Embedding embedding = null;  // by now it is null but we can recover from the db if needed
                matches.add(new EmbeddingMatch(score, embeddingId, embedding, seg));
            }

            return new EmbeddingSearchResult(matches);
        } catch (SQLException | DotDataException ex) {
            try {
                if (null != db) {
                    db.rollback();
                }
            } catch (Exception ignore) {}
            throw new RuntimeException("Search failed", ex);
        } finally {
            CloseUtils.closeQuietly(db);
        }
    }

    @Override
    public void removeAll() {
        try (Connection db = dataSource.getConnection()) {
            db.setAutoCommit(false);
            new DotConnect().setSQL("TRUNCATE TABLE dot_embeddings CASCADE").loadResult(db);
            new DotConnect().setSQL("TRUNCATE TABLE dot_content_metadata").loadResult(db); // eventually see if index is needed as a where
            db.commit();
        } catch (SQLException | DotDataException ex) {
            throw new RuntimeException("Failed to removeAll", ex);
        }
    }

    // ==== Helpers ====

    private static Integer approxTokenCount(final String text) {
        if (text == null) {
            return null;
        }

        final int chars = text.length();
        return Math.max(1, (int) Math.ceil(chars / 4.0));
    }


    private ContentMetadataDTO buildMetadataFromSegment(final TextSegment textSegment) {

        final Map<String, Object> md = textSegment.metadata().toMap();
        final String identifier = (String) md.getOrDefault(KEY_IDENTIFIER, UUID.randomUUID().toString());
        final String host = (String) md.getOrDefault(KEY_HOST, "default");
        final String variant = (String) md.getOrDefault(KEY_VARIANT, "DEFAULT");
        final String contentType = (String) md.getOrDefault(KEY_CONTENT_TYPE, "Generic");
        final String title = (String) md.getOrDefault(KEY_TITLE, "");
        final String inode = (String) md.getOrDefault(KEY_INODE, "");
        final long language = toLong(md.get(KEY_LANGUAGE), 1L);

        final String text = textSegment.text();
        final String textHash = StringUtils.hashText(text);
        final Integer tokenCount = approxTokenCount(text);

        return new ContentMetadataDTO.Builder()
                .id(null)
                .inode(inode)
                .identifier(identifier)
                .language(language)
                .host(host)
                .variant(variant)
                .contentType(contentType)
                .indexName(indexName)
                .title(title)
                .extractedText(text)
                .extractedTextHash(textHash)
                .tokenCount(tokenCount)
                .build();
    }

    private String modelNameFromSegment(final TextSegment textSegment) {
        final Object modelName = textSegment.metadata().getString("modelName");
        return modelName == null ? "unknown-model" : String.valueOf(modelName);
    }

    private static float[] toFloatArray(final Embedding embedding) {
        // langchain4j Embedding tiene List<Float>; convertir a float[]
        final List<Float> vectorAsList = embedding.vectorAsList();
        float[] vectorAsPrimitive = new float[vectorAsList.size()];
        for (int i = 0; i < vectorAsList.size(); i++) {

            vectorAsPrimitive[i] = vectorAsList.get(i);
        }
        return vectorAsPrimitive;
    }

    private static long toLong(final Object value, final long defaultValue) {

        return ConversionUtils.toLong(value, defaultValue);
    }

    /**
     * Builds the score expression using pgvector operators.
     * We convert distance to similarity when needed so that higher is always better.
     *
     * COSINE:      1 - (embedding <=> query)   // distance ∈ [0..2], but if normalized ≈ [0..2]
     * INNER_PROD:  (embedding <#> query)       // higher is better
     * EUCLIDEAN:   1.0 / (1.0 + (embedding <-> query))
     */
    private static String scoreExpression(final SimilarityOperator similarityOperator,
                                          final String lhsEmb,
                                          final String rhsQuery) {
        switch (similarityOperator) {
            case INNER_PRODUCT:
                return "(" + lhsEmb + " <#> " + rhsQuery + ")";
            case EUCLIDEAN:
                return "(1.0 / (1.0 + (" + lhsEmb + " <-> " + rhsQuery + ")))";
            case COSINE:
            default:
                return "(1.0 - (" + lhsEmb + " <=> " + rhsQuery + "))";
        }
    }

    private Metadata buildMetadataMap(final Map<String, Object> combinedRow) {
        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(KEY_IDENTIFIER, combinedRow.get("identifier"));
        metadata.put(KEY_LANGUAGE, combinedRow.get("language"));
        metadata.put(KEY_HOST, combinedRow.get("host"));
        metadata.put(KEY_VARIANT, combinedRow.get("variant"));
        metadata.put(KEY_CONTENT_TYPE, combinedRow.get("content_type"));
        metadata.put("indexName", combinedRow.get("index_name"));
        metadata.put(KEY_TITLE, combinedRow.get("title"));
        metadata.put(KEY_INODE, combinedRow.getOrDefault(KEY_INODE, KEY_IDENTIFIER)); // todo: remove this workaround
        return new Metadata(metadata);
    }
}
