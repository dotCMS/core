package com.dotcms.enterprise.achecker.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dotcms.enterprise.achecker.model.GuideLineBean;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the parameter-binding query path of {@link DAOImpl}, run against an in-memory H2
 * database supplied to the DAO (the dotCMS Accessibility Checker database is not needed).
 */
class DAOImplTest {

    /** H2 2.2.x error code for a table that does not exist (reported with name candidates). */
    private static final int TABLE_NOT_FOUND = 42104;

    private Connection connection;
    private DAOImpl dao;

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        dao = new DAOImpl(() -> connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        } finally {
            connection.close();
        }
    }

    @Test
    void boundParameterSelectsMatchingRow() throws SQLException {
        createGuidelines();

        final List<Map<String, Object>> rows =
                dao.execute("select * from AC_guidelines where lower(abbr) = ?", "wcag2-aa");

        assertEquals(1, rows.size());
        assertEquals("WCAG2-AA", rows.get(0).get("abbr"));
    }

    @Test
    void valueWithApostropheIsMatchedLiterally() throws SQLException {
        run("create table AC_names (abbr varchar(100))",
                "insert into AC_names values ('o''neil')",
                "insert into AC_names values ('other')");

        final List<Map<String, Object>> rows =
                dao.execute("select * from AC_names where abbr = ?", "o'neil");

        assertEquals(1, rows.size());
        assertEquals("o'neil", rows.get(0).get("abbr"));
    }

    @Test
    void integerParameterIsBound() throws SQLException {
        createGuidelines();

        final List<Map<String, Object>> rows =
                dao.execute("select * from AC_guidelines where guideline_id = ?", Integer.valueOf(7));

        assertEquals(1, rows.size());
        assertEquals("WCAG2-AA", rows.get(0).get("abbr"));

        final List<Map<String, Object>> castRows = dao.execute("select cast(? as int) as v", Integer.valueOf(7));

        assertEquals(1, castRows.size());
        assertEquals(Integer.valueOf(7), castRows.get(0).get("v"));
    }

    @Test
    void executeOneReturnsNullWhenNoRow() throws SQLException {
        createGuidelines();
        final BaseDAO baseDAO = new BaseDAO(dao);

        assertNull(baseDAO.executeOne("select * from AC_guidelines where abbr = ?", "other"));
        assertNull(baseDAO.executeOne(GuideLineBean.class,
                "select * from AC_guidelines where abbr = ?", "other"));
    }

    @Test
    void typedOverloadMapsRowsToBeans() throws SQLException {
        createGuidelines();
        final BaseDAO baseDAO = new BaseDAO(dao);

        final List<GuideLineBean> beans = baseDAO.execute(GuideLineBean.class,
                "select * from AC_guidelines where abbr = ?", "WCAG2-AA");

        assertEquals(1, beans.size());
        final GuideLineBean bean = beans.get(0);
        assertEquals(7, bean.getGuideline_id());
        assertEquals("WCAG2-AA", bean.getAbbr());
        assertEquals("WCAG 2.0 AA", bean.getTitle());

        final GuideLineBean single = baseDAO.executeOne(GuideLineBean.class,
                "select * from AC_guidelines where abbr = ?", "508");
        assertNotNull(single);
        assertEquals(8, single.getGuideline_id());
    }

    @Test
    void failureMessageDoesNotEchoParameterValue() {
        // Given a connection whose driver reports the bound value in its error message
        final DAOImpl failingDao = new DAOImpl(() -> connectionFailingWith(
                new SQLException("bad value marker-value", "22018", 22018)));

        // When a query fails
        final SQLException failure = assertThrows(SQLException.class,
                () -> failingDao.execute("select * from AC_guidelines where abbr = ?", "marker-value"));

        // Then only the generic message, SQL state and error code reach the caller
        assertEquals(DAOImpl.QUERY_FAILED_MESSAGE, failure.getMessage());
        assertFalse(failure.getMessage().contains("marker-value"));
        assertEquals("22018", failure.getSQLState());
        assertEquals(22018, failure.getErrorCode());
        assertNull(failure.getCause());
    }

    @Test
    void missingTableFailureKeepsTheErrorCode() {
        final SQLException failure = assertThrows(SQLException.class,
                () -> dao.execute("select * from AC_missing where abbr = ?", "marker-value"));

        assertEquals(DAOImpl.QUERY_FAILED_MESSAGE, failure.getMessage());
        assertEquals(TABLE_NOT_FOUND, failure.getErrorCode());
        assertNull(failure.getCause());
    }

    @Test
    void queryWithoutValuesStillMapsRows() throws SQLException {
        // Given the query form kept for queries built from numeric ids
        createGuidelines();
        final BaseDAO baseDAO = new BaseDAO(dao);

        // When a typed query without placeholders runs
        final List<GuideLineBean> beans = baseDAO.execute(GuideLineBean.class,
                "select * from AC_guidelines where guideline_id = 7");
        final GuideLineBean single = baseDAO.executeOne(GuideLineBean.class,
                "select * from AC_guidelines where guideline_id = 8");

        // Then the rows are mapped as before
        assertEquals(1, beans.size());
        assertEquals("WCAG2-AA", beans.get(0).getAbbr());
        assertNotNull(single);
        assertEquals("508", single.getAbbr());
    }

    @Test
    void rowThatCannotBecomeABeanFailsWithFixedMessage() throws SQLException {
        // Given a bean class without a constructor taking the row map
        createGuidelines();
        final BaseDAO baseDAO = new BaseDAO(dao);

        // When a row is read into it
        final SQLException failure = assertThrows(SQLException.class,
                () -> baseDAO.executeOne(StringBuilder.class, "select * from AC_guidelines where abbr = ?", "508"));

        // Then the message is fixed and does not name the class
        assertEquals("Accessibility Checker row could not be read", failure.getMessage());
    }

    /** Builds a connection whose {@code prepareStatement} always throws the given exception. */
    private static Connection connectionFailingWith(final SQLException failure) {
        return (Connection) Proxy.newProxyInstance(DAOImplTest.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    if ("prepareStatement".equals(method.getName())) {
                        throw failure;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private void createGuidelines() throws SQLException {
        run("create table AC_guidelines (guideline_id int, abbr varchar(100), title varchar(255))",
                "insert into AC_guidelines values (7, 'WCAG2-AA', 'WCAG 2.0 AA')",
                "insert into AC_guidelines values (8, '508', 'Section 508')");
    }

    private void run(final String... statements) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (final String sql : statements) {
                statement.execute(sql);
            }
        }
    }
}
