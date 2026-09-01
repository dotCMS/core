package com.dotcms.integritycheckers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.postgresql.core.Parser;

/**
 * Fast, infrastructure-free guard for the bug described in
 * <a href="https://github.com/dotCMS/core/issues/37325">dotCMS/core#37325</a>: the Integrity Checker
 * "Fix Inconsistencies" used to inline the serialized contentlet JSON ({@code contentlet_as_json})
 * into the generated SQL via {@code String.format(..., '%s', ...)}. Any single quote (apostrophe)
 * inside the content &#8212; e.g. "Ensure your data's security..." &#8212; terminated the SQL string
 * literal early, producing a PostgreSQL {@code Unterminated identifier} parse error and aborting the fix.
 *
 * <p>This test validates the two SQL shapes through the actual PostgreSQL JDBC driver's SQL parser
 * ({@code org.postgresql.core.Parser}), the same parser that raised the original error:</p>
 * <ul>
 *   <li>The pre-fix shape (JSON inlined via {@code '%s'}) fails to parse when the JSON contains an
 *   apostrophe.</li>
 *   <li>The fixed shape (JSON bound as a JDBC {@code ?} parameter via {@code DotConnect.addJSONParam})
 *   parses correctly regardless of the JSON content, because the JSON never becomes part of the SQL
 *   text.</li>
 * </ul>
 *
 * <p>End-to-end coverage (executing the actual checkers against a real database) lives in
 * {@code ContentPageIntegrityCheckerTest#TestFixConflictWhenContentContainsSingleQuotes} and
 * {@code ContentFileAssetIntegrityCheckerTest#TestFixConflictWhenContentContainsSingleQuotes}
 * in the {@code dotcms-integration} module.</p>
 */
public class ContentletAsJsonSqlBindingTest {

    /**
     * Representative serialized contentlet, taken from the original failure: the
     * {@code ogDescription} value contains an apostrophe ("data's"), exactly like the content that
     * broke the Integrity Checker fix.
     */
    private static final String CONTENTLET_AS_JSON =
            "{\n"
            + "  \"title\" : \"BROKEN Security & Compliance\",\n"
            + "  \"inode\" : \"3be3cb1a-c28a-4bf3-a9b1-e5cf6c54613c\",\n"
            + "  \"identifier\" : \"e6bae33c7ac0d54d281b429d0a23c686\",\n"
            + "  \"baseType\" : \"HTMLPAGE\",\n"
            + "  \"fields\" : {\n"
            + "    \"ogDescription\" : {\n"
            + "      \"value\" : \"Ensure your data's security and compliance with dotCMS.\",\n"
            + "      \"type\" : \"TextArea\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    /**
     * A JSON payload without apostrophes; the pre-fix inlined SQL worked with this, which is why the
     * bug went unnoticed for so long.
     */
    private static final String CONTENTLET_AS_JSON_NO_APOSTROPHES =
            CONTENTLET_AS_JSON.replace("data's", "data");

    /**
     * The pre-fix SQL shape used by {@code ContentPageIntegrityChecker#fixContentPageConflicts}
     * (and the equivalent statements in {@code ContentFileAssetIntegrityChecker} and
     * {@code HostIntegrityChecker}): the JSON was inlined into the SELECT list through a
     * {@code '%s'} format specifier.
     */
    private static final String PRE_FIX_SQL_TEMPLATE =
            "INSERT INTO contentlet(inode, identifier, language_id, contentlet_as_json) "
            + "SELECT ?, ?, ?, '%s' "
            + "FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.working_inode) "
            + "WHERE c.identifier = ? and c.language_id = ?";

    /**
     * The fixed SQL shape: the JSON is bound as a JDBC parameter through
     * {@code DotConnect.addJSONParam(...)}, so the SQL text is content-independent.
     */
    private static final String FIXED_SQL =
            "INSERT INTO contentlet(inode, identifier, language_id, contentlet_as_json) "
            + "SELECT ?, ?, ?, ? "
            + "FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.working_inode) "
            + "WHERE c.identifier = ? and c.language_id = ?";

    /**
     * When: The pre-fix pattern is used and the contentlet JSON contains an apostrophe.
     * Should: The PostgreSQL JDBC driver's parser rejects the statement, reproducing the
     * "Unterminated identifier" error reported by the Integrity Checker. This is the exact
     * code path ({@code Parser.replaceProcessing}) that raised the original production error.
     */
    @Test
    public void test_pre_fix_inlined_json_with_apostrophe_breaks_postgres_parser() {
        final String inlinedSql = String.format(PRE_FIX_SQL_TEMPLATE, CONTENTLET_AS_JSON);
        final SQLException exception = assertThrows(SQLException.class,
                () -> Parser.replaceProcessing(inlinedSql, true, true),
                "Inlining a JSON payload with an apostrophe into the SQL must break the PostgreSQL parser");
        assertTrue(exception.getMessage().contains("Unterminated identifier"),
                "Unexpected parser error: " + exception.getMessage());
    }

    /**
     * When: The pre-fix pattern is used but the JSON happens to contain no apostrophes.
     * Should: The statement parses. This documents why the bug only surfaced intermittently,
     * depending on the content being fixed.
     */
    @Test
    public void test_pre_fix_inlined_json_without_apostrophes_parses() {
        final String inlinedSql = String.format(PRE_FIX_SQL_TEMPLATE, CONTENTLET_AS_JSON_NO_APOSTROPHES);
        assertDoesNotThrow(() -> Parser.replaceProcessing(inlinedSql, true, true));
    }

    /**
     * When: The fixed pattern is used (JSON bound as a JDBC parameter).
     * Should: The statement parses fine even though the JSON contains apostrophes, because the
     * payload never becomes part of the SQL text. Also asserts the SQL is payload-free, as an
     * additional guard against re-introducing the inlining.
     */
    @Test
    public void test_fixed_parameterized_sql_parses_and_contains_no_json_payload() {
        assertFalse(FIXED_SQL.contains(CONTENTLET_AS_JSON),
                "The fixed SQL must not contain the JSON payload");
        assertFalse(FIXED_SQL.contains("data's"),
                "The fixed SQL must not contain any content value");
        assertDoesNotThrow(() -> Parser.replaceProcessing(FIXED_SQL, true, true));
    }

}
