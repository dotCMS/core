package com.dotcms.ai.v2.api.embeddings;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.PgVectorSql;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DAO for dot_embeddings.
 * Provides insert/upsert and cleanup helpers.
 * @author jsanca
 */
@ApplicationScoped
public class EmbeddingsFactoryImpl implements EmbeddingsFactory {

    private static final String INSERT_SQL =
            "INSERT INTO dot_embeddings (metadata_id, model_name, dimensions, embedding) " +
                    "VALUES (?, ?, ?, CAST(? AS vector)) RETURNING id";

    private static final String UPSERT_SQL =
            // If you want to avoid duplicates per (metadata_id, model_name), add a unique index and use ON CONFLICT
            "INSERT INTO dot_embeddings (metadata_id, model_name, dimensions, embedding) " +
                    "VALUES (?, ?, ?, CAST(? AS vector)) " +
                    "RETURNING id";

    private static final String DELETE_BY_METADATA_SQL =
            "DELETE FROM dot_embeddings WHERE metadata_id=?";

    /**
     * Inserts a new embedding row.
     * @return generated id
     */
    @Override
    public long insert(final EmbeddingInput embeddingInput)
            throws DotDataException {

        final long metadataId = embeddingInput.getMetadataId();
        final String modelName = embeddingInput.getModelName();
        final int dimensions = embeddingInput.getDimensions();
        final float[] embedding = embeddingInput.getEmbedding();

        try (Connection connection = DbConnectionFactory.getPGVectorConnection()) {

            final List<Map<String, Object>> rows = new DotConnect()
            .setSQL(INSERT_SQL)
            .addParam(metadataId)
            .addParam(modelName)
            .addParam(dimensions)
            .addParam(PgVectorSql.toVectorLiteral(embedding)) // '[...]'
            .loadObjectResults(connection);

            if (UtilMethods.isSet(rows)) {

                final Object idVal = rows.get(0).get("id");
                return ((Number) idVal).longValue();
            }

            return 0l;
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }

    /**
     * Upsert variant (requires a unique constraint to be effective, otherwise equals to insert).
     */
    @Override
    public long upsert(final EmbeddingInput embeddingInput)
            throws  DotDataException {

        final long metadataId = embeddingInput.getMetadataId();
        final String modelName = embeddingInput.getModelName();
        final int dimensions = embeddingInput.getDimensions();
        final float[] embedding = embeddingInput.getEmbedding();

        try (Connection connection = DbConnectionFactory.getPGVectorConnection()) {

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL(UPSERT_SQL)
                    .addParam(metadataId)
                    .addParam(modelName)
                    .addParam(dimensions)
                    .addParam(PgVectorSql.toVectorLiteral(embedding))
                    .loadObjectResults(connection);

            if (UtilMethods.isSet(rows)) {

                final Object idVal = rows.get(0).get("id");
                return ((Number) idVal).longValue();
            }

            return 0l;
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }

    @Override
    public long upsert(Connection connection, EmbeddingInput embeddingInput) throws DotDataException {
        return 0;
    }

    /**
     * Deletes all embeddings for a metadata id (useful when reindexing).
     */
    @Override
    public int deleteByMetadataId(final long metadataId) throws DotDataException {

        try (Connection pgVectorConn = DbConnectionFactory.getPGVectorConnection()) {

            Logger.debug(this, ()-> "Deleting the content metadata id: " + metadataId);

            return new DotConnect().executeUpdate(pgVectorConn, DELETE_BY_METADATA_SQL, metadataId);
        } catch (SQLException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e);
        }
    }
}
