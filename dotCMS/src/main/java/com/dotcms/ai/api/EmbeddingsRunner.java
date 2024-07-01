package com.dotcms.ai.api;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;

import javax.validation.constraints.NotNull;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

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

            final String cleanContent = String.join(" ", content.trim().split("\\s+"));
            final int splitAtTokens = embeddingsAPI.config.getConfigInteger(AppKeys.EMBEDDINGS_SPLIT_AT_TOKENS);

            // split into sentences
            final BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
            final StringBuilder buffer = new StringBuilder();
            iterator.setText(cleanContent);
            int start = iterator.first();
            int totalTokens = 0;
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                final String sentence = cleanContent.substring(start, end);
                final int tokenCount = EncodingUtil.encoding.get().countTokens(sentence);
                totalTokens += tokenCount;

                if (totalTokens < splitAtTokens) {
                    buffer.append(sentence.trim()).append(" ");
                } else {
                    saveEmbedding(buffer.toString());
                    buffer.setLength(0);
                    buffer.append(sentence.trim()).append(" ");
                    totalTokens = tokenCount;
                }
            }

            if (buffer.toString().split("\\s+").length > 0) {
                saveEmbedding(buffer.toString());
            }
        } catch (Exception e) {
            if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
                Logger.warn(this.getClass(), "unable to embed content:" + contentlet.getIdentifier() + " error:" + e.getMessage(), e);
            } else {
                Logger.warnAndDebug(this.getClass(), "unable to embed content:" + contentlet.getIdentifier() + " error:" + e.getMessage(), e);
            }
        }
    }

    private void saveEmbedding(@NotNull final String initial) {
        if (UtilMethods.isEmpty(initial)) {
            return;
        }

        final String normalizedContent = initial.trim();
        if (embeddingsAPI.embeddingExists(contentlet.getInode(), indexName, normalizedContent)) {
            Logger.info(
                    this.getClass(),
                    "embedding already exists for content:"
                            + contentlet.getTitle()
                            + ", inode:"
                            + contentlet.getInode());
            return;
        }

        final Tuple2<Integer, List<Float>> embeddings = embeddingsAPI.pullOrGenerateEmbeddings(normalizedContent);
        if (embeddings._2.isEmpty()) {
            Logger.info(this.getClass(), "NO TOKENS for " + contentlet.getContentType().variable() + " content:" + normalizedContent);
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
