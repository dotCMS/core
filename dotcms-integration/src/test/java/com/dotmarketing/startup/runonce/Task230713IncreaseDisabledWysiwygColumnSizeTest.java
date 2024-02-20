package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Verifies that the Upgrade Task {@link Task230713IncreaseDisabledWysiwygColumnSize} is working as expected.
 *
 * @author Jose Castro
 * @since Jul 13th, 2023
 */
public class Task230713IncreaseDisabledWysiwygColumnSizeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link Task230713IncreaseDisabledWysiwygColumnSize#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Verifies that the {@code contentlet.disabled_wysiwyg} has a size of 1000 now.</li>
     *     <li><b>Expected Result: </b>The column size must be the expected one.</li>
     * </ul>
     */
    @Test
    public void executeTaskUpgrade() throws DotDataException, SQLException {
        final Task230713IncreaseDisabledWysiwygColumnSize task = new Task230713IncreaseDisabledWysiwygColumnSize();
        task.executeUpgrade();
        final Map<String, String> columnData = new DotDatabaseMetaData().getModifiedColumnLength("contentlet",
                "disabled_wysiwyg");
        assertEquals("Column 'contentlet.disabled_wysiwyg' must have a length of 1000, Column 'contentlet" +
                ".disabled_wysiwyg' must have a length of 1000", "1000", columnData.get("field_length"));
    }

}
