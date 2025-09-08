package com.dotcms.ai.v2.api.embeddings;

import com.dotcms.config.DotInitializer;
import com.dotcms.util.ClasspathResourceLoader;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Initializes the RAG schema (content metadata + embeddings) using PGVector.
 * <p>
 * It loads an .sql file from the classpath and executes it via DotConnect,
 * using a PGVector-capable JDBC connection.
 */
public class RagSchemaInitializer implements DotInitializer, Serializable {

    private static final long serialVersionUID = 1L;
    public static final Lazy<String> SQL_RAG_SCHEMA_SQL = Lazy.of(()-> Config.getStringProperty("SQL_RAG_SCHEME_LOCATION","/sql/rag_schema.sql"));

    private final String sqlResourcePath;

    /**
     * Default constructor using the standard resource path and a default connection provider.
     * Resource: {@code /sql/rag_schema.sql}
     */
    public RagSchemaInitializer() {
        this(SQL_RAG_SCHEMA_SQL.get());
    }

    /**
     * Customizable constructor (handy for testing).
     *
     * @param sqlResourcePath classpath resource path for the SQL script
     */
    public RagSchemaInitializer(final String sqlResourcePath) {
        this.sqlResourcePath = sqlResourcePath;
    }

    /**
     * Executes the SQL script found at the configured classpath location.
     * Wraps exceptions into DotRuntimeException for dotCMS bootstrap compatibility.
     */
    @Override
    public void init() {

        final String sql = ClasspathResourceLoader.readTextOrThrow(sqlResourcePath);
        if (org.apache.commons.lang3.StringUtils.isBlank(sql)) {
            throw new DotRuntimeException("RAG schema SQL is empty: " + sqlResourcePath);
        }
        Logger.info(this, () -> "Initializing RAG schema from: " + sqlResourcePath);

        try (final Connection db = DbConnectionFactory.getPGVectorConnection()) {
            new DotConnect().setSQL(sql).loadResult(db);
            Logger.info(this, () -> "RAG schema initialized successfully.");
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException("Failed to initialize RAG schema", e);
        }
    }

    @Override
    public String getName() {
        return getClass().getName() + " [" + sqlResourcePath + "]";
    }
}
