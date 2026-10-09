package com.dotcms.rest.api.v1.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the license parsing in {@link CompanyConfigHelper}.
 *
 * @author hassandotcms
 */
class CompanyConfigHelperTest {

    /**
     * The header values are read from the license file, trimmed, and the title carries the
     * license version.
     */
    @Test
    void parseLicenseInfo_readsHeaderValuesAndVersion() {
        final String text = "Licensor:             dotCMS LLC\n\n"
                + "Change Date:     Four years from August 01, 2025\n\n"
                + "Change License:  GNU General Public License (GPL) v3\n\n"
                + "Business Source License 1.1\n\nTerms...";

        final LicenseInfoView info = CompanyConfigHelper.parseLicenseInfo(text);

        assertEquals("dotCMS Business Source License 1.1", info.title());
        assertEquals("dotCMS LLC", info.licensor());
        assertEquals("Four years from August 01, 2025", info.changeDate());
        assertEquals("GNU General Public License (GPL) v3", info.changeLicense());
        assertEquals(text, info.text());
    }

    /**
     * When the file can't be read, the fallback sentence still gives a usable response.
     */
    @Test
    void parseLicenseInfo_fallbackText_givesDefaultTitleAndNullFields() {
        final String fallback = "Please see the LICENSE file in the root directory of the dotCMS "
                + "git hub repo: https://github.com/dotCMS/core/blob/main/LICENSE";

        final LicenseInfoView info = CompanyConfigHelper.parseLicenseInfo(fallback);

        assertEquals("dotCMS Business Source License", info.title());
        assertNull(info.licensor());
        assertNull(info.changeDate());
        assertNull(info.changeLicense());
        assertEquals(fallback, info.text());
    }
}
