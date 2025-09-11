package com.dotcms.ai.v2.api.dto;


import java.util.Objects;

/**
 * Immutable DTO mapping the dot_content_metadata row.
 * @author jsanca
 */
public final class ContentMetadataDTO {

    private final Long id;
    private final String inode;
    private final String identifier;
    private final Long language;
    private final String host;
    private final String variant;
    private final String contentType;
    private final String indexName;
    private final String title;
    private final String extractedText;
    private final String extractedTextHash;
    private final Integer tokenCount;

    private ContentMetadataDTO(Builder b) {
        this.id = b.id;
        this.inode = b.inode;
        this.identifier = b.identifier;
        this.language = b.language;
        this.host = b.host;
        this.variant = b.variant;
        this.contentType = b.contentType;
        this.indexName = b.indexName;
        this.title = b.title;
        this.extractedText = b.extractedText;
        this.extractedTextHash = b.extractedTextHash;
        this.tokenCount = b.tokenCount;
    }

    /** Returns a pre-populated builder from an existing DTO (copy-for-modify). */
    public static Builder of(ContentMetadataDTO dto) {
        return new Builder()
                .id(dto.id).inode(dto.inode).identifier(dto.identifier)
                .language(dto.language).host(dto.host).variant(dto.variant)
                .contentType(dto.contentType).indexName(dto.indexName)
                .title(dto.title).extractedText(dto.extractedText)
                .extractedTextHash(dto.extractedTextHash).tokenCount(dto.tokenCount);
    }

    public Long getId() { return id; }
    public String getInode() { return inode; }
    public String getIdentifier() { return identifier; }
    public Long getLanguage() { return language; }
    public String getHost() { return host; }
    public String getVariant() { return variant; }
    public String getContentType() { return contentType; }
    public String getIndexName() { return indexName; }
    public String getTitle() { return title; }
    public String getExtractedText() { return extractedText; }
    public String getExtractedTextHash() { return extractedTextHash; }
    public Integer getTokenCount() { return tokenCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentMetadataDTO)) return false;
        ContentMetadataDTO that = (ContentMetadataDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(inode, that.inode) &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(language, that.language) &&
                Objects.equals(host, that.host) &&
                Objects.equals(variant, that.variant) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(indexName, that.indexName) &&
                Objects.equals(title, that.title) &&
                Objects.equals(extractedTextHash, that.extractedTextHash) &&
                Objects.equals(tokenCount, that.tokenCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, inode, identifier, language, host, variant, contentType, indexName, extractedTextHash, tokenCount);
    }

    @Override
    public String toString() {
        return "ContentMetadataDTO{" +
                "id=" + id +
                ", inode='" + inode + '\'' +
                ", identifier='" + identifier + '\'' +
                ", language=" + language +
                ", host='" + host + '\'' +
                ", variant='" + variant + '\'' +
                ", contentType='" + contentType + '\'' +
                ", indexName='" + indexName + '\'' +
                ", title='" + title + '\'' +
                ", tokenCount=" + tokenCount +
                '}';
    }

    /** Builder for {@link ContentMetadataDTO}. */
    public static final class Builder {
        private Long id;
        private String inode;
        private String identifier;
        private Long language;
        private String host;
        private String variant;
        private String contentType;
        private String indexName;
        private String title;
        private String extractedText;
        private String extractedTextHash;
        private Integer tokenCount;

        public Builder id(Long v){ this.id=v; return this; }
        public Builder inode(String v){ this.inode=v; return this; }
        public Builder identifier(String v){ this.identifier=v; return this; }
        public Builder language(Long v){ this.language=v; return this; }
        public Builder host(String v){ this.host=v; return this; }
        public Builder variant(String v){ this.variant=v; return this; }
        public Builder contentType(String v){ this.contentType=v; return this; }
        public Builder indexName(String v){ this.indexName=v; return this; }
        public Builder title(String v){ this.title=v; return this; }
        public Builder extractedText(String v){ this.extractedText=v; return this; }
        public Builder extractedTextHash(String v){ this.extractedTextHash=v; return this; }
        public Builder tokenCount(Integer v){ this.tokenCount=v; return this; }

        public ContentMetadataDTO build(){ return new ContentMetadataDTO(this); }
    }
}

