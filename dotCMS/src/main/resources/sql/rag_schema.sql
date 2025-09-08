-- sql/rag_schema.sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS dot_ai_content_metadata(
                                                   id BIGSERIAL PRIMARY KEY,
                                                   inode VARCHAR(255) NOT NULL,
                                                   identifier VARCHAR(255) NOT NULL,
                                                   language BIGINT NOT NULL,
                                                   host VARCHAR(255) NOT NULL,
                                                   variant VARCHAR(255) NOT NULL,
                                                   content_type VARCHAR(255) NOT NULL,
                                                   index_name VARCHAR(255) NOT NULL DEFAULT 'default',
                                                   title VARCHAR(512) NOT NULL,
                                                   extracted_text TEXT,
                                                   extracted_text_hash VARCHAR(255),
                                                   token_count INT,
                                                   UNIQUE (identifier, language, host, variant)
);

CREATE TABLE IF NOT EXISTS dot_ai_embeddings(
                                             id BIGSERIAL PRIMARY KEY,
                                             metadata_id BIGINT NOT NULL REFERENCES dot_ai_content_metadata(id) ON DELETE CASCADE,
                                             model_name VARCHAR(255) NOT NULL,
                                             dimensions INT NOT NULL,
                                             embedding VECTOR NOT NULL
);
