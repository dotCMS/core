package com.dotcms.ai.v2.api.embeddings;

public interface RagIngestAPI {
    int indexOne(SingleRagIndexRequest ragIndexRequest) throws Exception;

    /**
     * Index a content type contentlets into the embedding knowledge base
     * @param contentTypeRagIndexRequest v
     * @return int how many rows added
     */
    int indexContentType(ContentTypeRagIndexRequest contentTypeRagIndexRequest);
}
