package com.dotcms.rest.api.v1.drive;

import com.dotcms.browser.ContentStatus;
import com.dotcms.rest.exception.BadRequestException;
import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for how {@link ContentDriveHelper} turns the raw {@code status} strings of a Content
 * Drive search request into {@link ContentStatus} values.
 *
 * <p>The form declares the field as {@code List<String>} rather than the enum on purpose: FR-010
 * asks for a 400 on an unrecognized value "consistent with how {@code userSearchable} rejects
 * unknown keys", and that precedent is an explicit {@link BadRequestException} thrown by the helper.
 * Declaring the enum would instead surface a Jackson deserialization failure, whose mapping to a 400
 * with a useful message is not under this code's control. These tests pin that contract.</p>
 *
 * <p>An <b>empty</b> list is the case that matters most: it is the default and the path every drive
 * search that exists today takes, so it must parse to an empty set and produce no filtering at all
 * (FR-002).</p>
 *
 * <p>Needs no database — parsing is pure.</p>
 */
public class ContentDriveHelperStatusTest {

    /**
     * Given an empty status list, When parsed, Then the result is empty — no status filtering.
     * This is the default path for every existing caller (FR-002).
     */
    @Test
    public void test_parseStatuses_empty_returnsEmptySet() {
        assertTrue("An empty status list must parse to an empty set, so no clause is emitted",
                ContentDriveHelper.parseStatuses(List.of()).isEmpty());
    }

    /**
     * Given each accepted value, When parsed, Then it maps to the matching enum constant.
     */
    @Test
    public void test_parseStatuses_acceptsEveryDeclaredStatus() {
        assertEquals(Set.of(ContentStatus.ARCHIVED),
                ContentDriveHelper.parseStatuses(List.of("ARCHIVED")));
        assertEquals(Set.of(ContentStatus.UNPUBLISHED),
                ContentDriveHelper.parseStatuses(List.of("UNPUBLISHED")));
        assertEquals(Set.of(ContentStatus.LOCKED),
                ContentDriveHelper.parseStatuses(List.of("LOCKED")));
    }

    /**
     * Given several statuses, When parsed, Then all are returned. They are OR'd downstream (FR-006);
     * parsing itself simply collects them.
     */
    @Test
    public void test_parseStatuses_multiple_returnsAll() {
        assertEquals(Set.of(ContentStatus.UNPUBLISHED, ContentStatus.LOCKED),
                ContentDriveHelper.parseStatuses(List.of("UNPUBLISHED", "LOCKED")));
    }

    /**
     * Given lowercase input, When parsed, Then it is accepted — the value is uppercased before the
     * enum lookup, so callers are not forced to match the constant's casing.
     */
    @Test
    public void test_parseStatuses_isCaseInsensitive() {
        assertEquals(Set.of(ContentStatus.ARCHIVED),
                ContentDriveHelper.parseStatuses(List.of("archived")));
    }

    /**
     * Given a Turkish default locale, When a lowercase status is parsed, Then it still resolves.
     *
     * <p>Regression guard: {@code "unpublished".toUpperCase()} under {@code tr-TR} produces
     * {@code "UNPUBLİSHED"} with a dotted capital I, which {@code valueOf} rejects — turning a valid
     * value into a 400 for Turkish-locale servers only. The uppercase must be locale-independent.</p>
     */
    @Test
    public void test_parseStatuses_isLocaleIndependent() {
        final Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals(Set.of(ContentStatus.UNPUBLISHED),
                    ContentDriveHelper.parseStatuses(List.of("unpublished")));
        } finally {
            Locale.setDefault(original);
        }
    }

    /**
     * Given a duplicated status, When parsed, Then it appears once — the result is a Set, so a
     * repeated value cannot widen the generated OR group.
     */
    @Test
    public void test_parseStatuses_collapsesDuplicates() {
        assertEquals(Set.of(ContentStatus.LOCKED),
                ContentDriveHelper.parseStatuses(List.of("LOCKED", "LOCKED")));
    }

    /**
     * Given an unrecognized status, When parsed, Then a {@link BadRequestException} names the
     * accepted values (FR-010).
     *
     * <p>Silently ignoring it would return a <b>wider</b> result set than the caller asked for,
     * which is worse than failing: a request for "DRAFT" would quietly become "no status filter".</p>
     */
    @Test
    public void test_parseStatuses_unknownValue_throwsBadRequest() {
        final BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ContentDriveHelper.parseStatuses(List.of("ARCHIVED", "DRAFT")));

        // getMessage() is WebApplicationException's ("HTTP 400 Bad Request"); the useful text is
        // what actually reaches the client — the error-message header and the JSON entity.
        final String message = exception.getResponse().getHeaderString("error-message");
        assertTrue("The message must name the offending value, got: " + message,
                message.contains("DRAFT"));
        assertTrue("The message must name the accepted values, got: " + message,
                message.contains("ARCHIVED") && message.contains("UNPUBLISHED")
                        && message.contains("LOCKED"));
    }

    /**
     * Given a blank entry, When parsed, Then it is rejected rather than skipped — the same reasoning
     * as an unknown value: quietly dropping it widens the result set.
     */
    @Test
    public void test_parseStatuses_blankValue_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> ContentDriveHelper.parseStatuses(List.of("")));
    }

    /**
     * Given a status containing a percent sign, When parsed, Then it still raises a clean 400.
     *
     * <p>Regression guard: {@code HttpStatusCodeException} runs {@link String#format} over the
     * message it is handed, so building that message by concatenating the user's value would make
     * {@code "50%"} raise UnknownFormatConversionException — surfacing as a 500 instead of a 400.
     * The value must travel as a format argument.</p>
     */
    @Test
    public void test_parseStatuses_valueWithPercent_stillThrowsBadRequest() {
        final BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ContentDriveHelper.parseStatuses(List.of("50%")));

        assertTrue("The offending value must survive into the message",
                exception.getResponse().getHeaderString("error-message").contains("50%"));
    }
}
