package com.dotcms.ai.v2.api.embeddings;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ContentRetriever for LangChain4j that delegates to an EmbeddingStore-backed Retriever.
 * @author jsanca
 */
public final class DotContentRetriever implements ContentRetriever {

    private final EmbeddingStoreRetriever adapter; // el adapter que creamos antes

    public DotContentRetriever(final EmbeddingStoreRetriever adapter) {
        this.adapter = adapter;
    }

    @Override
    public List<Content> retrieve(final Query query) {

        final String text = query.text();

        final RetrievalQuery.Builder retrievalQuery = RetrievalQuery.builder();
        retrievalQuery.prompt(text);
        retrievalQuery.limit(8);
        retrievalQuery.offset(0);
        retrievalQuery.threshold(0.0);
        final List<RetrievedChunk> chunks = adapter.search(retrievalQuery.build());

        return chunks.stream()
                .map(retrievedChunk ->
                        Content.from(
                        TextSegment.from(retrievedChunk.getText(), new Metadata(Map.of(
                        "identifier", retrievedChunk.getIdentifier(),
                        "contentType", retrievedChunk.getContentType(),
                        "language", retrievedChunk.getLanguageId(),
                        "title", retrievedChunk.getTitle()
                        ))))
                ).collect(Collectors.toList());
    }

}
