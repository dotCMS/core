package com.dotcms.ai.api;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;

import javax.validation.constraints.NotNull;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

import static com.dotcms.ai.app.AppConfig.debugLogger;
import static com.liferay.util.StringPool.SPACE;

/**
 * The EmbeddingsRunner class is responsible for generating embeddings for a specific contentlet.
 * It implements the Runnable interface, allowing it to be used in a multithreaded context.
 * The class takes a Contentlet object, a content string, and an index name as parameters.
 * The embeddings generation is performed in the run() method, which is executed in a separate thread when the class is used in a Thread object.
 */
class EmbeddingsRunner implements Runnable {

    private final Contentlet contentlet;
    private final String content;
    private final String indexName;
    private final EmbeddingsAPIImpl embeddingsAPI;

    public EmbeddingsRunner(final EmbeddingsAPIImpl embeddingsAPI,
                            final Contentlet contentlet,
                            final String content,
                            final String index) {
        this.embeddingsAPI = embeddingsAPI;
        this.contentlet = contentlet;
        this.content = content;
        this.indexName = index;
    }

    @Override
    @WrapInTransaction
    public void run() {
        try {
            if (embeddingsAPI.config.getConfigBoolean(AppKeys.EMBEDDINGS_DB_DELETE_OLD_ON_UPDATE)) {
                final EmbeddingsDTO deleteOldVersions = new EmbeddingsDTO.Builder()
                        .withIdentifier(contentlet.getIdentifier())
                        .withLanguage(contentlet.getLanguageId())
                        .withIndexName(indexName)
                        .withContentType(contentlet.getContentType().variable())
                        .withExcludeInodes(new String[]{contentlet.getInode()})
                        .build();
                embeddingsAPI.deleteEmbedding(deleteOldVersions);
            }

            final String cleanContent = String.join(SPACE, this.content.trim().split("\\s+"));
            final int splitAtTokens = embeddingsAPI.config.getConfigInteger(AppKeys.EMBEDDINGS_SPLIT_AT_TOKENS);

            // split into sentences
            final BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
            final StringBuilder buffer = new StringBuilder();
            iterator.setText(cleanContent);
            int start = iterator.first();
            int totalTokens = 0;
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                final String sentence = cleanContent.substring(start, end);
                final int tokenCount = EncodingUtil.get()
                        .getEncoding()
                        .map(encoding -> encoding.countTokens(sentence))
                        .orElse(0);
                totalTokens += tokenCount;

                if (totalTokens < splitAtTokens) {
                    buffer.append(sentence.trim()).append(SPACE);
                } else {
                    saveEmbedding(buffer.toString());
                    buffer.setLength(0);
                    buffer.append(sentence.trim()).append(SPACE);
                    totalTokens = tokenCount;
                }
            }

            if (buffer.toString().split("\\s+").length > 0) {
                debugLogger(this.getClass(), () -> String.format("Saving embeddings for contentlet ID '%s'", this.contentlet.getIdentifier()));
                this.saveEmbedding(buffer.toString());
                debugLogger(this.getClass(), () -> String.format("Embeddings for contentlet ID '%s' were saved", this.contentlet.getIdentifier()));
            }
        } catch (final Exception e) {
            final String errorMsg = String.format("Failed to generate embeddings for contentlet ID " +
                    "'%s': %s", contentlet.getIdentifier(), ExceptionUtil.getErrorMessage(e));
            if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
                Logger.warn(this.getClass(), errorMsg, e);
            } else {
                Logger.warnAndDebug(this.getClass(), errorMsg, e);
            }
        }
    }

    /**
     * Takes the tokenized content of a given Contentlet and pulls or generates its respective
     * embeddings.
     *
     * @param initial The content to generate embeddings for.
     */
    private void saveEmbedding(@NotNull final String initial) {
        if (UtilMethods.isEmpty(initial)) {
            return;
        }

        final String normalizedContent = initial.trim();
        if (this.embeddingsAPI.embeddingExists(this.contentlet.getInode(), this.indexName, normalizedContent)) {
            Logger.info(this, String.format("Embedding already exists for content " +
                            "'%s' with Inode '%s'", this.contentlet.getTitle(), this.contentlet.getInode()));
            return;
        }

        final Tuple2<Integer, List<Float>> embeddings =
                this.embeddingsAPI.pullOrGenerateEmbeddings(
                        contentlet.getIdentifier(),
                        normalizedContent,
                        UtilMethods.extractUserIdOrNull(APILocator.systemUser()));
        if (embeddings._2.isEmpty()) {
            Logger.info(this.getClass(), String.format("No tokens for Content Type " +
                    "'%s'. Normalized content: %s", this.contentlet.getContentType().variable(), normalizedContent));
            return;
        }

        final EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                .withContentType(contentlet.getContentType().variable())
                .withTokenCount(embeddings._1)
                .withInode(contentlet.getInode())
                .withLanguage(contentlet.getLanguageId())
                .withTitle(contentlet.getTitle())
                .withIdentifier(contentlet.getIdentifier())
                .withHost(contentlet.getHost())
                .withExtractedText(normalizedContent)
                .withIndexName(indexName)
                .withEmbeddings(embeddings._2).build();

        embeddingsAPI.saveEmbeddings(embeddingsDTO);
    }

}
