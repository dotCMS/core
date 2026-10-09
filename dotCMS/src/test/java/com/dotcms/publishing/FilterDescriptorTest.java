package com.dotcms.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.exception.DotDataValidationException;
import com.dotcms.util.YamlUtil;
import java.io.File;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link FilterDescriptor#validate()}. No DB dependency — exercises only the
 * in-memory validation of a Push Publishing Filter's declared properties.
 */
public class FilterDescriptorTest {

    private static final String TITLE = "Test Filter";
    private static final String ROLES = "DOTCMS_BACK_END_USER";

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the filters map declares {@code relationshipsSecondLevel} as a {@code Boolean}
     * (either {@code true} or {@code false})
     * Should: validate without throwing, since {@code relationshipsSecondLevel} is a recognized
     * filter property
     */
    @Test
    public void validate_accepts_relationshipsSecondLevel_boolean_true() throws DotDataValidationException {
        final FilterDescriptor filterDescriptor = new FilterDescriptor(
                "test.yml", TITLE, Map.of("relationshipsSecondLevel", true), false, ROLES);

        filterDescriptor.validate();
    }

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the filters map declares {@code relationshipsSecondLevel} as {@code Boolean.FALSE}
     * Should: validate without throwing, mirroring the {@code true} case above
     */
    @Test
    public void validate_accepts_relationshipsSecondLevel_boolean_false() throws DotDataValidationException {
        final FilterDescriptor filterDescriptor = new FilterDescriptor(
                "test.yml", TITLE, Map.of("relationshipsSecondLevel", false), false, ROLES);

        filterDescriptor.validate();
    }

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the filters map declares {@code relationshipsSecondLevel} with a non-Boolean value
     * Should: throw {@link DotDataValidationException}, mirroring how {@code dependencies},
     * {@code relationships}, and {@code forcePush} are already validated
     */
    @Test(expected = DotDataValidationException.class)
    public void validate_rejects_relationshipsSecondLevel_non_boolean() throws DotDataValidationException {
        final FilterDescriptor filterDescriptor = new FilterDescriptor(
                "test.yml", TITLE, Map.of("relationshipsSecondLevel", "yes"), false, ROLES);

        filterDescriptor.validate();
    }

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the filters map omits {@code relationshipsSecondLevel} entirely
     * Should: validate without throwing — the key remains optional, matching every other
     * optional filter property
     */
    @Test
    public void validate_accepts_relationshipsSecondLevel_absent() throws DotDataValidationException {
        final FilterDescriptor filterDescriptor = new FilterDescriptor(
                "test.yml", TITLE, Map.of("dependencies", true), false, ROLES);

        filterDescriptor.validate();

        assertEquals(TITLE, filterDescriptor.getTitle());
    }

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the real 'SecondLevelRelationships.yml' filter shipped under
     * dotCMS/src/main/webapp/WEB-INF/publishing-filters/ (added for issue #31400) is parsed with
     * the same {@link YamlUtil} the running server uses to load every Push Publishing Filter
     * Should: parse without error, validate without throwing, and declare
     * relationshipsSecondLevel=true — proving the shipped file itself is well-formed, without
     * depending on PublisherAPIImpl's full init() lifecycle (which has a known, pre-existing,
     * unrelated memoization issue around re-reading the filters directory — see the @Ignore'd
     * PublisherAPIImplTest#testFilterDescriptors)
     */
    @Test
    public void validate_real_second_level_relationships_yml_file_parses() throws DotDataValidationException {
        final File filterFile = new File(
                "src/main/webapp/WEB-INF/publishing-filters/SecondLevelRelationships.yml");
        assertTrue("Expected filter file at " + filterFile.getAbsolutePath(), filterFile.exists());

        final FilterDescriptor filterDescriptor = YamlUtil.parse(filterFile, FilterDescriptor.class);
        filterDescriptor.setKey("SecondLevelRelationships.yml");

        filterDescriptor.validate();

        assertEquals(Boolean.TRUE, filterDescriptor.getFilters().get("relationshipsSecondLevel"));
    }
}
