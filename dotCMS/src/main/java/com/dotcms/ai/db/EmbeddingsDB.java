package com.dotcms.ai.db;


import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.hash.Hashing;
import com.pgvector.PGvector;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import org.apache.commons.lang3.ArrayUtils;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.dotcms.ai.db.EmbeddingsDTO.ALL_INDICES;

public class EmbeddingsDB {


    public static final Lazy<EmbeddingsDB> impl = Lazy.of(EmbeddingsDB::new);


    private EmbeddingsDB() {
        initVectorExtension();
        initVectorDbTable();

    }

    public void initVectorExtension() {

        if(!doesExtensionExist()) {
            Logger.info(EmbeddingsDB.class, "Adding PGVector extension to database");
            runSQL(EmbeddingsSQL.INIT_VECTOR_EXTENSION);
        }else{
            Logger.info(EmbeddingsDB.class, "PGVector exists, skipping extension installation");
        }
    }

    public void initVectorDbTable() {

        try {
            internalInitVectorDbTable();
        } catch (Exception e) {
            Logger.info(EmbeddingsDB.class, "Create Table Failed : " + e.getMessage());
            moveVectorDbTable();
            internalInitVectorDbTable();
        }
    }

    public void moveVectorDbTable() {
        String newTableName = "dot_embeddings_" + System.currentTimeMillis();
        Logger.info(EmbeddingsDB.class, "Create Table Failed : trying to rename:" + newTableName);
        runSQL("ALTER TABLE IF EXISTS dot_embeddings RENAME TO " + newTableName);
    }

    public void internalInitVectorDbTable() {

        Logger.info(EmbeddingsDB.class, "Adding table dot_embeddings to database");
        runSQL(EmbeddingsSQL.CREATE_EMBEDDINGS_TABLE);
        Logger.info(EmbeddingsDB.class, "Adding indexes to dot_embedding table");
        for (String index : EmbeddingsSQL.CREATE_TABLE_INDEXES) {
            runSQL(index);
        }

    }


    public void dropVectorDbTable() {
        Logger.info(EmbeddingsDB.class, "Dropping table dot_embeddings from database");
        runSQL(EmbeddingsSQL.DROP_EMBEDDINGS_TABLE);

    }

    void runSQL(String sql) {
        try (Connection db = getPGVectorConnection()) {
            new DotConnect().setSQL(sql).loadResult(db);
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private boolean doesExtensionExist(){
        try(Connection conn = DbConnectionFactory.getDataSource().getConnection()){
            Logger.info(EmbeddingsDB.class, "Checking if Vector Extension Exists");
            return !new DotConnect().setSQL(EmbeddingsSQL.CHECK_IF_VECTOR_EXISTS).loadObjectResults(conn).isEmpty();

        } catch (Exception  e) {
            throw new DotRuntimeException(e);
        }

    }

    /**
     * Adds the PGvector type to the SQLConnection
     * so it can be used and queried against
     * @return
     * @throws SQLException
     */
    private Connection getPGVectorConnection() throws SQLException {
        Connection conn = PgVectorDataSource.datasource.get().getConnection();
        PGvector.addVectorType(conn);
        return conn;
    }

    public Tuple3<String,Integer, List<Float>> searchExistingEmbeddings(String extractedText) {
        try (Connection conn = getPGVectorConnection(); PreparedStatement statement = conn.prepareStatement(EmbeddingsSQL.SELECT_EMBEDDING_BY_TEXT_HASH)) {
            String hash = hashText(extractedText);
            statement.setObject(1, hash);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int tokenCount = rs.getInt("token_count");
                String indexName = rs.getString("index_name");
                float[] vector = rs.getObject("embeddings", PGvector.class).toArray();
                List<Float> list = Arrays.asList(ArrayUtils.toObject(vector));
                return Tuple.of(indexName,tokenCount, list);
            }
            return Tuple.of("n/a",0, List.of());


        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }
    public boolean embeddingExists(String inode, String indexName, String extractedText) {
        try (Connection conn = getPGVectorConnection(); PreparedStatement statement = conn.prepareStatement(EmbeddingsSQL.SELECT_EMBEDDING_BY_TEXT_HASH_INODE_AND_INDEX)) {
            String hash = hashText(extractedText);
            statement.setObject(1, hash);
            statement.setObject(2, inode);
            statement.setObject(3, indexName);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                return true;
            }
            return false;


        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }




    public String hashText(@NotNull String text) {

        return Hashing.sha256()
                .hashString(text, StandardCharsets.UTF_8)
                .toString();
    }

    public void saveEmbeddings(EmbeddingsDTO embeddings) {
        Logger.info(EmbeddingsDB.class, "Saving embeddings for content:" + embeddings.title);

        PGvector vector = new PGvector(ArrayUtils.toPrimitive(embeddings.embeddings));
        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(EmbeddingsSQL.INSERT_EMBEDDINGS)) {

            int i = 0;
            statement.setObject(++i, embeddings.inode);
            statement.setObject(++i, embeddings.identifier);
            statement.setObject(++i, embeddings.language);
            statement.setObject(++i, embeddings.contentType[0]);
            statement.setObject(++i, embeddings.title);
            statement.setObject(++i, embeddings.extractedText);
            statement.setObject(++i, hashText(embeddings.extractedText));
            statement.setObject(++i, embeddings.host);
            statement.setObject(++i, embeddings.indexName);
            statement.setObject(++i, embeddings.tokenCount);
            statement.setObject(++i, vector);
            statement.execute();

        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    public List<EmbeddingsDTO> searchEmbeddings(EmbeddingsDTO dto) {


        StringBuilder sql = new StringBuilder(EmbeddingsSQL.SEARCH_EMBEDDINGS_SELECT_PREFIX.replace("{operator}", dto.operator));

        List<Object> params = appendParams(sql, dto);
        sql.append(" ) data ");

        if (dto.threshold != 0) {
            sql.append(" where distance <= ? ");
            params.add(dto.threshold);
        }

        sql.append(" order by distance limit ? offset ? ");
        params.add(dto.limit);
        params.add(dto.offset);

        params.add(0, new PGvector(ArrayUtils.toPrimitive(dto.embeddings)));


        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }


            List<EmbeddingsDTO> results = new ArrayList<>();
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                float distance = rs.getFloat("distance");
                EmbeddingsDTO conEmbed = new EmbeddingsDTO.Builder()
                        .withContentType(rs.getString("content_type"))
                        .withIdentifier(rs.getString("identifier"))
                        .withInode(rs.getString("inode"))
                        .withTitle(rs.getString("title"))
                        .withLanguage(rs.getLong("language"))
                        .withIndexName(rs.getString("index_name"))
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

    List<Object> appendParams(StringBuilder sql, EmbeddingsDTO dto) {
        List<Object> params = new ArrayList<>();

        if (UtilMethods.isSet(dto.inode)) {
            sql.append(" and inode=? ");
            params.add(dto.inode);
        }
        if (UtilMethods.isSet(dto.identifier)) {
            sql.append(" and identifier=? ");
            params.add(dto.identifier);
        }

        if (UtilMethods.isSet(dto.excludeIdentifiers)) {
            for (String id : dto.excludeIdentifiers) {
                sql.append(" and identifier <> ? ");
                params.add(id);
            }
        }

        if (UtilMethods.isSet(dto.excludeInodes)) {
            for (String id : dto.excludeInodes) {
                sql.append(" and inode <> ? ");
                params.add(id);
            }
        }

        if (dto.language > 0) {
            sql.append(" and language=? ");
            params.add(dto.language);
        }

        if (UtilMethods.isSet(dto.contentType) && dto.contentType.length > 0) {
            sql.append(" and ( false ") ;
            for(String contentType : dto.contentType){
                sql.append(" OR lower(content_type)=lower(?)");
                params.add(contentType);
            }
            sql.append(") ") ;
        }

        if (UtilMethods.isSet(dto.host)) {
            sql.append(" and host=? ");
            params.add(dto.host);
        }
        if (UtilMethods.isSet(dto.indexName) && !ALL_INDICES.equals(dto.indexName)) {
            sql.append(" and lower(index_name)=lower(?) ");
            params.add(dto.indexName);
        }


        return params;


    }

    public int deleteEmbeddings(EmbeddingsDTO dto) {


        StringBuilder sql = new StringBuilder("delete from dot_embeddings where true ");
        List<Object> params = appendParams(sql, dto);
        Logger.info(EmbeddingsDB.class, "deleting embeddings:" + dto);

        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    public long countEmbeddings(EmbeddingsDTO dto) {
        StringBuilder sql = new StringBuilder(EmbeddingsSQL.COUNT_EMBEDDINGS_PREFIX.replace("{operator}", dto.operator));

        List<Object> params = appendParams(sql, dto);
        sql.append(" ) data ");

        if (dto.threshold != 0) {
            sql.append(" where distance <= ? ");
            params.add(dto.threshold);
        }
        params.add(0, new PGvector(ArrayUtils.toPrimitive(dto.embeddings)));


        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject((i + 1), params.get(i));
            }
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getLong("test");
            }
            return 0;
        } catch (SQLException e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e);
        }


    }

    public Map<String, Map<String, Object>> countEmbeddingsByIndex() {
        StringBuilder sql = new StringBuilder(EmbeddingsSQL.COUNT_EMBEDDINGS_BY_INDEX);

        try (Connection conn = getPGVectorConnection();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {

            Map<String, Map<String, Object>> results = new TreeMap<>();
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                results.put(
                        rs.getString("index_name"),
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
