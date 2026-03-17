package com.dotmarketing.common.db;

import com.liferay.util.StringPool;
import java.util.Locale;

/**
 * Utilities for PGVector handling.
 * @author jsanca
 */
public final class PgVectorSql {

    private PgVectorSql() {}

    /**
     * Builds a SQL-literal for PGVector from a float array, e.g. '[0.1, -0.2, 0.3]'.
     * Safe for numbers; do NOT pass user-supplied strings here.
     */
    public static String toVectorLiteral(final float[] floatVector) {

        if (floatVector == null || floatVector.length == 0) {
            return "[]";
        }

        final StringBuilder sb = new StringBuilder(floatVector.length * 8);
        sb.append(StringPool.OPEN_BRACKET);

        for (int i = 0; i < floatVector.length; i++) {
            if (i > 0) {
                sb.append(StringPool.COMMA);
            }
            // force US locale for '.' decimal separator
            sb.append(String.format(Locale.US, "%f", floatVector[i]));
        }
        sb.append(StringPool.CLOSE_BRACKET);
        return sb.toString();
    }
}
