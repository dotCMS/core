package com.dotmarketing.util;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.TestDataUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Maintenance Utilities integration tests.
 *
 * @author victor
 */
public class MaintenanceUtilTest extends IntegrationTestBase {
    /**
     * Method to test: DBSearchAndReplace
     * Given Scenario: Given a Rich Text contentlet with non latin characters, in this case Russian.
     * ExpectedResult: after replacing text for contentlets the non latin character should remain.
     */
    @Test
    public void test_nonLatinCharactersRemainAfter_DBSearchAndReplace() {
        final String bodyWithRussian = "This is some Russian: ижф";
        final Contentlet persisted = TestDataUtils.getRichTextContent(
                true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                bodyWithRussian);
        Optional<Contentlet> fetched = APILocator.getContentletAPI().findInDb(persisted.getInode());
        assertTrue(fetched.isPresent());
        assertEquals(bodyWithRussian, fetched.get().getStringProperty("body"));

        MaintenanceUtil.DBSearchAndReplace("intelligent", "smart");
        fetched = APILocator.getContentletAPI().findInDb(persisted.getInode());
        assertTrue(fetched.isPresent());
        assertEquals(bodyWithRussian, fetched.get().getStringProperty("body"));
    }
}
