package com.dotcms.ai.v2.api.embeddings.factory;


import com.dotcms.ai.v2.api.dto.ContentMetadataDTO;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO for dot_content_metadata table.
 * Provides upsert and basic lookups using DotConnect.
 * @author jsanca
 */
@ApplicationScoped
public class ContentMetadataFactoryImpl implements ContentMetadataFactory {

    private static final String UPSERT_SQL =
            "INSERT INTO dot_ai_content_metadata " +
                    "(inode, identifier, language, host, variant, content_type, index_name, title, extracted_text, extracted_text_hash, token_count) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (identifier, language, host, variant) DO UPDATE SET " +
                    "  inode = EXCLUDED.inode, " +
                    "  content_type = EXCLUDED.content_type, " +
                    "  index_name = EXCLUDED.index_name, " +
                    "  title = EXCLUDED.title, " +
                    "  extracted_text = EXCLUDED.extracted_text, " +
                    "  extracted_text_hash = EXCLUDED.extracted_text_hash, " +
                    "  token_count = EXCLUDED.token_count " +
                    "RETURNING id";

    private static final String SELECT_UNIQUE_SQL =
            "SELECT id, inode, identifier, language, host, variant, content_type, index_name, title, extracted_text, extracted_text_hash, token_count " +
                    "FROM dot_ai_content_metadata WHERE identifier=? AND language=? AND host=? AND variant=?";

    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM dot_ai_content_metadata WHERE id=?";

    /**
     * Upserts a metadata row and returns the id (generated or existing).
     */
    @Override
    public long upsert(final ContentMetadataDTO contentMetadataDTO) throws DotDataException {

        try (Connection connection = DbConnectionFactory.getPGVectorConnection()) {
            return upsert(connection, contentMetadataDTO);
        }catch (SQLException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }

    @Override
    public long upsert(final Connection connection, final ContentMetadataDTO contentMetadataDTO) throws DotDataException {
        Logger.debug(this, ()-> "Doing upsert over: " + contentMetadataDTO);
        try {

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL(UPSERT_SQL)
                    .addParam(contentMetadataDTO.getInode())
                    .addParam(contentMetadataDTO.getIdentifier())
                    .addParam(contentMetadataDTO.getLanguage())
                    .addParam(contentMetadataDTO.getHost())
                    .addParam(contentMetadataDTO.getVariant())
                    .addParam(contentMetadataDTO.getContentType())
                    .addParam(contentMetadataDTO.getIndexName())
                    .addParam(contentMetadataDTO.getTitle())
                    .addParam(contentMetadataDTO.getExtractedText())
                    .addParam(contentMetadataDTO.getExtractedTextHash())
                    .addParam(contentMetadataDTO.getTokenCount())
                    .loadObjectResults(connection);

            final Object idVal = rows.get(0).get("id");
            return ((Number) idVal).longValue();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }

    /**
     * Finds a row by the unique business key (identifier, language, host, variant).
     */
    @Override
    public Optional<ContentMetadataDTO> findUnique(final ContentMetadataInput contentMetadataInput)
            throws DotDataException {

        final String identifier = contentMetadataInput.getIdentifier();
        final long language = contentMetadataInput.getLanguage();
        final String host = contentMetadataInput.getHost();
        final String variant = contentMetadataInput.getVariant();
        final DotConnect dc = new DotConnect().setSQL(SELECT_UNIQUE_SQL).addParam(identifier)
                .addParam(language).addParam(host).addParam(variant);

        try (Connection connection = DbConnectionFactory.getPGVectorConnection()){

            List<Map<String, Object>> rows = dc.loadObjectResults(connection);
            if (rows.isEmpty()) {
                return Optional.empty();
            }

            final Map<String, Object> rowData = rows.get(0);
            final ContentMetadataDTO dto = new ContentMetadataDTO.Builder()
                    .id(((Number) rowData.get("id")).longValue())
                    .inode((String) rowData.get("inode"))
                    .identifier((String) rowData.get("identifier"))
                    .language(((Number) rowData.get("language")).longValue())
                    .host((String) rowData.get("host"))
                    .variant((String) rowData.get("variant"))
                    .contentType((String) rowData.get("content_type"))
                    .indexName((String) rowData.get("index_name"))
                    .title((String) rowData.get("title"))
                    .extractedText((String) rowData.get("extracted_text"))
                    .extractedTextHash((String) rowData.get("extracted_text_hash"))
                    .tokenCount(rowData.get("token_count") == null ? null : ((Number) rowData.get("token_count")).intValue())
                    .build();
            return Optional.of(dto);
        } catch (SQLException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }

    /**
     * Deletes by id.
     */
    @Override
    public int deleteById(final long id) throws DotDataException {

        try (Connection pgVectorConn = DbConnectionFactory.getPGVectorConnection()) {

            Logger.debug(this, ()-> "Deleting the embedding id: " + id);

            return new DotConnect().executeUpdate(pgVectorConn, DELETE_BY_ID_SQL, id);
        } catch (SQLException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }
}

