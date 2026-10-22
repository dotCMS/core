package com.dotcms.enterprise.achecker.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dotcms.enterprise.achecker.model.LanguageTextBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the language text lookups of {@link LanguageTextDAO}, run against an in-memory H2
 * database supplied to the DAO (the dotCMS Accessibility Checker database is not needed).
 */
class LanguageTextDAOTest {

    private Connection connection;
    private LanguageTextDAO languageTextDAO;

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        createLanguageTexts();
        languageTextDAO = new LanguageTextDAO(new DAOImpl(() -> connection));
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
    void textIsFoundForTermAndLanguage() throws Exception {
        final LanguageTextBean text = languageTextDAO.getByTermAndLang("_CNAME_1", "eng");

        assertNotNull(text);
        assertEquals("Name one", text.getText());
        assertEquals("eng", text.getLanguage_code());
        assertEquals("_CNAME_1", text.getTerm());
    }

    @Test
    void languageValueWithQuoteAndSpacesMatchesNoText() throws Exception {
        assertNull(languageTextDAO.getByTermAndLang("_CNAME_1", "eng' or '1'='1"));
    }

    private void createLanguageTexts() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE AC_language_text ("
                    + "language_code VARCHAR(20) NOT NULL DEFAULT '', "
                    + "variable VARCHAR(30) NOT NULL DEFAULT '', "
                    + "term VARCHAR(50) NOT NULL DEFAULT '', "
                    + "text TEXT NOT NULL, "
                    + "revised_date VARCHAR(20) NOT NULL DEFAULT '0000-00-00 00:00:00', "
                    + "context TEXT, "
                    + "PRIMARY KEY (language_code, variable, term))");
            statement.execute("INSERT INTO AC_language_text VALUES "
                    + "('eng', '_check', '_CNAME_1', 'Name one', '2008-11-24 13:01:07', '')");
        }
    }
}
