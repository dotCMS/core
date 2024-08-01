package com.dotcms.datagen;

import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EmbeddingsDTODataGen extends AbstractDataGen<EmbeddingsDTO> {

    private static final int DIMENSIONS = 1536;

    private final long currentTime = System.currentTimeMillis();

    private String inode;
    private String identifier;
    private long language;
    private String title;
    private String contentType;
    private String host;
    private String operator;
    private String query;
    private String indexName;
    private String extractedText;
    private Integer tokenCount;
    private float[] embeddings;

    public EmbeddingsDTODataGen withInode(final String inode) {
        this.inode = inode;
        return this;
    }

    public EmbeddingsDTODataGen withLanguage(final long language) {
        this.language = language;
        return this;
    }

    public EmbeddingsDTODataGen withIdentifier(final String identifier) {
        this.identifier = identifier;
        return this;
    }

    public EmbeddingsDTODataGen withHost(final String host) {
        this.host = host;
        return this;
    }

    public EmbeddingsDTODataGen withOperator(final String operator) {
        this.operator = operator;
        return this;
    }

    public EmbeddingsDTODataGen withQuery(final String query) {
        this.query = query;
        return this;
    }

    public EmbeddingsDTODataGen withContentType(final String contentType) {
        this.contentType = contentType;
        return this;
    }

    public EmbeddingsDTODataGen withIndexName(final String indexName) {
        this.indexName = indexName;
        return this;
    }

    public EmbeddingsDTODataGen withTitle(final String title) {
        this.title = title;
        return this;
    }

    public EmbeddingsDTODataGen withExtractedText(final String extractedText) {
        this.extractedText = extractedText;
        return this;
    }

    public EmbeddingsDTODataGen withTokenCount(final int tokenCount) {
        this.tokenCount = tokenCount;
        return this;
    }

    public EmbeddingsDTODataGen withEmbeddings(final float[] embeddings) {
        this.embeddings = embeddings;
        return this;
    }

    @Override
    public EmbeddingsDTO next() {
        return new EmbeddingsDTO.Builder()
                .withInode(inode)
                .withIdentifier(identifier)
                .withLanguage(language)
                .withTitle(title)
                .withContentType(contentType)
                .withExtractedText(extractedText)
                .withHost(host)
                .withIndexName(indexName)
                .withOperator(operator)
                .withQuery(query)
                .withTokenCount(tokenCount)
                .withEmbeddings(Arrays.stream(ArrayUtils.toObject(embeddings)).collect(Collectors.toList()))
                .build();
    }

    @Override
    @WrapInTransaction
    public EmbeddingsDTO persist(final EmbeddingsDTO embeddings) {
        APILocator.getDotAIAPI().getEmbeddingsAPI().saveEmbeddings(embeddings);
        return embeddings;
    }

    @Override
    @WrapInTransaction
    public EmbeddingsDTO nextPersisted() {
        return persist(next());
    }

    public EmbeddingsDTODataGen generate(final String inode, final String indexName, final String text) {
        return new EmbeddingsDTODataGen()
                .withInode(Optional.ofNullable(inode).orElse("inode_" + currentTime))
                .withIdentifier("identifier_" + currentTime)
                .withLanguage(1L)
                .withTitle("Some Title: " + currentTime)
                .withContentType("application/json")
                .withExtractedText(text)
                .withHost("host_"  + currentTime)
                .withIndexName(Optional.ofNullable(indexName).orElse("index_" + currentTime))
                .withOperator("<=>")
                .withQuery("Some query for "  + currentTime)
                .withTokenCount(RandomUtils.nextInt(1, 1000))
                .withEmbeddings(generateVector());
    }

    public static List<EmbeddingsDTODataGen> generateEmbeddings(final String text,
                                                                final String inode,
                                                                final String indexName,
                                                                final int limit) {
        final String[] words = text.split(" ");
        if (words.length == 0) {
            return List.of();
        }

        return IntStream.range(0, limit)
                .mapToObj(i -> new EmbeddingsDTODataGen().generate(inode, indexName, text))
                .collect(Collectors.toList());
    }

    public static void persistEmbeddings(final String text,
                                         final String inode,
                                         final String indexName,
                                         final int limit) {
        generateEmbeddings(text, inode, indexName, limit).forEach(EmbeddingsDTODataGen::nextPersisted);
    }

    public static void persistEmbeddings(final String text,
                                         final String inode,
                                         final String indexName) {
        persistEmbeddings(text, inode, indexName, 16);
    }

    private static float[] generateVector() {
        final float[] vector = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] = RandomUtils.nextFloat(0, 1);
        }
        return vector;
    }

}
