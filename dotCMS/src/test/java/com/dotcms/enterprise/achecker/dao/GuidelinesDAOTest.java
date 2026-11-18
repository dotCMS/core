package com.dotcms.enterprise.achecker.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dotcms.enterprise.achecker.model.GuideLineBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the guideline lookups of {@link GuidelinesDAO}, run against an in-memory H2
 * database supplied to the DAO (the dotCMS Accessibility Checker database is not needed).
 */
class GuidelinesDAOTest {

    private Connection connection;
    private GuidelinesDAO guidelinesDAO;

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        createGuidelines();
        guidelinesDAO = new GuidelinesDAO(new DAOImpl(() -> connection));
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
    void enabledGuidelineIsFoundIgnoringCase() throws Exception {
        final GuideLineBean guideline = guidelinesDAO.getEnabledGuidelinesByAbbr("wcag2-aa", true);

        assertNotNull(guideline);
        assertEquals(7, guideline.getGuideline_id());
        assertEquals("WCAG2-AA", guideline.getAbbr());
    }

    @Test
    void valueWithQuoteAndSpacesMatchesNoGuideline() throws Exception {
        final GuideLineBean guideline = guidelinesDAO.getEnabledGuidelinesByAbbr("wcag2-aa' or '1'='1", true);

        assertNull(guideline);
        assertEquals(2, countGuidelines());
    }

    private void createGuidelines() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE AC_guidelines ("
                    + "guideline_id INT NOT NULL PRIMARY KEY, "
                    + "user_id INT NOT NULL DEFAULT 0, "
                    + "title VARCHAR(255), "
                    + "abbr VARCHAR(100), "
                    + "long_name VARCHAR(255), "
                    + "earlid VARCHAR(255), "
                    + "preamble TEXT, "
                    + "status TINYINT NOT NULL DEFAULT 0, "
                    + "open_to_public TINYINT NOT NULL DEFAULT 0, "
                    + "seal_icon_name VARCHAR(255), "
                    + "subset VARCHAR(100) NOT NULL DEFAULT '0')");
            statement.execute("INSERT INTO AC_guidelines (guideline_id, title, abbr, status, open_to_public) "
                    + "VALUES (7, 'WCAG 2.0 AA', 'WCAG2-AA', 1, 1)");
            statement.execute("INSERT INTO AC_guidelines (guideline_id, title, abbr, status, open_to_public) "
                    + "VALUES (2, 'Section 508', '508', 1, 1)");
        }
    }

    private int countGuidelines() throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM AC_guidelines")) {
            count.next();
            return count.getInt(1);
        }
    }
}
