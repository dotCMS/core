package com.dotcms.ai.api.embeddings.retrieval;

import java.util.List;

/**
 * Retrieval SPI to decouple the orchestrator from your storage/index implementation.
 * Implementations may use vector DBs, pgvector, or custom search backends.
 * @author jsanca
 */
public interface Retriever {

    /**
     * Find relevant chunks for the given query.
     *
     * @param query Retrieval query with filters and scoring operator.
     * @return A (possibly empty) list of ranked chunks.
     */
    List<RetrievedChunk> search(RetrievalQuery query);
}
