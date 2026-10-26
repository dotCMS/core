package com.dotcms.security.multipart;

import static org.junit.Assert.assertThrows;

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
        for (final String name : new String[] {"shell.jsp", "shell.JSP", "shell.Jsp", "tool.EXE", "run.Sh"}) {
            assertThrows(name, IllegalArgumentException.class, () -> validator.validate(name));
        }
    }

    /**
     * The other JSP file types that the servlet container compiles are in the default set.
     */
    @Test
    public void jspFamily_isRefusedByDefault() {
        for (final String name : new String[] {"page.jspx", "fragment.jspf", "PAGE.JSPX", "FRAGMENT.JSPF"}) {
            assertThrows(name, IllegalArgumentException.class, () -> validator.validate(name));
        }
    }

    /**
     * Ordinary names, including ones whose earlier dotted part looks like a listed extension,
     * are accepted; only the final extension counts.
     */
    @Test
    public void unlistedExtension_isAccepted() {
        for (final String name : new String[] {"report.txt", "archive.tar.gz", "shell.jsp.txt", "README", "photo.JPG"}) {
            validator.validate(name);
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
