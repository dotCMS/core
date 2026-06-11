package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DotConnectionWrapper;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that hydrating a Content Type's fields does NOT eagerly load each field's
 * {@code field_variable} rows from the database, and that variables are loaded lazily,
 * exactly once per field, and served from the existing Content Type cache on subsequent reads.
 *
 * <p>This guards against the cold-cache N+1 burst of
 * {@code select ... from field_variable where field_id = ?} queries observed during content
 * hydration (e.g. {@code POST /api/content/_search} with {@code render:true}).</p>
 */
public class FieldVariableLazyHydrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link FieldFactoryImpl#byContentTypeId(String)} and the lazy
     * {@link Field#fieldVariables()} hydration path used during content rendering.
     * <p>
     * Given Scenario: A Content Type with several fields (one carrying field variables) is loaded
     * cold from the database.
     * <p>
     * Expected Result: Loading the fields issues ZERO {@code field_variable} queries (hydration is
     * lazy), and reading every field's variables afterwards issues exactly ONE query per field
     * (no duplicate/double loads), returning the correct variable data.
     */
    @Test
    public void loadingFields_doesNotEagerlyLoadVariables_andReadsEachFieldOnce() throws Exception {
        ContentType type = null;
        try {
            type = new ContentTypeDataGen().nextPersisted();
            final Field withVars = new FieldDataGen().contentTypeId(type.id()).type(TextField.class)
                    .velocityVarName("hasVars").nextPersisted();
            new FieldDataGen().contentTypeId(type.id()).type(TextField.class)
                    .velocityVarName("noVarsA").nextPersisted();
            new FieldDataGen().contentTypeId(type.id()).type(TextField.class)
                    .velocityVarName("noVarsB").nextPersisted();

            final FieldAPI fieldApi = APILocator.getContentTypeFieldAPI();
            fieldApi.save(ImmutableFieldVariable.builder().key("k1").value("v1")
                    .fieldId(withVars.id()).build(), APILocator.systemUser());
            fieldApi.save(ImmutableFieldVariable.builder().key("k2").value("v2")
                    .fieldId(withVars.id()).build(), APILocator.systemUser());

            final AtomicInteger fieldVarQueries = new AtomicInteger();
            final Connection current = DbConnectionFactory.getConnection();
            DbConnectionFactory.setConnection(
                    new CountingConnection(current, "field_variable", fieldVarQueries));
            try {
                final FieldFactory factory = new FieldFactoryImpl();

                // Loading the fields from the DB must NOT eagerly trigger field-variable queries.
                final List<Field> fields = factory.byContentTypeId(type.id());
                assertEquals("Loading fields must not eagerly load field variables (cold N+1)",
                        0, fieldVarQueries.get());

                // Lazily reading each field's variables loads exactly once per field (no doubles).
                fieldVarQueries.set(0);
                int totalVars = 0;
                for (final Field field : fields) {
                    totalVars += field.fieldVariables().size();
                }
                assertEquals("Each field's variables must load exactly once (no double-load)",
                        fields.size(), fieldVarQueries.get());
                assertEquals("Field variables must be loaded correctly", 2, totalVars);
            } finally {
                DbConnectionFactory.setConnection(current);
            }
        } finally {
            if (type != null && type.id() != null) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
            }
        }
    }

    /**
     * Method to test: the Content Type cache memoization of field variables.
     * <p>
     * Given Scenario: A cached Content Type's field variables are read once (warming them).
     * <p>
     * Expected Result: Reading the same cached Content Type's field variables again issues ZERO
     * additional {@code field_variable} queries — they are served from the existing cache.
     */
    @Test
    public void cachedContentType_servesFieldVariablesWithoutReloading() throws Exception {
        ContentType type = null;
        try {
            type = new ContentTypeDataGen().nextPersisted();
            final Field withVars = new FieldDataGen().contentTypeId(type.id()).type(TextField.class)
                    .velocityVarName("hasVars").nextPersisted();
            final FieldAPI fieldApi = APILocator.getContentTypeFieldAPI();
            fieldApi.save(ImmutableFieldVariable.builder().key("k1").value("v1")
                    .fieldId(withVars.id()).build(), APILocator.systemUser());

            final String typeId = type.id();

            // Warm: load the cached Content Type and read its field variables once.
            // NOTE: this guards the contract the lazy-hydration fix relies on — that the content-type
            // cache returns the SAME live ContentType instance, so @Value.Lazy fieldVariables() stays
            // memoized across reads. It holds for the in-memory (Caffeine) provider used today. If a
            // serialized/remote content-type cache is ever introduced, memoization becomes
            // per-round-trip and this assertion is the intended canary, not a flaky test.
            ContentType cached = APILocator.getContentTypeAPI(APILocator.systemUser()).find(typeId);
            cached.fields().forEach(Field::fieldVariables);

            final AtomicInteger fieldVarQueries = new AtomicInteger();
            final Connection current = DbConnectionFactory.getConnection();
            DbConnectionFactory.setConnection(
                    new CountingConnection(current, "field_variable", fieldVarQueries));
            try {
                // Same cached instance — reading variables again must hit the cache, not the DB.
                cached = APILocator.getContentTypeAPI(APILocator.systemUser()).find(typeId);
                cached.fields().forEach(Field::fieldVariables);
                assertEquals("Warm reads must be served from cache (0 field_variable queries)",
                        0, fieldVarQueries.get());
            } finally {
                DbConnectionFactory.setConnection(current);
            }
        } finally {
            if (type != null && type.id() != null) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
            }
        }
    }

    /**
     * Thread connection that counts {@link PreparedStatement} preparations whose SQL contains a
     * given needle (here, {@code field_variable}). Survives {@link DbConnectionFactory#setConnection}
     * because it is a {@link DotConnectionWrapper} (only {@code ManagedConnection} is unwrapped).
     */
    private static final class CountingConnection extends DotConnectionWrapper {

        private final String needle;
        private final AtomicInteger counter;

        CountingConnection(final Connection conn, final String needle, final AtomicInteger counter) {
            super(conn);
            this.needle = needle;
            this.counter = counter;
        }

        private void count(final String sql) {
            if (sql != null && sql.contains(needle)) {
                counter.incrementAndGet();
            }
        }

        @Override
        public PreparedStatement prepareStatement(final String sql) throws SQLException {
            count(sql);
            return super.prepareStatement(sql);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType,
                final int resultSetConcurrency) throws SQLException {
            count(sql);
            return super.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType,
                final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
            count(sql);
            return super.prepareStatement(sql, resultSetType, resultSetConcurrency,
                    resultSetHoldability);
        }

        /**
         * DotConnect's cached-prepared-statement path prepares against
         * {@code getMetaData().getConnection()}; return metadata whose {@code getConnection()}
         * yields this wrapper so that path is counted too.
         */
        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            final DatabaseMetaData real = super.getMetaData();
            return (DatabaseMetaData) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[]{DatabaseMetaData.class},
                    (proxy, method, args) -> "getConnection".equals(method.getName())
                            ? this
                            : method.invoke(real, args));
        }
    }
}
