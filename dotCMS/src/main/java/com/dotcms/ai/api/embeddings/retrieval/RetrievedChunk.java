package com.dotcms.ai.api.embeddings.retrieval;

import java.util.Objects;

/** Lightweight retrieved chunk representation.
 * @author jsanca
 **/
public final class RetrievedChunk {

    private final String docId;
    private final String title;
    private final String url;
    private final String contentType;
    private final String identifier;
    private final String languageId;
    private final String fieldVar;
    private final int chunkIndex;
    private final String text;
    private final double score;

    private RetrievedChunk(Builder builder) {
        this.docId = Objects.requireNonNull(builder.docId);
        this.title = builder.title;
        this.url = builder.url;
        this.contentType = builder.contentType;
        this.identifier = builder.identifier;
        this.languageId = builder.languageId;
        this.fieldVar = builder.fieldVar;
        this.chunkIndex = builder.chunkIndex;
        this.text = Objects.requireNonNull(builder.text);
        this.score = builder.score;
    }

    // --- Getters ---
    public String getDocId() { return docId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getContentType() { return contentType; }
    public String getIdentifier() { return identifier; }
    public String getLanguageId() { return languageId; }
    public String getFieldVar() { return fieldVar; }
    public int getChunkIndex() { return chunkIndex; }
    public String getText() { return text; }
    public double getScore() { return score; }

    // --- toString() ---
    @Override
    public String toString() {
        return "RetrievedChunk{" +
                "docId='" + docId + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", contentType='" + contentType + '\'' +
                ", identifier='" + identifier + '\'' +
                ", languageId='" + languageId + '\'' +
                ", fieldVar='" + fieldVar + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", text='" + text + '\'' +
                ", score=" + score +
                '}';
    }

    // --- Static Methods ---
    public static Builder builder() { return new Builder(); }

    public static Builder of(RetrievedChunk chunk) {
        return builder()
                .docId(chunk.getDocId())
                .title(chunk.getTitle())
                .url(chunk.getUrl())
                .contentType(chunk.getContentType())
                .identifier(chunk.getIdentifier())
                .languageId(chunk.getLanguageId())
                .fieldVar(chunk.getFieldVar())
                .chunkIndex(chunk.getChunkIndex())
                .text(chunk.getText())
                .score(chunk.getScore());
    }

    // --- Builder Class ---
    public static final class Builder {
        private String docId;
        private String title;
        private String url;
        private String contentType;
        private String identifier;
        private String languageId;
        private String fieldVar;
        private int chunkIndex;
        private String text;
        private double score;

        private Builder() {}

        public Builder docId(String docId) { this.docId = docId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder identifier(String identifier) { this.identifier = identifier; return this; }
        public Builder languageId(String languageId) { this.languageId = languageId; return this; }
        public Builder fieldVar(String fieldVar) { this.fieldVar = fieldVar; return this; }
        public Builder chunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder score(double score) { this.score = score; return this; }

        public RetrievedChunk build() {
            return new RetrievedChunk(this);
        }
    }
}
