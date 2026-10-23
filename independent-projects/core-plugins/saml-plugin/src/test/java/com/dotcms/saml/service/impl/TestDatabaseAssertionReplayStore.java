package com.dotcms.saml.service.impl;

import com.dotcms.saml.service.external.SamlException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link DatabaseAssertionReplayStore} against a fake JDBC layer that behaves like Postgres for the four
 * statements the store uses (the SQL itself is checked against a real Postgres separately).
 */
public class TestDatabaseAssertionReplayStore {

    private final Map<String, Long> table = new HashMap<>();
    private final List<String> executed = new ArrayList<>();
    private final AtomicLong now = new AtomicLong(1_000_000L);
    private boolean failCreate;
    private boolean failInsert;
    private DatabaseAssertionReplayStore store;

    @Before
    public void setUp() {

        this.store = new DatabaseAssertionReplayStore(this::connection, this.now::get, new AtomicBoolean(false));
    }

    @Test
    public void firstUseIsAcceptedAndAReplayIsRejected() {

        Assert.assertTrue(this.store.markUsed("idp|_assertion1", 2_000_000L));
        Assert.assertFalse(this.store.markUsed("idp|_assertion1", 2_000_000L));
        Assert.assertTrue(this.store.markUsed("idp|_assertion2", 2_000_000L));
    }

    @Test
    public void storesAFixedLengthHashOfTheKey() {

        this.store.markUsed("idp|" + new String(new char[2000]).replace('\0', 'x'), 2_000_000L);

        final String storedKey = this.table.keySet().iterator().next();
        Assert.assertEquals(64, storedKey.length());
        Assert.assertTrue(storedKey.matches("[0-9a-f]{64}"));
    }

    @Test
    public void expiredRowsAreDeletedBeforeTheCheck() {

        this.store.markUsed("idp|_old", 1_500_000L);
        this.now.set(1_600_000L);
        this.store.markUsed("idp|_new", 3_000_000L);

        Assert.assertEquals(1, this.table.size());
    }

    @Test
    public void tableIsCreatedOnce() {

        this.store.markUsed("idp|_a", 2_000_000L);
        this.store.markUsed("idp|_b", 2_000_000L);

        Assert.assertEquals(1, this.executed.stream().filter(sql -> sql.startsWith("CREATE TABLE")).count());
        Assert.assertEquals(1, this.executed.stream().filter(sql -> sql.startsWith("CREATE INDEX")).count());
    }

    @Test
    public void failedTableCreationIsRetriedAndDoesNotSkipTheCheck() {

        this.failCreate = true;
        Assert.assertTrue(this.store.markUsed("idp|_a", 2_000_000L));
        this.failCreate = false;
        Assert.assertFalse(this.store.markUsed("idp|_a", 2_000_000L));

        Assert.assertEquals(2, this.executed.stream().filter(sql -> sql.startsWith("CREATE TABLE")).count());
    }

    @Test
    public void databaseErrorIsReportedNotTreatedAsFirstUse() {

        this.failInsert = true;
        try {
            this.store.markUsed("idp|_a", 2_000_000L);
            Assert.fail("expected a SamlException");
        } catch (SamlException e) {
            Assert.assertTrue(e.getMessage().contains("already been used"));
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    private Connection connection() {

        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "createStatement":  return statement();
                        case "prepareStatement": return preparedStatement((String) args[0]);
                        default:                 return null;
                    }
                });
    }

    private Statement statement() {

        return (Statement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    if ("execute".equals(method.getName())) {
                        this.executed.add((String) args[0]);
                        if (this.failCreate) {
                            throw new SQLException("relation already exists");
                        }
                        return false;
                    }
                    return null;
                });
    }

    private PreparedStatement preparedStatement(final String sql) {

        final Map<Integer, Object> parameters = new HashMap<>();
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setString":
                        case "setLong":
                            parameters.put((Integer) args[0], args[1]);
                            return null;
                        case "executeUpdate":
                            return this.execute(sql, parameters);
                        default:
                            return null;
                    }
                });
    }

    private int execute(final String sql, final Map<Integer, Object> parameters) throws SQLException {

        this.executed.add(sql);
        if (DatabaseAssertionReplayStore.DELETE_EXPIRED.equals(sql)) {

            final long cutoff = (Long) parameters.get(1);
            final int before = this.table.size();
            this.table.values().removeIf(expiresAt -> expiresAt < cutoff);
            return before - this.table.size();
        }

        if (DatabaseAssertionReplayStore.INSERT.equals(sql)) {

            if (this.failInsert) {
                throw new SQLException("connection refused");
            }
            return null == this.table.putIfAbsent((String) parameters.get(1), (Long) parameters.get(2)) ? 1 : 0;
        }

        throw new SQLException("unexpected statement: " + sql);
    }
}
