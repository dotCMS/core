package com.dotcms.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotmarketing.exception.DotDataValidationException;
import com.dotcms.util.YamlUtil;
import java.io.File;
import java.util.List;
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
        assertFalse("The shipped filter must not set relationshipsDepth (it is only documented in comments)",
                filterDescriptor.getFilters().containsKey(FilterDescriptor.RELATIONSHIPS_DEPTH_KEY));
    }

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the filters map declares {@code relationshipsDepth} as a positive whole number, either
     * as an {@code Integer} or as a {@code Long} (YAML parsers can produce either)
     * Should: validate without throwing (issue #38394)
     */
    @Test
    public void validate_accepts_relationshipsDepth_positive_integer() throws DotDataValidationException {
        new FilterDescriptor("test.yml", TITLE, Map.of("relationshipsDepth", 3), false, ROLES).validate();
        new FilterDescriptor("test.yml", TITLE, Map.of("relationshipsDepth", 3L), false, ROLES).validate();
    }

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the filters map declares {@code relationshipsDepth: 0}, the "unlimited" value
     * Should: validate without throwing
     */
    @Test
    public void validate_accepts_relationshipsDepth_unlimited() throws DotDataValidationException {
        new FilterDescriptor("test.yml", TITLE,
                Map.of("relationshipsDepth", FilterDescriptor.RELATIONSHIPS_DEPTH_UNLIMITED), false, ROLES)
                .validate();
    }

    /**
     * Method to test: {@link FilterDescriptor#validate()}
     * When: the filters map declares {@code relationshipsDepth} as a negative number, a numeric
     * string, a decimal, a boolean, or a number larger than {@link Integer#MAX_VALUE}
     * Should: throw {@link DotDataValidationException} naming the field, for every one of them
     */
    @Test
    public void validate_rejects_relationshipsDepth_invalid_values() {
        for (final Object invalid : List.of(-1, "3", 2.5, true, (long) Integer.MAX_VALUE + 1)) {
            final FilterDescriptor filterDescriptor = new FilterDescriptor(
                    "test.yml", TITLE, Map.of("relationshipsDepth", invalid), false, ROLES);
            try {
                filterDescriptor.validate();
                fail("relationshipsDepth=" + invalid + " (" + invalid.getClass().getSimpleName()
                        + ") should have been rejected");
            } catch (final DotDataValidationException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("relationshipsDepth"));
            }
        }
    }
}
