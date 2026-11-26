package com.dotcms.rest.api.v1.drive.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.rest.api.v1.drive.export.ExportFileNames.TypeName;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link ExportFileNames}: the CSV and zip names, how they are made safe and kept
 * distinct, and the {@code Content-Disposition} header (research R10, FR-016, FR-019).
 */
public class ExportFileNamesTest {

    /** November 26, 2026: {@code M_d_yyyy} gives {@code 11_26_2026}, with no zero padding. */
    private static final Date DATE = new GregorianCalendar(2026, Calendar.NOVEMBER, 26).getTime();
    /** March 5, 2026, to prove month and day are not zero-padded. */
    private static final Date SINGLE_DIGIT_DATE =
            new GregorianCalendar(2026, Calendar.MARCH, 5).getTime();

    /**
     * Method to test: {@link ExportFileNames#csvName(String, String, Date)} and
     * {@link ExportFileNames#zipName(String, Date)}
     * Given Scenario: A plain content type name and a site host name
     * ExpectedResult: The Content Search CSV name and the selection zip name, dated {@code M_d_yyyy}
     */
    @Test
    public void test_names_followTheContentSearchPattern() {
        assertEquals("Blog_contents_11_26_2026.csv", ExportFileNames.csvName("Blog", "blog", DATE));
        assertEquals("demo.dotcms.com_export_11_26_2026.zip",
                ExportFileNames.zipName("demo.dotcms.com", DATE));
        assertEquals("Blog_contents_3_5_2026.csv",
                ExportFileNames.csvName("Blog", "blog", SINGLE_DIGIT_DATE));
    }

    /**
     * Method to test: {@link ExportFileNames#csvName(String, String, Date)}
     * Given Scenario: Names holding each character that is unsafe in a file name, a zip entry or a
     *                 header: {@code / \ : * ? " < > |}, CR, LF and another control character
     * ExpectedResult: Each becomes {@code _}; nothing unsafe survives
     */
    @Test
    public void test_csvName_replacesUnsafeCharacters() {
        assertEquals("A_B_ C_contents_11_26_2026.csv",
                ExportFileNames.csvName("A/B: C", "abc", DATE));
        assertEquals("a_b_c_d_e_f_g_h_i_contents_11_26_2026.csv",
                ExportFileNames.csvName("a\\b*c?d\"e<f>g|h:i", "x", DATE));
        assertEquals("tab_separated_contents_11_26_2026.csv",
                ExportFileNames.csvName("tab\tseparated", "x", DATE));

        final String withControls = ExportFileNames.csvName("bad\r\nname\u0007", "x", DATE);
        assertFalse(withControls.contains("\r"));
        assertFalse(withControls.contains("\n"));
        assertFalse(withControls.contains("\u0007"));
        assertEquals("bad__name__contents_11_26_2026.csv", withControls);
    }

    /**
     * Method to test: {@link ExportFileNames#csvName(String, String, Date)}
     * Given Scenario: Names with leading and trailing dots and spaces, a name with nothing left after
     *                 that, and a name with non-ASCII letters
     * ExpectedResult: Leading and trailing dots and spaces are trimmed; an empty result falls back to
     * the content type variable; Unicode letters are kept
     */
    @Test
    public void test_csvName_trimsAndFallsBack_andKeepsUnicode() {
        assertEquals("News_contents_11_26_2026.csv",
                ExportFileNames.csvName(" ..News.. ", "news", DATE));
        assertEquals("pressRelease_contents_11_26_2026.csv",
                ExportFileNames.csvName(" . . ", "pressRelease", DATE));
        assertEquals("Artículo_contents_11_26_2026.csv",
                ExportFileNames.csvName("Artículo", "articulo", DATE));
    }

    /**
     * Method to test: {@link ExportFileNames#zipName(String, Date)}
     * Given Scenario: A host name with a character unsafe for a file name
     * ExpectedResult: It is replaced the same way as in CSV names
     */
    @Test
    public void test_zipName_isMadeSafe() {
        assertEquals("my_site_export_11_26_2026.zip", ExportFileNames.zipName("my/site", DATE));
    }

    /**
     * Method to test: {@link ExportFileNames#csvNames(List, Date)}
     * Given Scenario: Three content types sent out of name order, two of them with the same name
     *                 and a third that becomes the same name once made safe
     * ExpectedResult: Names are assigned in content type name order; the first keeps the plain name,
     * the next ones get {@code -2} and {@code -3} before {@code .csv}; the map iterates in that order
     */
    @Test
    public void test_csvNames_resolveCollisionsInNameOrder() {
        final List<TypeName> types = List.of(
                new TypeName("id-zeta", "Zeta", "zeta"),
                new TypeName("id-blog-2", "Blog", "blog2"),
                new TypeName("id-blog-1", "Blog", "blog1"),
                new TypeName("id-alpha", "Alpha", "alpha"));

        final Map<String, String> names = ExportFileNames.csvNames(types, DATE);

        assertEquals(4, names.size());
        assertEquals("Alpha_contents_11_26_2026.csv", names.get("id-alpha"));
        assertEquals("Zeta_contents_11_26_2026.csv", names.get("id-zeta"));

        final List<String> blogNames = List.of(names.get("id-blog-1"), names.get("id-blog-2"));
        assertTrue(blogNames.contains("Blog_contents_11_26_2026.csv"));
        assertTrue(blogNames.contains("Blog_contents_11_26_2026-2.csv"));

        final List<String> order = new ArrayList<>(names.values());
        assertEquals("Alpha_contents_11_26_2026.csv", order.get(0));
        assertTrue(order.get(1).startsWith("Blog_contents_"));
        assertTrue(order.get(2).startsWith("Blog_contents_"));
        assertEquals("Zeta_contents_11_26_2026.csv", order.get(3));
    }

    /**
     * Method to test: {@link ExportFileNames#csvNames(List, Date)}
     * Given Scenario: Two names that differ before being made safe but are equal after
     *                 ({@code A/B} and {@code A:B})
     * ExpectedResult: Both CSVs are kept, the second with {@code -2}
     */
    @Test
    public void test_csvNames_collisionAfterSanitizing() {
        final Map<String, String> names = ExportFileNames.csvNames(List.of(
                new TypeName("one", "A/B", "ab1"), new TypeName("two", "A:B", "ab2")), DATE);

        assertEquals(List.of("A_B_contents_11_26_2026.csv", "A_B_contents_11_26_2026-2.csv"),
                new ArrayList<>(names.values()));
    }

    /**
     * Method to test: {@link ExportFileNames#contentDisposition(String)}
     * Given Scenario: An ASCII name and a name with non-ASCII letters
     * ExpectedResult: {@code attachment; filename="<ASCII, non-ASCII as _>"; filename*=UTF-8''<percent-encoded>}
     */
    @Test
    public void test_contentDisposition_hasAsciiFallbackAndEncodedName() {
        assertEquals("attachment; filename=\"Blog_contents_11_26_2026.csv\"; "
                        + "filename*=UTF-8''Blog_contents_11_26_2026.csv",
                ExportFileNames.contentDisposition("Blog_contents_11_26_2026.csv"));

        assertEquals("attachment; filename=\"Art_culo_contents_11_26_2026.csv\"; "
                        + "filename*=UTF-8''Art%C3%ADculo_contents_11_26_2026.csv",
                ExportFileNames.contentDisposition("Artículo_contents_11_26_2026.csv"));
    }

    /**
     * Method to test: {@link ExportFileNames#contentDisposition(String)}
     * Given Scenario: A name with a space and a percent sign
     * ExpectedResult: The {@code filename*} part percent-encodes them ({@code %20}, {@code %25}), and
     * never uses {@code +} for a space
     */
    @Test
    public void test_contentDisposition_encodesSpacesAndPercent() {
        final String header = ExportFileNames.contentDisposition("A B 50%_contents_11_26_2026.csv");

        assertTrue(header.contains("filename=\"A B 50%_contents_11_26_2026.csv\""));
        assertTrue(header.endsWith("filename*=UTF-8''A%20B%2050%25_contents_11_26_2026.csv"));
    }
}
