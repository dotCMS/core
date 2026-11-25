package com.dotcms.enterprise.achecker.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dotcms.enterprise.achecker.model.LanguageCodeBean;
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
 * Unit tests for the language code lookups of {@link LangCodesDAO}, run against an in-memory H2
 * database supplied to the DAO (the dotCMS Accessibility Checker database is not needed).
 */
class LangCodesDAOTest {

    private Connection connection;
    private LangCodesDAO langCodesDAO;

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        createLanguageCodes();
        langCodesDAO = new LangCodesDAO(new DAOImpl(() -> connection));
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
    void threeLetterCodeWithQuoteAndSpacesMatchesNoRow() throws Exception {
        final LanguageCodeBean found = langCodesDAO.getLangCodeBy3LetterCode("eng");
        assertNotNull(found);
        assertEquals("en", found.getCode_2letters());

        final LanguageCodeBean notFound = langCodesDAO.getLangCodeBy3LetterCode("eng' or '1'='1");

        assertNull(notFound);
        assertEquals(2, countLanguageCodes());
    }

    @Test
    void twoLetterCodeIsFoundAndQuotedValueIsNot() throws Exception {
        final LanguageCodeBean found = langCodesDAO.getLangCodeBy2LetterCode("en");
        assertNotNull(found);
        assertEquals("eng", found.getCode_3letters());

        assertNull(langCodesDAO.getLangCodeBy2LetterCode("en' or '1'='1"));
        assertEquals(2, countLanguageCodes());
    }

    private void createLanguageCodes() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE AC_lang_codes ("
                    + "code_3letters VARCHAR(3) NOT NULL DEFAULT '' PRIMARY KEY, "
                    + "direction VARCHAR(16) NOT NULL DEFAULT '', "
                    + "code_2letters VARCHAR(2), "
                    + "description VARCHAR(50))");
            statement.execute("INSERT INTO AC_lang_codes VALUES ('eng', 'ltr', 'en', 'English')");
            statement.execute("INSERT INTO AC_lang_codes VALUES ('ara', 'rtl', 'ar', 'Arabic')");
        }
    }

    private int countLanguageCodes() throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM AC_lang_codes")) {
            count.next();
            return count.getInt(1);
        }
    }
}
