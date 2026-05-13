package com.dotmarketing.startup.runonce;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Verifies the S3 Vanity URL mapping startup task DDL.
 */
public class Task260408CreateS3VanityAliasTableTest {

    private static final String TABLE_NAME = "static_s3_vanity_mapping";
    private static final Pattern BOUNDED_VARCHAR = Pattern.compile("varchar\\s*\\(");

    @Test
    public void postgresScriptCreatesStaticS3VanityMappingTableIfMissing() {
        final String script = normalizedPostgresScript();

        Assert.assertTrue(script.contains("create table if not exists " + TABLE_NAME));
    }

    @Test
    public void postgresScriptCreatesVanityUrlIndexIfMissing() {
        final String script = normalizedPostgresScript();

        Assert.assertTrue(script.contains("create index if not exists idx_static_s3_vanity_mapping_vurl"));
        Assert.assertTrue(script.contains("on " + TABLE_NAME + " (endpoint_id, vanity_url_id)"));
    }

    @Test
    public void postgresScriptUsesTimestamptzForModificationDate() {
        final String script = normalizedPostgresScript();

        Assert.assertTrue(script.contains("mod_date timestamptz not null"));
    }

    @Test
    public void postgresScriptDoesNotUseArtificialVarcharLimits() {
        final String script = normalizedPostgresScript();

        Assert.assertFalse(BOUNDED_VARCHAR.matcher(script).find());
    }

    @Test
    public void databaseScriptsShareTheSameTableDefinition() {
        final Task260408CreateS3VanityAliasTable task = new Task260408CreateS3VanityAliasTable();
        final String postgresScript = task.getPostgresScript();

        Assert.assertEquals(postgresScript, task.getMySQLScript());
        Assert.assertEquals(postgresScript, task.getOracleScript());
        Assert.assertEquals(postgresScript, task.getMSSQLScript());
    }

    private String normalizedPostgresScript() {
        return new Task260408CreateS3VanityAliasTable()
                .getPostgresScript()
                .toLowerCase(Locale.ROOT);
    }
}
