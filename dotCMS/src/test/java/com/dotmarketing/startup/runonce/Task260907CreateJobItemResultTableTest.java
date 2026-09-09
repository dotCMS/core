package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Pins the one thing the upgrade task's own javadoc admits nothing checks: that its DDL and the
 * copy in {@code postgres.sql} still describe the same table.
 * <p>
 * <b>Why a new table needs two homes at all.</b> A run-once task skips {@code executeUpgrade()}
 * when {@code DB_VERSION} is 0 — a fresh database — while still recording the version as applied.
 * So on a new install the task never runs and can never self-correct, and the table has to be
 * declared in {@code postgres.sql} as well.
 * <p>
 * <b>What that costs.</b> Two definitions of one table, kept in step by hand. Let them drift and a
 * fresh install and an upgraded one get different schemas — a difference nothing surfaces until
 * something reads a column one of them does not have, on one class of installation only. That is
 * about the worst shape a defect can take, and it is invisible to every other test here because
 * both halves are correct in isolation.
 *
 * @author dotCMS
 */
public class Task260907CreateJobItemResultTableTest {

    private static final String TABLE = "job_item_result";

    /**
     * Method to test: {@link Task260907CreateJobItemResultTable#getPostgresScript()} against
     * {@code postgres.sql}
     * <p>
     * Given scenario: The table is declared in both places, as it must be.
     * <p>
     * Expected result: Both declare the same columns, in the same order, with the same types and
     * nullability — and the same primary key.
     * <p>
     * Compared column by column rather than as text: the two are formatted differently on purpose
     * (one is a Java string concatenation, the other readable SQL), so a whitespace comparison
     * would fail permanently and be deleted, which is worse than no test.
     */
    @Test
    public void test_theUpgradeTaskAndPostgresSql_declareTheSameTable() throws IOException {

        final List<String> fromTask =
                columnsOf(new Task260907CreateJobItemResultTable().getPostgresScript());
        final List<String> fromStarter = columnsOf(readPostgresSql());

        assertTrue("the upgrade task must declare " + TABLE, !fromTask.isEmpty());
        assertTrue(TABLE + " must also be declared in postgres.sql, or a fresh install will "
                + "never get it — the task is skipped and marked applied when DB_VERSION is 0",
                !fromStarter.isEmpty());

        assertEquals(
                "the upgrade task and postgres.sql have drifted, so an upgraded install and a "
                        + "fresh one would get different tables\n"
                        + "  upgrade task : " + fromTask + "\n"
                        + "  postgres.sql : " + fromStarter,
                fromTask, fromStarter);
    }

    /**
     * Method to test: {@link Task260907CreateJobItemResultTable#getPostgresScript()}
     * <p>
     * Given scenario: The DDL is read.
     * <p>
     * Expected result: It creates the index the resume path reads through, and keys the table on
     * {@code (job_id, seq)} rather than on the item key.
     * <p>
     * The primary key is the load-bearing choice: a batch may legitimately carry two files of one
     * name, so keying on the name would reject the second write outright and leave a resumed run
     * unable to tell them apart.
     */
    @Test
    public void test_theTableIsKeyedOnSeq_andIndexesTheItemKey() {
        final String ddl = new Task260907CreateJobItemResultTable()
                .getPostgresScript().toLowerCase(Locale.ROOT);

        assertTrue("keyed on (job_id, seq), never on the item key",
                ddl.contains("primary key (job_id, seq)"));
        assertTrue("the item key is indexed but deliberately not unique",
                ddl.contains("create index") && ddl.contains("(job_id, item_key)"));
    }

    /** The column definitions of the {@code job_item_result} CREATE TABLE, normalised. */
    private static List<String> columnsOf(final String sql) {

        final Matcher create = Pattern.compile(
                        "create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?" + TABLE + "\\s*\\((.*?)\\)\\s*;",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(sql);

        final List<String> columns = new ArrayList<>();
        if (!create.find()) {
            return columns;
        }

        // Split on commas that are not inside parentheses, so "primary key (job_id, seq)" and
        // "varchar(255)" survive intact.
        int depth = 0;
        final StringBuilder current = new StringBuilder();
        for (final char c : create.group(1).toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                columns.add(normalise(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        columns.add(normalise(current.toString()));
        columns.removeIf(String::isEmpty);
        return columns;
    }

    private static String normalise(final String column) {
        return column.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String readPostgresSql() throws IOException {
        try (InputStream in = Task260907CreateJobItemResultTableTest.class
                .getResourceAsStream("/postgres.sql")) {
            assertNotNull("postgres.sql must be on the test classpath", in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
