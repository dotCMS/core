package com.dotmarketing.common.util;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.util.SecurityLoggerServiceAPIFactory;
import com.liferay.util.StringPool;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class SQLUtilTest  extends UnitTestBase {

    protected static SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

    @Before
    public void before () {

        SecurityLoggerServiceAPIFactory.setAlternativeSecurityLogger(securityLoggerServiceAPI);
    }

    @Test()
    public void testNullSanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter(null);

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testEmptySanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelectSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("select");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelect2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("select*");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelect3SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("selecbox select");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelect4SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("select all");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelect5SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("select ");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelect6SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter(" select ");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelect7SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter(" select");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidSelect8SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx select xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }


    @Test()
    public void testValidSelectBoxSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("selectBox");

        assertNotNull(s);
        assertEquals("selectBox", s);
    }

    @Test()
    public void testValidSelectBox2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx selectBox xxx");

        assertNotNull(s);
        assertEquals("xxx selectBox xxx", s);
    }

    @Test()
    public void testValidSelect3SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx select1 xxx");

        assertNotNull(s);
        assertEquals("xxx select1 xxx", s);
    }

    @Test
    public void testValidSelectBox3SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx select_box xxx");

        assertNotNull(s);
        assertEquals("xxx select_box xxx", s);
    }

    @Test
    public void testValidSelectBox4SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx select_ xxx");

        assertNotNull(s);
        assertEquals("xxx select_ xxx", s);
    }

    @Test
    public void testValidSelectBox5SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx _select xxx");

        assertNotNull(s);
        assertEquals("xxx _select xxx", s);
    }

    @Test
    public void testValidSelectBox6SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx _select_ xxx");

        assertNotNull(s);
        assertEquals("xxx _select_ xxx", s);
    }


    @Test
    public void testValidSelectBox7SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx _select_box_ xxx");

        assertNotNull(s);
        assertEquals("xxx _select_box_ xxx", s);
    }



    @Test
    public void testValidSelectBox8SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx select-box xxx");

        assertNotNull(s);
        assertEquals("xxx select-box xxx", s);
    }

    @Test
    public void testValidSelectBox9SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx select- xxx");

        assertNotNull(s);
        assertEquals("xxx select- xxx", s);
    }

    @Test
    public void testValidSelectBox10SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx -select xxx");

        assertNotNull(s);
        assertEquals("xxx -select xxx", s);
    }

    @Test
    public void testValidSelectBox11SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx -select- xxx");

        assertNotNull(s);
        assertEquals("xxx -select- xxx", s);
    }


    @Test
    public void testValidSelectBox12SanitizeParameter() throws Exception {

        final String s = SQLUtil.sanitizeParameter("xxx -select-box- xxx");

        assertNotNull(s);
        assertEquals("xxx -select-box- xxx", s);
    }

    @Test()
    public void testValidTableSelectSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("tableselect");

        assertNotNull(s);
        assertEquals("tableselect", s);
    }

    @Test()
    public void testValidEvilWordsSanitizeParameter() throws Exception {

        final String [] evilWords = {"select", "insert", "delete", "update", "replace", "create", "distinct", "like", "and ", "or ", "limit",
                "group", "order", "as ", "count","drop", "alter","truncate", "declare", "where", "exec", "--", "procedure", "pg_", "lock",
                "unlock","write", "engine", "null","not ","mode", "set ",";"};


        for (String evilWord : evilWords) {

            final String s = SQLUtil.sanitizeParameter("xxx" + evilWord + "xxxx");

            assertNotNull(s);
            assertEquals("xxx" + evilWord + "xxxx", s);
        }
    }

    private static String exploit1 = "\\') rlike (select/**/(case/**/when/**/((substring((select/**/password_/**/from/**/dotcms37new.user_/**/limit/**/1,1),1,1)/**/like/**/binary/**/\"1\"))/**/then/**/1/**/else/**/0x28/**/end))#";

    @Test()
    public void testValidExploit1SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter(exploit1);

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testExploit2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("q=') or 1=1");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testExploit3SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("\\\\') rlike (select/*/sleep(1))#");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }


    @Test()
    public void testInValidWordsSanitizeParameter() throws Exception {

        final String [] evilWords = {"select", "insert", "delete", "update", "replace", "create", "distinct", "like", "and ", "or ", "limit",
                "group", "order", "as ", "count","drop", "alter","truncate", "declare", "where", "exec", "--", "procedure", "pg_", "lock",
                "unlock","write", "engine", "null","not ","mode", "set ",";"};


        for (String evilWord : evilWords) {

            final String s = SQLUtil.sanitizeParameter(evilWord);

            assertNotNull(s);
            assertEquals(StringPool.BLANK, s);
        }
    }

    @Test()
    public void testInValidWordsBeforePaddingSanitizeParameter() throws Exception {

        final String [] evilWords = {"select", "insert", "delete", "update", "replace", "create", "distinct", "like", "and ", "or ", "limit",
                "group", "order", "as ", "count","drop", "alter","truncate", "declare", "where", "exec", "--", "procedure", "pg_", "lock",
                "unlock","write", "engine", "null","not ","mode", "set ",";"};


        for (String evilWord : evilWords) {

            final String s = SQLUtil.sanitizeParameter("xxx " + evilWord);

            assertNotNull(s);
            assertEquals(StringPool.BLANK, s);
        }
    }


    @Test()
    public void testInValidWordsAfterPaddingSanitizeParameter() throws Exception {

        final String [] evilWords = {"select", "insert", "delete", "update", "replace", "create", "distinct", "like", "and ", "or ", "limit",
                "group", "order", "as ", "count","drop", "alter","truncate", "declare", "where", "exec", "--", "procedure", "pg_", "lock",
                "unlock","write", "engine", "null","not ","mode", "set ",";"};


        for (String evilWord : evilWords) {

            final String s = SQLUtil.sanitizeParameter(evilWord + " xxxx");

            assertNotNull(s);
            assertEquals(StringPool.BLANK, s);
        }
    }


    @Test()
    public void testInValidWordsBothPaddingSanitizeParameter() throws Exception {

        final String [] evilWords = {"select", "insert", "delete", "update", "replace", "create", "distinct", "like", "and ", "or ", "limit",
                "group", "order", "as ", "count","drop", "alter","truncate", "declare", "where", "exec", "--", "procedure", "pg_", "lock",
                "unlock","write", "engine", "null","not ","mode", "set ",";"};


        for (String evilWord : evilWords) {

            final String s = SQLUtil.sanitizeParameter("xxx " + evilWord + " xxxx");

            assertNotNull(s);
            assertEquals(StringPool.BLANK, s);
        }
    }

    @Test()
    public void testInvalidInsertSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx insert xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }


    @Test()
    public void testValidInsertSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx inserta xxx");

        assertNotNull(s);
        assertEquals("xxx inserta xxx", s);
    }

    @Test()
    public void testInvalidDeleteSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx delete xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }


    @Test()
    public void testValidDeleteSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx deletee xxx");

        assertNotNull(s);
        assertEquals("xxx deletee xxx", s);
    }

    @Test()
    public void testInvalidUpdateSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx update xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidUpdate2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx update/**/ xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }


    @Test()
    public void testValidUpdateSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx updated xxx");

        assertNotNull(s);
        assertEquals("xxx updated xxx", s);
    }

    @Test()
    public void testValidUpdate2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx update1 xxx");

        assertNotNull(s);
        assertEquals("xxx update1 xxx", s);
    }

    @Test()
    public void testInvalidLikeSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx like%crack% xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidLike2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx like xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidLike3SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx like %crack% xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }


    @Test()
    public void testValidLikeSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx liked xxx");

        assertNotNull(s);
        assertEquals("xxx liked xxx", s);
    }

    @Test()
    public void testValidLike2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx nlike xxx");

        assertNotNull(s);
        assertEquals("xxx nlike xxx", s);
    }

    @Test()
    public void testInvalidOrSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx or/**/ xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testInvalidOr2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx or xxx");

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }


    @Test()
    public void testValidOrSanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx orx xxx");

        assertNotNull(s);
        assertEquals("xxx orx xxx", s);
    }

    @Test()
    public void testValidOr2SanitizeParameter() throws Exception {


        final String s = SQLUtil.sanitizeParameter("xxx nor xxx");

        assertNotNull(s);
        assertEquals("xxx nor xxx", s);
    }

    @Test()
    public void testValidCondition() throws Exception {

        final String query = "structuretype = 1";
        final String s = SQLUtil.sanitizeCondition( query );

        assertNotNull(s);
        assertEquals(query, s);
    }

    @Test()
    public void testInvalidCondition() throws Exception {

        final String query = "and if(length(user())>0,sleep(10),2)";
        final String s = SQLUtil.sanitizeCondition( query );

        assertNotNull(s);
        assertEquals(StringPool.BLANK, s);
    }

    @Test()
    public void testEscapeSqlwithSingleQuote() throws Exception {

        final String querySingleQuote = "velocity_var_name 'velocityVarNameTesting'";
        final String queryDoubleQuote = "velocity_var_name ''velocityVarNameTesting''";
        final String s = SQLUtil.sanitizeParameter( querySingleQuote );

        assertNotNull(s);
        assertEquals(queryDoubleQuote, s);
    }

    @Test()
    public void testDontEscapeSqlwithSingleQuote() throws Exception {

        final String querySingleQuote = "velocity_var_name like 'velocityVarNameTesting'";
        final String s = SQLUtil.sanitizeCondition( querySingleQuote );

        assertNotNull(s);
        assertEquals(querySingleQuote, s);
    }

}