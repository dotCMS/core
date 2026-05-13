package com.dotcms.enterprise.publishing.staticpublishing;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Verifies the SQL contract used by the S3 Vanity URL mapping repository.
 */
public class S3VanityAliasRepositoryTest {

    private static final String TABLE_NAME = "static_s3_vanity_mapping";
    private static final String OLD_TABLE_NAME = "s3_vanity_alias";

    @Test
    public void tableNameReturnsStaticS3VanityMapping() {
        Assert.assertEquals(TABLE_NAME, new S3VanityAliasRepository().tableName());
    }

    @Test
    public void sqlStatementsUseStaticS3VanityMappingTable() throws IllegalAccessException {
        for (final Field field : S3VanityAliasRepository.class.getDeclaredFields()) {
            if (isSqlString(field)) {
                field.setAccessible(true);
                final String sql = String.valueOf(field.get(null));

                Assert.assertFalse(sql.contains(OLD_TABLE_NAME));
                Assert.assertTrue(sql.contains(TABLE_NAME));
            }
        }
    }

    private boolean isSqlString(final Field field) {
        return String.class.equals(field.getType())
                && Modifier.isStatic(field.getModifiers())
                && !field.getName().equals("TABLE_NAME");
    }
}
