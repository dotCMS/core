package com.dotmarketing.common.util;

import com.dotcms.UnitTestBase;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.util.SecurityLoggerServiceAPIFactory;
import com.dotcms.util.pagination.OrderDirection;
import com.liferay.util.StringPool;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * This Unit Test verifies that the {@link SQLUtil} class works as expected.
 *
 * @author Jonathan Sanchez
 * @since Jan 4th, 2017
 */
public class SQLUtilTest extends UnitTestBase {

    protected static SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

    public final static String MALICIOUS_SQL_CONDITION = "; SeleCt pg_sleep(200)";
    
    
    public final static String MALICIOUS_SQL_ORDER_BY = "; SeleCt pg_sleep(200)";
    
    
    
    
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

    private static final String exploit1 = "\\') rlike (select/**/(case/**/when/**/((substring((select/**/password_/**/from/**/dotcms37new.user_/**/limit/**/1,1),1,1)/**/like/**/binary/**/\"1\"))/**/then/**/1/**/else/**/0x28/**/end))#";

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

    @Test
    public void Test_TranslateSortBy_UIFieldsListParameter_ShouldReturnConvertedList() {
       final List<String> input = List.of("moddate", "categoryname", "categoryvelocityvarname",
                "categorykey", "pageurl", "velocityvarname", "sortorder", "hostname",
                "relationtypevalue", "childrelationname", "parentrelationname");

       final List<String> expected = List.of("mod_date", "category_name", "category_velocity_var_name",
                "category_key", "page_url", "velocity_var_name", "sort_order", "hostName",
                "relation_type_value", "child_relation_name", "parent_relation_name");

        for (int i = 0; i < input.size(); i++) {
           final String result = SQLUtil.translateSortBy(input.get(i));
            assertEquals(expected.get(i), result);
        }
    }

    @Test
    public void Test_SanitizeSortBy_BlankOrNullAsStringParameter_ShouldReturnBlankString() {
        final String blankStringOutput = SQLUtil.sanitizeSortBy(StringPool.BLANK);
        final String nullStringOutput = SQLUtil.sanitizeSortBy("null");

        assertEquals(StringPool.BLANK, blankStringOutput);
        assertEquals(StringPool.BLANK, nullStringOutput);
    }

    @Test
    public void Test_SanitizeSortBy_ModDateWithHyphenOrDescAsStringParameter_ShouldReturnHyphenOrDescWithModDate() {
        final String hyphenStringOutput = SQLUtil.sanitizeSortBy("-modDate");
        final String descStringOutput = SQLUtil.sanitizeSortBy("modDate desc");

        assertEquals("-mod_date", hyphenStringOutput);
        assertEquals("mod_date desc", descStringOutput);
    }

    @Test
    public void Test_SanitizeSortBy_ModDateAsStringParameter_ShouldReturnModDate() {
        final String Output = SQLUtil.sanitizeSortBy("modDate");

        assertEquals("mod_date", Output);
    }

    @Test
    public void Test_SanitizeSortBy_InvalidSortByFieldAsStringParameter_ShouldReturnBlankString() {
        final String Output = SQLUtil.sanitizeSortBy("invalidColumn");

        assertEquals(StringPool.BLANK, Output);
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link SQLUtil#getOrderByAndDirectionSql(String, OrderDirection)}</li>
     *     <li><b>Given Scenario: </b>Checks that the order-by and direction statements are
     *     appended correctly, and with the expected fallbacks as well.</li>
     *     <li><b>Expected Result: </b>If the order-by is specified, use it and the direction to
     *     form the SQL statement. If it isn't, then fall back to mod_date in DESC order.</li>
     * </ul>
     */
    @Test
    public void testGetOrderByAndDirectionSql() {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String orderByClause = "upper(name)";
        final String expectedTestOne = "upper(name) asc";
        final String expectedTestTwo = "upper(name) desc";
        final String expectedTestThree = "mod_date desc";

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final String testResultOne = SQLUtil.getOrderByAndDirectionSql(orderByClause,
                OrderDirection.ASC);
        final String testResultTwo = SQLUtil.getOrderByAndDirectionSql(orderByClause,
                OrderDirection.DESC);
        final String testResultThree = SQLUtil.getOrderByAndDirectionSql("", OrderDirection.DESC);

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertEquals("Result of test one is not valid", expectedTestOne, testResultOne);
        assertEquals("Result of test two is not valid", expectedTestTwo, testResultTwo);
        assertEquals("Result of test three is not valid", expectedTestThree, testResultThree);
    }

}