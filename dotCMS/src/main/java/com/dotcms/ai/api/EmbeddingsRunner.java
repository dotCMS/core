package com.dotcms.ai.api;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.EncodingUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;

import javax.validation.constraints.NotNull;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

class EmbeddingsRunner implements Runnable {

    final Contentlet contentlet;
    final String content;
    final String indexName;
    private final EmbeddingsAPIImpl embeddingsAPI;

    public EmbeddingsRunner(EmbeddingsAPIImpl embeddingsAPI, Contentlet contentlet, String content, String index) {
        this.embeddingsAPI = embeddingsAPI;
        this.contentlet = contentlet;
        this.content = content;
        this.indexName = index;
    }

    @Override
    public void run() {
        try {

            if (embeddingsAPI.config.getConfigBoolean(AppKeys.EMBEDDINGS_DB_DELETE_OLD_ON_UPDATE)) {

                EmbeddingsDTO deleteOldVersions = new EmbeddingsDTO.Builder()
                        .withIdentifier(contentlet.getIdentifier())
                        .withLanguage(contentlet.getLanguageId())
                        .withIndexName(indexName)
                        .withContentType(contentlet.getContentType().variable())
                        .withExcludeInodes(new String[]{contentlet.getInode()})
                        .build();
                EmbeddingsDB.impl.get().deleteEmbeddings(deleteOldVersions);
            }


            final String cleanContent = String.join(" ", content.trim().split("\\s+"));
            final int SPLIT_AT_TOKENS = embeddingsAPI.config.getConfigInteger(AppKeys.EMBEDDINGS_SPLIT_AT_TOKENS);

            // split into sentences
            final BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
            final StringBuilder buffer = new StringBuilder();
            iterator.setText(cleanContent);
            int start = iterator.first();
            int totalTokens = 0;
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                String sentence = cleanContent.substring(start, end);
                int tokenCount = EncodingUtil.encoding.get().countTokens(sentence);
                totalTokens += tokenCount;

                if (totalTokens < SPLIT_AT_TOKENS) {
                    buffer.append(sentence).append(" ");
                } else {
                    saveEmbedding(buffer.toString(), contentlet, indexName);
                    buffer.setLength(0);
                    buffer.append(sentence).append(" ");
                    totalTokens = tokenCount;
                }

            }
            if (buffer.toString().split("\\s+").length > 0) {
                saveEmbedding(buffer.toString(), contentlet, indexName);
            }

        } catch (Exception e) {
            if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
                Logger.warn(this.getClass(), "unable to embed content:" + contentlet.getIdentifier() + " error:" + e.getMessage(), e);
            } else {
                Logger.warnAndDebug(this.getClass(), "unable to embed content:" + contentlet.getIdentifier() + " error:" + e.getMessage(), e);
            }


        }

    }

    private void saveEmbedding(@NotNull String content, @NotNull Contentlet contentlet, String indexName) {
        if (UtilMethods.isEmpty(content)) {
            return;
        }

        if (EmbeddingsDB.impl.get().embeddingExists(contentlet.getInode(), indexName, content)) {
            Logger.info(this.getClass(), "embedding already exists for content:" + contentlet.getTitle() + ", inode:" + contentlet.getInode());
            return;
        }


        Tuple2<Integer, List<Float>> embeddings = embeddingsAPI.pullOrGenerateEmbeddings(content);

        if (embeddings._2.isEmpty()) {
            Logger.info(this.getClass(), "NO TOKENS for " + contentlet.getContentType().variable() + " content:" + content);
            return;
        }


        EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                .withContentType(contentlet.getContentType().variable())
                .withTokenCount(embeddings._1)
                .withInode(contentlet.getInode())
                .withLanguage(contentlet.getLanguageId())
                .withTitle(contentlet.getTitle())
                .withIdentifier(contentlet.getIdentifier())
                .withHost(contentlet.getHost())
                .withExtractedText(content)
                .withIndexName(indexName)
                .withEmbeddings(embeddings._2).build();


        EmbeddingsDB.impl.get().saveEmbeddings(embeddingsDTO);


    }

}
