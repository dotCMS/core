package com.dotcms.security.multipart;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Config;
import java.util.Set;
import org.junit.Test;

/**
 * The file-extension check applied to uploaded file names. The configured extensions are
 * compared without regard to letter case, and the default set covers the JSP family.
 */
public class IllegalFileExtensionsValidatorTest {

    private final IllegalFileExtensionsValidator validator = new IllegalFileExtensionsValidator();

    /**
     * A listed extension is refused whatever its letter case.
     */
    @Test
    public void listedExtension_anyCase_isRefused() {
        for (final String name : new String[] {"sample-1.jsp", "sample-2.JSP", "sample-3.Jsp", "sample-4.EXE", "sample-5.Sh"}) {
            final IllegalArgumentException refusal =
                    assertThrows(name, IllegalArgumentException.class, () -> validator.validate(name));
            assertTrue("the message names the file so per-item callers can report it",
                    refusal.getMessage().contains(name));
        }
    }

    /**
     * The other JSP file types that the servlet container compiles are in the default set.
     */
    @Test
    public void jspFamily_isRefusedByDefault() {
        for (final String name : new String[] {"sample-6.jspx", "sample-7.jspf", "SAMPLE-8.JSPX", "SAMPLE-9.JSPF"}) {
            assertThrows(name, IllegalArgumentException.class, () -> validator.validate(name));
        }
    }

    /**
     * Ordinary names, including ones whose earlier dotted part looks like a listed extension,
     * are accepted; only the final extension counts.
     */
    @Test
    public void unlistedExtension_isAccepted() {
        for (final String name : new String[] {"report.txt", "archive.tar.gz", "sample-1.jsp.txt", "README", "photo.JPG"}) {
            validator.validate(name);
        }
    }

    /**
     * Dots and spaces at the end of a name are dropped by some file systems, so the name is judged
     * without them: the extension is the one the stored file will have.
     */
    @Test
    public void listedExtension_withTrailingDotsOrSpaces_isRefused() {
        for (final String name : new String[] {"sample-1.jsp.", "sample-2.JSP.", "sample-3.jsp ", "sample-4.Jsp. .", "sample-5.exe..."}) {
            assertThrows(name, IllegalArgumentException.class, () -> validator.validate(name));
        }
    }

    /**
     * Trailing dots and spaces on an unlisted name do not make it refused, and a name made only of
     * them has no extension.
     */
    @Test
    public void unlistedExtension_withTrailingDotsOrSpaces_isAccepted() {
        for (final String name : new String[] {"report.txt.", "report.txt ", "...", " "}) {
            validator.validate(name);
        }
    }

    /**
     * Without a configured list the default one applies.
     */
    @Test
    public void toBlockList_withoutConfiguration_usesTheDefaultList() {
        assertTrue(IllegalFileExtensionsValidator.toBlockList(null).contains(".jsp"));
        assertTrue(IllegalFileExtensionsValidator.toBlockList(new String[0]).contains(".exe"));
    }

    /**
     * A configured list replaces the default one; each entry is read without surrounding spaces and
     * without regard to letter case.
     */
    @Test
    public void toBlockList_withConfiguration_replacesTheDefaultList() {
        final Set<String> blocked =
                IllegalFileExtensionsValidator.toBlockList(new String[] {".EXE", " .Php "});
        assertTrue(blocked.contains(".exe"));
        assertTrue(blocked.contains(".php"));
        assertFalse(blocked.contains(".jsp"));
    }

    /**
     * An entry written without its leading dot, or as a glob, names the same extension as one
     * written with it.
     */
    @Test
    public void toBlockList_entriesWithoutLeadingDot_areMatched() {
        final Set<String> blocked =
                IllegalFileExtensionsValidator.toBlockList(new String[] {"jsp", " EXE", "*.bat", "..sh"});
        assertTrue(blocked.contains(".jsp"));
        assertTrue(blocked.contains(".exe"));
        assertTrue(blocked.contains(".bat"));
        assertTrue(blocked.contains(".sh"));
        assertFalse(blocked.contains("jsp"));
    }

    /**
     * A configured value that names no extension (an empty string, spaces, a lone dot) leaves the
     * default list in force instead of blocking nothing.
     */
    @Test
    public void toBlockList_withOnlyBlankEntries_usesTheDefaultList() {
        for (final String[] configured : new String[][] {{""}, {" "}, {"."}, {"", " . ", "*"}}) {
            final Set<String> blocked = IllegalFileExtensionsValidator.toBlockList(configured);
            assertTrue(String.join("|", configured), blocked.contains(".jsp"));
            assertFalse(String.join("|", configured), blocked.contains("."));
        }
    }

    /**
     * The configured list is read at each check: a value set while the application runs applies
     * to the next check on the same validator, and removing it restores the default list.
     */
    @Test
    public void validate_configurationChangedAtRuntime_isAppliedWithoutRestart() {
        final String property = "MULTI_PART_ILLEGAL_FILE_EXTENSIONS";
        validator.validate("before.php");
        assertThrows(IllegalArgumentException.class, () -> validator.validate("before.jsp"));
        try {
            Config.setProperty(property, ".exe,.php");
            assertThrows(IllegalArgumentException.class, () -> validator.validate("configured.php"));
            validator.validate("configured.jsp");

            Config.setProperty(property, null);
            assertThrows(IllegalArgumentException.class, () -> validator.validate("restored.jsp"));
            validator.validate("restored.php");
        } finally {
            Config.setProperty(property, null);
        }
    }

    /**
     * A missing name is refused.
     */
    @Test
    public void nullName_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
    }
}
