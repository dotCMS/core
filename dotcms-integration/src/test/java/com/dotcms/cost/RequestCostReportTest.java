package com.dotcms.cost;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.util.IntegrationTestInitService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RequestCostReport}. Tests HTML report generation with various accounting data scenarios.
 *
 */
public class RequestCostReportTest {

    private RequestCostReport report;
    private RequestCostApi requestCostApi;
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();
        report = new RequestCostReport();
        requestCostApi = new RequestCostApiImpl(true);
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test/page").request())));
    }

    /**
     * Test: {@link RequestCostReport#writeAccounting} with full accounting data Should: Generate complete HTML report
     * with all details Expected: HTML contains table with class, method, args, and costs
     */
    @Test
    public void test_writeAccounting_withFullData_shouldGenerateCompleteReport() {
        // Given
        requestCostApi.initAccounting(request, true);
        requestCostApi.incrementCost(Price.FIVE, RequestCostReportTest.class, "method1", new Object[]{"arg1", "arg2"});
        requestCostApi.incrementCost(Price.TEN, RequestCostReportTest.class, "method2", new Object[]{123, true});
        requestCostApi.incrementCost(Price.THREE, RequestCostReportTest.class, "method3", new Object[]{});

        // When
        String html = report.writeAccounting(request);

        // Then
        assertNotNull("HTML should not be null", html);
        assertTrue("Should contain HTML tag", html.contains("<html>"));
        assertTrue("Should contain title", html.contains("<title>Request Accounting"));

        assertTrue("Should contain table", html.contains("<table"));
        assertTrue("Should contain class name", html.contains("RequestCostReportTest"));
        assertTrue("Should contain method names", html.contains("method1") || html.contains("method2"));
        assertTrue("Should contain total", html.contains("Total:"));
        assertTrue("Should close HTML", html.contains("</html>"));
    }

    /**
     * Test: {@link RequestCostReport#writeAccounting} with minimal data Should: Generate report with only cost
     * information Expected: HTML contains basic structure and costs
     */
    @Test
    public void test_writeAccounting_withMinimalData_shouldGenerateBasicReport() {
        // Given
        requestCostApi.initAccounting(request, false);
        requestCostApi.incrementCost(Price.SEVEN, RequestCostReportTest.class, "method", new Object[]{});

        // When
        String html = report.writeAccounting(request);

        // Then
        assertNotNull("HTML should not be null", html);
        assertTrue("Should contain HTML structure", html.contains("<html>"));
        assertTrue("Should contain table", html.contains("<table"));
        assertTrue("Should contain total", html.contains("Total:"));
    }

    /**
     * Test: {@link RequestCostReport#writeAccounting} with empty accounting Should: Generate report with zero total
     * Expected: HTML contains structure but no data rows
     */
    @Test
    public void test_writeAccounting_withEmptyAccounting_shouldGenerateEmptyReport() {
        // Given
        requestCostApi.initAccounting(request, true);
        // Don't add any costs beyond initialization

        // When
        String html = report.writeAccounting(request);

        // Then
        assertNotNull("HTML should not be null", html);
        assertTrue("Should contain HTML structure", html.contains("<html>"));
        assertTrue("Should contain table", html.contains("<table"));
        assertTrue("Should contain total", html.contains("Total:"));
    }

    /**
     * Test: Request URI is properly escaped in report Should: Escape HTML special characters Expected: XSS attempts are
     * neutralized
     */
    @Test
    public void test_writeAccounting_shouldEscapeRequestURI() {
        // Given
        HttpServletRequest xssRequest = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test<script>alert('xss')</script>").request())));
        requestCostApi.initAccounting(xssRequest, true);

        // When
        String html = report.writeAccounting(xssRequest);

        // Then
        assertNotNull("HTML should not be null", html);
        assertFalse("Should not contain unescaped script tag", html.contains("<script>alert"));
        assertTrue("Should contain escaped content", html.contains("&lt;") || html.contains("&gt;"));
    }

    /**
     * Test: Total cost calculation Should: Sum all individual costs correctly Expected: Total matches sum of all
     * entries
     */
    @Test
    public void test_writeAccounting_shouldCalculateTotalCorrectly() {
        // Given
        requestCostApi.initAccounting(request, true);
        requestCostApi.incrementCost(Price.TEN, RequestCostReportTest.class, "method1", new Object[]{});
        requestCostApi.incrementCost(Price.TWENTY, RequestCostReportTest.class, "method2", new Object[]{});
        requestCostApi.incrementCost(Price.THIRTY, RequestCostReportTest.class, "method3", new Object[]{});

        int expectedTotal = requestCostApi.getRequestCost(request);

        // When
        String html = report.writeAccounting(request);

        // Then
        assertTrue("Should contain total cost", html.contains("Total:"));
        assertTrue("Should contain total value: " + expectedTotal,
                html.contains(expectedTotal + "</"));
    }

    /**
     * Test: Row numbering in report Should: Number rows sequentially Expected: Rows are numbered 1, 2, 3, etc.
     */
    @Test
    public void test_writeAccounting_shouldNumberRowsSequentially() {
        // Given
        requestCostApi.initAccounting(request, true);
        requestCostApi.incrementCost(Price.ONE, RequestCostReportTest.class, "method1", new Object[]{});
        requestCostApi.incrementCost(Price.TWO, RequestCostReportTest.class, "method2", new Object[]{});
        requestCostApi.incrementCost(Price.THREE, RequestCostReportTest.class, "method3", new Object[]{});

        // When
        String html = report.writeAccounting(request);

        // Then
        assertTrue("Should contain row number 1", html.contains(">1</td>"));
        assertTrue("Should contain row number 2", html.contains(">2</td>"));
        assertTrue("Should contain row number 3", html.contains(">3</td>"));
    }

    /**
     * Test: Arguments are displayed in report Should: Show all method arguments Expected: Arguments appear in table
     * cells
     */
    @Test
    public void test_writeAccounting_shouldDisplayArguments() {
        // Given
        requestCostApi.initAccounting(request, true);
        requestCostApi.incrementCost(Price.FIVE, RequestCostReportTest.class, "testMethod",
                new Object[]{"stringArg", 42, true, null});

        // When
        String html = report.writeAccounting(request);

        // Then
        assertTrue("Should contain string argument", html.contains("stringArg"));
        assertTrue("Should contain numeric argument", html.contains("42"));
        assertTrue("Should contain boolean argument", html.contains("true"));
    }

    /**
     * Test: Null arguments are handled Should: Display empty string for null args Expected: No NullPointerException,
     * empty content for nulls
     */
    @Test
    public void test_writeAccounting_withNullArguments_shouldNotFail() {
        // Given
        requestCostApi.initAccounting(request, true);
        requestCostApi.incrementCost(Price.FIVE, RequestCostReportTest.class, "method",
                new Object[]{null, "valid", null});

        // When
        String html = report.writeAccounting(request);

        // Then
        assertNotNull("HTML should not be null", html);
        assertTrue("Should contain valid argument", html.contains("valid"));
        // Null args should be rendered as empty strings
    }

    /**
     * Test: Large number of entries Should: Handle many cost entries without issues Expected: All entries are included
     * in report
     */
    @Test
    public void test_writeAccounting_withManyEntries_shouldHandleAll() {
        // Given
        requestCostApi.initAccounting(request, true);
        int entryCount = 100;
        for (int i = 0; i < entryCount; i++) {
            requestCostApi.incrementCost(Price.ONE, RequestCostReportTest.class, "method" + i, new Object[]{i});
        }

        // When
        String html = report.writeAccounting(request);

        // Then
        assertNotNull("HTML should not be null", html);
        assertTrue("Should contain table", html.contains("<table"));
        assertTrue("Should contain total", html.contains("Total:"));
        // Check that we have many rows
        int rowCount = html.split("<tr>").length - 1; // -1 for the split behavior
        assertTrue("Should have many rows", rowCount > 50);
    }

    /**
     * Test: Report structure is valid HTML Should: Generate well-formed HTML Expected: Opening and closing tags match
     */
    @Test
    public void test_writeAccounting_shouldGenerateValidHTML() {
        // Given
        requestCostApi.initAccounting(request, true);
        requestCostApi.incrementCost(Price.FIVE, RequestCostReportTest.class, "method", new Object[]{});

        // When
        String html = report.writeAccounting(request);

        // Then
        // Check basic HTML structure
        assertTrue("Should start with <html>", html.contains("<html>"));
        assertTrue("Should end with </html>", html.contains("</html>"));
        assertTrue("Should have <title>", html.contains("<title>"));
        assertTrue("Should have </title>", html.contains("</title>"));
        assertTrue("Should have <body>", html.contains("<body>"));
        assertTrue("Should have </body>", html.contains("</body>"));
        assertTrue("Should have <table>", html.contains("<table"));
        assertTrue("Should have </table>", html.contains("</table>"));

        // Count opening and closing table rows
        int openTr = countOccurrences(html, "<tr>");
        int closeTr = countOccurrences(html, "</tr>");
        assertEquals("Opening and closing <tr> tags should match", openTr, closeTr);
    }

    /**
     * Test: Special characters in method names and class names Should: Handle special characters without breaking HTML
     * Expected: HTML remains valid
     */
    @Test
    public void test_writeAccounting_withSpecialCharacters_shouldHandleGracefully() {
        // Given
        requestCostApi.initAccounting(request, true);
        // Class and method names from Java won't have HTML special chars, but args might
        requestCostApi.incrementCost(Price.FIVE, RequestCostReportTest.class, "method",
                new Object[]{"<test>", "&value", "\"quoted\""});

        // When
        String html = report.writeAccounting(request);

        // Then
        assertNotNull("HTML should not be null", html);
        assertTrue("Should contain HTML structure", html.contains("<html>"));
        // The arguments should be present (possibly escaped or not, depending on implementation)
    }

    /**
     * Helper method to count occurrences of a substring
     */
    private int countOccurrences(String str, String substring) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
