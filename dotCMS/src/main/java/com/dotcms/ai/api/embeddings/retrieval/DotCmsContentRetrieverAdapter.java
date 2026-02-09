package com.dotcms.ai.api.embeddings.retrieval;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.rag.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter to adapt the {@link EmbeddingStoreRetriever} to the Rag expecting contract
 * @author jsanca
 */
public class DotCmsContentRetrieverAdapter implements ContentRetriever {

    private final Retriever dotcmsRetriever;

    public DotCmsContentRetrieverAdapter(final Retriever retriever) {
        this.dotcmsRetriever = retriever;
    }

    @Override
    public List<Content> retrieve(final Query query) {

        // Convert LangChain4j Query → your RetrievalQuery
        final RetrievalQuery retrievalQuery = RetrievalQuery.builder()
                .prompt(query.text())
                // todo: we have to figured out how to retrieve here the right site
                //.site(query.metadata() != null ? query.metadata().getString("site") : null)
                .limit(8)
                .offset(0)
                .threshold(0.75)
                .build();

        // Run your RAG
        final List<RetrievedChunk> chunks = dotcmsRetriever.search(retrievalQuery);

        // Convert your chunks → LangChain4j Content
        return chunks.stream()
                .map(chunk -> {

                    final Map<String, Object> metadataMap = new HashMap<>();
                    metadataMap.put("docId", chunk.getDocId());
                    metadataMap.put("title", chunk.getTitle());
                    metadataMap.put("url", chunk.getUrl());
                    metadataMap.put("contentType", chunk.getContentType());
                    metadataMap.put("identifier", chunk.getIdentifier());
                    metadataMap.put("languageId", chunk.getLanguageId());
                    metadataMap.put("fieldVar", chunk.getFieldVar());
                    metadataMap.put("chunkIndex", chunk.getChunkIndex());
                    metadataMap.put("score", chunk.getScore());
                    metadataMap.put("inode", chunk.getInode());

                    final TextSegment textSegment = TextSegment.from(
                            chunk.getText(),
                            Metadata.from(metadataMap)
                    );

                    final Map<ContentMetadata, Object> scoreMetadata = new HashMap<>();
                    scoreMetadata.put(ContentMetadata.SCORE, chunk.getScore());

                    return Content.from(
                            textSegment,
                            scoreMetadata);

                }).collect(Collectors.toList());
    }
}
