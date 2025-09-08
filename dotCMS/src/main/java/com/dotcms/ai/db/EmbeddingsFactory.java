package com.dotcms.ai.db;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.pgvector.PGvector;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

import static com.dotcms.ai.db.EmbeddingsDTO.ALL_INDICES;

/**
 * The EmbeddingsDB class provides methods for managing and interacting with embeddings in the database.
 * It uses the PGVector extension for PostgresSQL to store and query embeddings.
 * This class provides methods for initializing the database table and extension, saving embeddings, searching embeddings, and deleting embeddings.
 */
public class EmbeddingsFactory {

    private static final String AND_KEYWORD = " and ";
    private static final String EQUALS_OPERATOR = "=";
    private static final String NOT_EQUALS_OPERATOR = " <> ";
    private static final String LOWER_FN = "LOWER(%s)";
    private static final String INODE_KEY = "inode";
    private static final String IDENTIFIER_KEY = "identifier";
    private static final String INDEX_NAME_KEY = "index_name";
    public static final Lazy<EmbeddingsFactory> impl = Lazy.of(EmbeddingsFactory::new);

    private EmbeddingsFactory() {
        initVector();
    }

    /**
     * Initializes the PGVector extension in the database.
     */
    public void initVector() {
        initVectorExtension(); // todo: I do need this
        initVectorDbTable();
    }

    /**
     * This method renames the existing `dot_embeddings` table in the database to a new name that includes the current timestamp.
     * This is useful when the creation of the `dot_embeddings` table fails for some reason and you want to try again without losing the existing data.
     */  // todo: analyze if this is may neeed
    public void moveVectorDbTable() {
        String newTableName = "dot_embeddings_" + System.currentTimeMillis();
        Logger.info(EmbeddingsFactory.class, "Create Table Failed : trying to rename:" + newTableName);
        runSQL("ALTER TABLE IF EXISTS dot_embeddings RENAME TO " + newTableName);
    }

    /**
     * This method is responsible for creating the `dot_embeddings` table in the database.
     * It first logs the action, then runs the SQL command to create the table, and finally logs the creation of indexes on the table.
     */
    public void internalInitVectorDbTable() {
        Logger.info(EmbeddingsFactory.class, "Adding table dot_embeddings to database");
        runSQL(EmbeddingsSQL.CREATE_EMBEDDINGS_TABLE);
        Logger.info(EmbeddingsFactory.class, "Adding indexes to dot_embedding table");
        for (final String index : EmbeddingsSQL.CREATE_TABLE_INDEXES) {
            runSQL(index); // todo: check if we need the indexes or not
        }
    }

    /**
     * Drops the database table for storing embeddings.
     * This method is used when you want to remove the embeddings table from the database.
     */ // todo: this could be need in some way
    public void dropVectorDbTable() {
        Logger.info(EmbeddingsFactory.class, "Dropping table dot_embeddings from database");
        runSQL(EmbeddingsSQL.DROP_EMBEDDINGS_TABLE);
    }

    void runSQL(final String sql) {
        try (final Connection db = getPGVectorConnection()) {
            new DotConnect().setSQL(sql).loadResult(db);
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private boolean doesExtensionExist() {
        try(final Connection conn = DbConnectionFactory.getDataSource().getConnection()){
            Logger.info(EmbeddingsFactory.class, "Checking if Vector Extension Exists");
            return !new DotConnect().setSQL(EmbeddingsSQL.CHECK_IF_VECTOR_EXISTS).loadObjectResults(conn).isEmpty();
        } catch (Exception  e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Initializes the PGVector extension in the database.
     * This method is called when the class is instantiated.
     */
    private void initVectorExtension() {
        if (!doesExtensionExist()) {
            Logger.info(EmbeddingsFactory.class, "Adding PGVector extension to database");
            runSQL(EmbeddingsSQL.INIT_VECTOR_EXTENSION);
        } else {
            Logger.info(EmbeddingsFactory.class, "PGVector exists, skipping extension installation");
        }
    }

    /**
     * Initializes the database table for storing embeddings.
     * This method is called when the class is instantiated.
     */
    private void initVectorDbTable() {
        try {
            internalInitVectorDbTable();
        } catch (Exception e) {
            Logger.info(EmbeddingsFactory.class, "Create Table Failed : " + e.getMessage());
            moveVectorDbTable();
            internalInitVectorDbTable();
        }
    }

    /**
     * Adds the PGvector type to the SQLConnection
     * so it can be used and queried against
     * @return
     * @throws SQLException
     */
    private Connection getPGVectorConnection() throws SQLException {
        final Connection conn = PgVectorDataSource.datasource.get().getConnection();
        PGvector.addVectorType(conn);
        return conn;
    }

    /**
     * Searches for existing embeddings for the provided text in the database.
     * The method first checks the database.
     * If no embeddings are found, it sends a request to OpenAI to generate new embeddings.
     *
     * @param extractedText the text to find embeddings for
     * @return a tuple containing the index name, token count, and list of embeddings
     */ // todo: we need to see if this is needed when implement the rag approach,look for uses and see if make sense
    public Tuple3<String, Integer, List<Float>> searchExistingEmbeddings(final String extractedText) {
        try (final Connection conn = getPGVectorConnection(); PreparedStatement statement = conn.prepareStatement(EmbeddingsSQL.SELECT_EMBEDDING_BY_TEXT_HASH)) {
            final String hash = StringUtils.hashText(extractedText);
            statement.setString(1, hash);

            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                final int tokenCount = rs.getInt("token_count");
                final String indexName = rs.getString(INDEX_NAME_KEY);
                final float[] vector = rs.getObject("embeddings", PGvector.class).toArray();
                final List<Float> list = Arrays.asList(ArrayUtils.toObject(vector));
                return Tuple.of(indexName,tokenCount, list);
            }
            return Tuple.of("n/a",0, List.of());
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Checks if embeddings exist for the provided inode, index name, and text.
     *
     * @param inode the inode to check
     * @param indexName the index name to check
     * @param extractedText the text to check
     * @return true if embeddings exist, false otherwise
     */ // todo: we have to see if this make sense, and think the strategy to feet the rag
    public boolean embeddingExists(final String inode, final String indexName, final String extractedText) {
        try (final Connection conn = getPGVectorConnection();
             final PreparedStatement statement =
                     conn.prepareStatement(EmbeddingsSQL.SELECT_EMBEDDING_BY_TEXT_HASH_INODE_AND_INDEX)) {
            final String hash = StringUtils.hashText(extractedText);
            statement.setObject(1, hash);
            statement.setObject(2, inode);
            statement.setObject(3, indexName);

            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Saves the provided embeddings to the database.
     *
     * @param embeddings the embeddings to save
     */
    public void saveEmbeddings(final EmbeddingsDTO embeddings) {
        Logger.info(EmbeddingsFactory.class, String.format("Saving embeddings for content with Inode '%s': %s",
                embeddings.inode, embeddings.title));

        final PGvector vector = new PGvector(ArrayUtils.toPrimitive(embeddings.embeddings));
        try (final Connection conn = getPGVectorConnection();
             final PreparedStatement statement = conn.prepareStatement(EmbeddingsSQL.INSERT_EMBEDDINGS)) {

            int i = 0;
            statement.setString(++i, embeddings.inode);
            statement.setString(++i, embeddings.identifier);
            statement.setLong(++i, embeddings.language);
            statement.setString(++i, embeddings.contentType[0]);
            statement.setString(++i, embeddings.title);
            statement.setString(++i, embeddings.extractedText);
            statement.setString(++i, StringUtils.hashText(embeddings.extractedText));
            statement.setString(++i, embeddings.host);
            statement.setString(++i, embeddings.indexName);
            statement.setInt(++i, embeddings.tokenCount);
            statement.setObject(++i, vector);
            statement.execute();
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Searches for embeddings in the database that match the provided DTO.
     *
     * @param dto the DTO to match embeddings against
     * @return a list of DTOs that match the provided DTO
     */
    public List<EmbeddingsDTO> searchEmbeddings(final EmbeddingsDTO dto) {
        final StringBuilder sql = new StringBuilder(
                EmbeddingsSQL.SEARCH_EMBEDDINGS_SELECT_PREFIX
                        .replace(
                                "{operator}",
                                dto.operator));

        final List<Object> params = appendParams(sql, dto);
        sql.append(" ) data ");

        if (dto.threshold != 0) {
            sql.append(" where distance <= ? ");
            params.add(dto.threshold);
        }

        sql.append(" order by distance limit ? offset ? ");
        params.add(dto.limit);
        params.add(dto.offset);
        params.add(0, new PGvector(ArrayUtils.toPrimitive(dto.embeddings)));

        try (final Connection conn = getPGVectorConnection();
             final PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }

            final List<EmbeddingsDTO> results = new ArrayList<>();
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                final float distance = rs.getFloat("distance");
                final EmbeddingsDTO conEmbed = new EmbeddingsDTO.Builder()
                        .withContentType(rs.getString("content_type"))
                        .withIdentifier(rs.getString(IDENTIFIER_KEY))
                        .withInode(rs.getString(INODE_KEY))
                        .withTitle(rs.getString("title"))
                        .withLanguage(rs.getLong("language"))
                        .withIndexName(rs.getString(INDEX_NAME_KEY))
                        .withHost(rs.getString("host"))
                        .withTokenCount(rs.getInt("token_count"))
                        .withThreshold(distance)
                        .withExtractedText(rs.getString("extracted_text"))
                        .build();
                results.add(conEmbed);
            }
            return results;
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    private <T> void appendParam(final StringBuilder sb,
                                 final Predicate<T> evaluation,
                                 final List<Object> params,
                                 final String column,
                                 final String operator,
                                 final String decorator,
                                 final T value) {
        if (!evaluation.test(value)) {
            return;
        }

        sb.append(AND_KEYWORD)
                .append(column)
                .append(operator)
                .append(Optional.ofNullable(decorator).map(ph -> String.format(ph, "?")).orElse("?"));
        params.add(value);
    }

    private <T> void appendParams(final StringBuilder sb,
                                  final List<Object> params,
                                  final String column,
                                  final String operator,
                                  final String decorator,
                                  final List<T> values) {
        Optional
                .ofNullable(values)
                .ifPresent(v ->
                        v.forEach(value -> appendParam(sb, e -> true, params, column, operator, decorator, value)));
    }

    private void appendParams(final StringBuilder sb,
                              final List<Object> params,
                              final String column,
                              final String operator,
                              final String decorator,
                              final Object[] values) {
        appendParams(sb, params, column, operator, decorator, Arrays.asList(values));
    }

    /**
     * Appends parameters to the provided StringBuilder based on the provided DTO.
     * This method is used to build SQL queries dynamically based on the DTO.
     *
     * @param sql the StringBuilder to append parameters to
     * @param dto the DTO to get parameters from
     * @return a list of parameters
     */
    private List<Object> appendParams(final StringBuilder sql, final EmbeddingsDTO dto) {
        final List<Object> params = new ArrayList<>();

        appendParam(sql, UtilMethods::isSet, params, INODE_KEY, EQUALS_OPERATOR, null, dto.inode);
        appendParam(sql, UtilMethods::isSet, params, IDENTIFIER_KEY, EQUALS_OPERATOR, null, dto.identifier);
        appendParams(sql, params, IDENTIFIER_KEY, NOT_EQUALS_OPERATOR, null, dto.excludeIdentifiers);
        appendParams(sql, params, INODE_KEY, NOT_EQUALS_OPERATOR, null, dto.excludeInodes);
        appendParam(sql, lang -> lang > 0, params, "language", EQUALS_OPERATOR, null, dto.language);

        if (UtilMethods.isSet(dto.contentType) && dto.contentType.length > 0) {
            sql.append(" and ( false ");
            Arrays.stream(dto.contentType).forEach(ct -> {
                sql.append(" OR lower(content_type)=lower(?)");
                params.add(ct);
            });
            sql.append(") ") ;
        }

        appendParam(sql, UtilMethods::isSet, params, "host", EQUALS_OPERATOR, null, dto.host);
        appendParam(
                sql,
                indexName -> UtilMethods.isSet(indexName) && !ALL_INDICES.equals(indexName),
                params,
                "lower(index_name)",
                EQUALS_OPERATOR,
                LOWER_FN,
                dto.indexName);

        return params;
    }

    /**
     * Deletes embeddings from the database that match the provided DTO.
     *
     * @param dto the DTO to match embeddings against
     * @return the number of embeddings deleted
     */
    public int deleteEmbeddings(final EmbeddingsDTO dto) {
        final StringBuilder sql = new StringBuilder("delete from dot_embeddings where true ");
        final List<Object> params = appendParams(sql, dto);
        Logger.info(EmbeddingsFactory.class, "deleting embeddings:" + dto);

        try (final Connection conn = getPGVectorConnection();
             final PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Counts the number of embeddings in the database that match the provided DTO.
     *
     * @param dto the DTO to match embeddings against
     * @return the number of embeddings that match the provided DTO
     */
    public long countEmbeddings(final EmbeddingsDTO dto) {
        final StringBuilder sql = new StringBuilder(
                EmbeddingsSQL.COUNT_EMBEDDINGS_PREFIX
                        .replace(
                                "{operator}",
                                dto.operator));
        final List<Object> params = appendParams(sql, dto);
        sql.append(" ) data ");

        if (dto.threshold != 0) {
            sql.append(" where distance <= ? ");
            params.add(dto.threshold);
        }

        params.add(0, new PGvector(ArrayUtils.toPrimitive(dto.embeddings)));
        try (final Connection conn = getPGVectorConnection();
             final PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }

            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getLong("test");
            }
            return 0;
        } catch (SQLException e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Counts the number of embeddings in the database by index.
     *
     * @return a map of index names to counts
     */
    public Map<String, Map<String, Object>> countEmbeddingsByIndex() {
        final String sql = EmbeddingsSQL.COUNT_EMBEDDINGS_BY_INDEX;

        try (final Connection conn = getPGVectorConnection();
             final PreparedStatement statement = conn.prepareStatement(sql)) {

            final Map<String, Map<String, Object>> results = new TreeMap<>();
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                results.put(
                        rs.getString(INDEX_NAME_KEY),
                        Map.of(
                            "fragments", rs.getLong("embeddings"),
                            "contents", rs.getLong("contents"),
                            "tokenTotal", rs.getLong("token_total"),
                            "tokensPerChunk", rs.getLong("token_per_chunk"),
                            "contentTypes", rs.getString("content_types")
                        ));
            }
            return results;
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

}
