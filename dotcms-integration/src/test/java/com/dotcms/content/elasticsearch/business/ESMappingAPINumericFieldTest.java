package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.DOTRAW;
import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.TEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Emission contract for a Text field backed by a numeric storage column — issue #37272.
 *
 * <p>{@code loadFields} picks its serialization branch from the storage column
 * ({@code field_contentlet}) rather than the field type, so a {@code TextField} on an
 * {@code integerN}/{@code floatN} column hands a {@code String} to {@code DecimalFormat.format()},
 * which throws and — because the per-field catch rethrows — aborts {@code toMap} and loses the
 * whole contentlet from the index.</p>
 *
 * <h3>Why these assertions live at the integration layer</h3>
 * <p>The keys that matter do not exist inside {@code loadFields}. It writes
 * {@code <field>_text}; the rename {@code _text} → {@code _dotraw} happens later, in
 * {@code toMap}'s post-processing loop, after {@code loadFields} returns. A {@code loadFields}-only
 * unit test can therefore never observe {@code _dotraw}, which is the key every sort actually
 * targets. See {@code specs/37272-textfield-numeric-column/research.md} R-1.</p>
 *
 * <h3>Phase-awareness</h3>
 * <p>This class is registered in {@code MainSuite1b}, a shard of the weekly Scheduled OpenSearch
 * Phase Sweep, so it also runs under OS Phase 3 where the {@code indicies} table carries no
 * Elasticsearch pointers. It is deliberately <strong>phase-agnostic</strong>: it exercises
 * {@code toMap} only — document construction, no index I/O and no phase routing — and must never
 * call {@code setPhase(...)} nor assume an index name resolves. Getting that wrong reproduces
 * issue #37432.</p>
 *
 * @see ESMappingAPIImpl#toMap(Contentlet)
 */
public class ESMappingAPINumericFieldTest {

    /** The zero-padded form {@code _dotraw} must carry for a numeric field: 19 digits, 18 decimals. */
    private static final Pattern PADDED_NUMERIC = Pattern.compile("^\\d{19}\\.\\d{18}$");

    private static final String NUMERIC_FIELD_VAR = "numericTextField";
    private static final String COMPANION_FIELD_VAR = "companionTextField";
    private static final String COMPANION_VALUE = "untouched";

    private static ESMappingAPIImpl esMappingAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        esMappingAPI = new ESMappingAPIImpl();
        // Keep the default: _text is dropped and only _dotraw survives, which is the shape
        // production indexes actually have.
        Config.setProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", false);
    }

    /**
     * A content type carrying a Text field on the given numeric storage column — the modelling
     * dotCMS allows and its own built-in types use ({@code htmlpageasset.sortOrder} is an
     * {@code ImmutableTextField} with {@code DataTypes.INTEGER}).
     */
    private static ContentType typeWithNumericTextField(final DataTypes dataType) {
        final Field numericField = new FieldDataGen()
                .type(TextField.class)
                .dataType(dataType)
                // FieldDataGen defaults defaultValue to "testDefaultValue<millis>" — a
                // non-numeric String. Left alone it lands in the field on save, the save path
                // indexes, and the defect under test fires during setup. "0" mirrors what
                // dotCMS's own PageContentType does for its INTEGER-backed sortOrder field.
                .defaultValue("0")
                .velocityVarName(NUMERIC_FIELD_VAR)
                .indexed(true)
                .next();
        final Field companionField = new FieldDataGen()
                .type(TextField.class)
                .dataType(DataTypes.TEXT)
                .velocityVarName(COMPANION_FIELD_VAR)
                .indexed(true)
                .next();
        return new ContentTypeDataGen()
                .field(numericField)
                .field(companionField)
                .nextPersisted();
    }

    /**
     * A persisted contentlet that the <em>current</em> code can map.
     *
     * <p>This matters more than it looks. {@code nextPersisted()} saves the contentlet, the save
     * path indexes it (the generator forces {@code IndexPolicy.FORCE}), and indexing calls
     * {@code toMap} — so a non-numeric value in the numeric field triggers the very defect under
     * test <em>during setup</em>, and the test errors before reaching a single assertion. The
     * field's {@code "0"} default keeps the save mappable; the value under test is substituted in
     * memory afterwards, so only the {@code toMap} call in the test body ever sees it.</p>
     */
    private static Contentlet persistedContentlet(final ContentType type) {
        return new ContentletDataGen(type.id())
                .setProperty(COMPANION_FIELD_VAR, COMPANION_VALUE)
                .nextPersisted();
    }

    /**
     * Reproduces the defect's state without persisting bad data:
     * {@code Contentlet.setStringProperty} is a plain {@code map.put} with no coercion, and
     * {@code loadFields} reads the value back through {@code contentlet.get(velocityVarName)}.
     * This is the same shape {@code ImportStarterUtil} leaves in the database — a value whose
     * type contradicts its column, reached without going through the coercing save path.
     */
    private static Contentlet contentletWithStringInNumericField(final ContentType type,
            final String storedValue) {
        final Contentlet contentlet = persistedContentlet(type);
        contentlet.setStringProperty(NUMERIC_FIELD_VAR, storedValue);
        return contentlet;
    }

    private static Contentlet contentletWithNumberInNumericField(final ContentType type,
            final Number storedValue) {
        final Contentlet contentlet = persistedContentlet(type);
        contentlet.setProperty(NUMERIC_FIELD_VAR, storedValue);
        return contentlet;
    }

    private static String numericKey(final ContentType type) {
        return (type.variable() + "." + NUMERIC_FIELD_VAR).toLowerCase();
    }

    // ----------------------------------------------------------------------------------------
    // AC-001 / AC-009 — a numeric value stored as a String indexes, exactly like a native number
    // ----------------------------------------------------------------------------------------

    /**
     * The reported defect. Today {@code toMap} throws
     * {@code IllegalArgumentException: Cannot format given Object as a Number}.
     */
    @Test
    public void test_integerColumn_numericString_isIndexed() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithStringInNumericField(type, "54"));

        assertNotNull("the document must be built, not aborted", map);
        final String key = numericKey(type);
        assertTrue("the numeric key must be present", map.containsKey(key));
        assertEquals("emitted class must match the natively-stored path",
                Long.class, map.get(key).getClass());
        assertEquals(Long.valueOf(54L), map.get(key));
    }

    @Test
    public void test_floatColumn_numericString_isIndexed() {
        final ContentType type = typeWithNumericTextField(DataTypes.FLOAT);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithStringInNumericField(type, "54.3"));

        assertNotNull(map);
        final String key = numericKey(type);
        assertTrue("the float path is in scope too — AC-001 says numeric, not integer",
                map.containsKey(key));
        assertEquals("emitted class must match the natively-stored path",
                Float.class, map.get(key).getClass());
        assertEquals(Float.valueOf(54.3f), map.get(key));
    }

    /**
     * AC-009 stated as a direct comparison rather than a golden value: the same content type, the
     * same value, stored once as a String and once as a number, must emit the same entries.
     */
    @Test
    public void test_convertedString_emitsSameEntriesAsNativeNumber() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final String key = numericKey(type);

        final Map<String, Object> fromString =
                esMappingAPI.toMap(contentletWithStringInNumericField(type, "54"));
        final Map<String, Object> fromNumber =
                esMappingAPI.toMap(contentletWithNumberInNumericField(type, 54L));

        assertEquals("numeric key must be identical, value and class",
                fromNumber.get(key), fromString.get(key));
        assertEquals("_dotraw must be identical, padding included",
                fromNumber.get(key + DOTRAW), fromString.get(key + DOTRAW));
    }

    // ----------------------------------------------------------------------------------------
    // AC-007 — the _dotraw invariant: padded or absent, never raw text, never unpadded
    // ----------------------------------------------------------------------------------------

    /**
     * Pins the invariant on a value that converts. {@code _dotraw} is a {@code keyword} and every
     * sort in dotCMS targets it, so fixed-width zero-padding is the only reason ordering by a
     * numeric field is numeric rather than lexicographic.
     */
    @Test
    public void test_convertedValue_dotrawIsZeroPadded() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithStringInNumericField(type, "54"));

        final Object dotraw = map.get(numericKey(type) + DOTRAW);
        assertNotNull("_dotraw must be emitted for a convertible value", dotraw);
        assertTrue("_dotraw must be zero-padded 19.18, got: " + dotraw,
                PADDED_NUMERIC.matcher(String.valueOf(dotraw)).matches());
        assertEquals("0000000000000000054.000000000000000000", dotraw);
    }

    /**
     * The research R-1 trap, asserted as absence rather than inequality.
     *
     * <p>{@code toMap}'s derivation loop synthesizes {@code <field>_dotraw} from the numeric key
     * whenever {@code <field>_text} is missing — <em>unpadded</em>. So an implementation that omits
     * only {@code _text} while still writing the numeric key produces a malformed {@code _dotraw}
     * and silently reorders every listing sorted by that field. Written as
     * {@code assertNotEquals("n/a", dotraw)} this test would pass against that bug, which is why
     * it asserts the key is not there at all.</p>
     */
    @Test
    public void test_unconvertibleValue_dotrawIsAbsent_notUnpadded() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithStringInNumericField(type, "N/A"));

        final String dotrawKey = numericKey(type) + DOTRAW;
        assertFalse("_dotraw must be absent, not raw text and not an unpadded number — got: "
                + map.get(dotrawKey), map.containsKey(dotrawKey));
    }

    // ----------------------------------------------------------------------------------------
    // AC-008 — an unconvertible value omits both keys; no fabricated 0
    // ----------------------------------------------------------------------------------------

    /**
     * Both keys, in one test, because they are one atomic change: writing the numeric key alone
     * re-creates the unpadded {@code _dotraw} above.
     */
    @Test
    public void test_unconvertibleValue_omitsBothKeys() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithStringInNumericField(type, "N/A"));

        final String key = numericKey(type);
        assertNotNull("the document must still be built", map);
        assertFalse("no fabricated 0 under the numeric key — it would match a range query"
                + " indistinguishably from a genuine 0", map.containsKey(key));
        assertFalse(map.containsKey(key + DOTRAW));
        assertFalse("_text must not survive either", map.containsKey(key + TEXT));
    }

    /** AC-002: the field degrades, the document does not. */
    @Test
    public void test_unconvertibleValue_otherFieldsStillIndexed() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithStringInNumericField(type, "N/A"));

        final String companionKey = (type.variable() + "." + COMPANION_FIELD_VAR).toLowerCase();
        assertTrue("a sibling field must survive one field's failure",
                map.containsKey(companionKey));
        assertEquals(COMPANION_VALUE, map.get(companionKey));
    }

    // ----------------------------------------------------------------------------------------
    // AC-005 — regression: a correctly-stored number must not move at all.
    // These two must be GREEN before the fix; a red result here means the test is wrong.
    // ----------------------------------------------------------------------------------------

    @Test
    public void test_nativeNumber_onIntegerColumn_isUnchanged() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithNumberInNumericField(type, 54L));

        final String key = numericKey(type);
        assertEquals(Long.valueOf(54L), map.get(key));
        assertEquals("0000000000000000054.000000000000000000", map.get(key + DOTRAW));
    }

    @Test
    public void test_nativeNumber_dotrawMatchesThePaddedFormat() {
        final ContentType type = typeWithNumericTextField(DataTypes.FLOAT);
        final Map<String, Object> map =
                esMappingAPI.toMap(contentletWithNumberInNumericField(type, 54.3f));

        final Object dotraw = map.get(numericKey(type) + DOTRAW);
        assertTrue("the padded format is the pre-existing contract: " + dotraw,
                PADDED_NUMERIC.matcher(String.valueOf(dotraw)).matches());
    }

    // ----------------------------------------------------------------------------------------
    // AC-003 / AC-010 — the report is actionable, and the happy path stays silent
    // ----------------------------------------------------------------------------------------

    /**
     * AC-003. One WARN, naming the field and the content type so an operator can act on it —
     * and deliberately <strong>not</strong> naming the value, which is customer content
     * (Constitution Principle III: never log sensitive data).
     */
    @Test
    public void test_unconvertibleValue_logsOneWarnNamingFieldAndType() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Contentlet contentlet = contentletWithStringInNumericField(type, "N/A");

        final List<LogEvent> events = capturingMappingLogs(() -> esMappingAPI.toMap(contentlet));

        final List<LogEvent> warns = eventsAtLevel(events, Level.WARN);
        assertEquals("exactly one WARN for one unconvertible field, got: " + messagesOf(warns),
                1, warns.size());
        final String message = warns.get(0).getMessage().getFormattedMessage();
        assertTrue("the WARN must name the field: " + message,
                message.contains(NUMERIC_FIELD_VAR));
        assertTrue("the WARN must name the content type: " + message,
                message.contains(type.variable()));
        assertFalse("the offending value is customer content and must not be logged: " + message,
                message.contains("N/A"));
    }

    /**
     * AC-010. {@code loadFields} runs for every field of every contentlet on every index write,
     * so a fix that logs on the happy path would flood the log on every page's {@code sortOrder}.
     */
    @Test
    public void test_happyPath_logsNothing() {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);
        final Contentlet nativeNumber = contentletWithNumberInNumericField(type, 54L);
        final Contentlet convertibleString = contentletWithStringInNumericField(type, "54");

        final List<LogEvent> events = capturingMappingLogs(() -> {
            esMappingAPI.toMap(nativeNumber);
            esMappingAPI.toMap(convertibleString);
        });

        final List<LogEvent> noisy = new ArrayList<>(eventsAtLevel(events, Level.WARN));
        noisy.addAll(eventsAtLevel(events, Level.ERROR));
        assertTrue("a native number and a convertible String must both map silently, got: "
                + messagesOf(noisy), noisy.isEmpty());
    }

    // ----------------------------------------------------------------------------------------
    // AC-006 — neighbouring branches of the edited else-if chain are untouched
    // ----------------------------------------------------------------------------------------

    /**
     * The numeric branch sits in an {@code else if} chain with the boolean, date and general
     * text branches. This pins its immediate neighbours: they must keep emitting what they
     * emitted before, both key and runtime class.
     *
     * <p>Category and Relationship fields are covered by {@code ESMappingAPITest} and are
     * resolved in {@code loadCategories} / {@code loadRelationshipFields}, i.e. outside this
     * chain entirely.</p>
     */
    @Test
    public void test_neighbouringFieldTypes_areUnchanged() {
        final Field numericField = new FieldDataGen().type(TextField.class)
                .dataType(DataTypes.INTEGER).defaultValue("0")
                .velocityVarName(NUMERIC_FIELD_VAR).indexed(true).next();
        // RadioField, not CheckboxField: only Hidden/Radio/Select accept DataTypes.BOOL, which
        // is what puts the field on a bool% column and therefore in the branch above the
        // numeric one.
        final Field boolField = new FieldDataGen().type(RadioField.class)
                .dataType(DataTypes.BOOL).defaultValue("false")
                .velocityVarName("boolTestField").indexed(true).next();
        final Field dateField = new FieldDataGen().type(DateTimeField.class)
                .dataType(DataTypes.DATE).defaultValue(null)
                .velocityVarName("dateTestField").indexed(true).next();
        final Field textField = new FieldDataGen().type(TextField.class)
                .dataType(DataTypes.TEXT).defaultValue(null)
                .velocityVarName(COMPANION_FIELD_VAR).indexed(true).next();

        final ContentType type = new ContentTypeDataGen()
                .field(numericField).field(boolField).field(dateField).field(textField)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(type.id())
                .setProperty(NUMERIC_FIELD_VAR, 54L)
                .setProperty("boolTestField", true)
                .setProperty("dateTestField", new Date())
                .setProperty(COMPANION_FIELD_VAR, COMPANION_VALUE)
                .nextPersisted();

        final Map<String, Object> map = esMappingAPI.toMap(contentlet);
        // The final document lowercases every key, so build them the same way numericKey does.
        final String prefix = (type.variable() + ".").toLowerCase();

        assertEquals("numeric branch", Long.valueOf(54L), map.get(numericKey(type)));
        assertTrue("boolean branch must still emit its key",
                map.containsKey(prefix + "booltestfield"));
        assertTrue("date branch must still emit its key",
                map.containsKey(prefix + "datetestfield"));
        assertEquals("general text branch", COMPANION_VALUE,
                map.get(prefix + COMPANION_FIELD_VAR.toLowerCase()));
    }

    /**
     * AC-006, unique-field edge. The SHA-256 entry is written only when the numeric key exists
     * ({@code if (field.isUnique() && contentletMap.containsKey(keyName))}), so omitting both
     * keys skips it rather than throwing. Asserted so a future refactor of that guard cannot
     * turn this into an NPE.
     */
    @Test
    public void test_uniqueField_unconvertibleValue_skipsShaAndDoesNotThrow() {
        final Field uniqueNumericField = new FieldDataGen().type(TextField.class)
                .dataType(DataTypes.INTEGER).defaultValue("0").unique(true)
                .velocityVarName(NUMERIC_FIELD_VAR).indexed(true).next();
        final Field companionField = new FieldDataGen().type(TextField.class)
                .dataType(DataTypes.TEXT).defaultValue(null)
                .velocityVarName(COMPANION_FIELD_VAR).indexed(true).next();
        final ContentType type = new ContentTypeDataGen()
                .field(uniqueNumericField).field(companionField).nextPersisted();

        // A unique field is implicitly required, so it must carry a value at save time — the
        // field-level default is not enough. The unconvertible value is substituted in memory
        // afterwards, as everywhere else in this class.
        final Contentlet contentlet = new ContentletDataGen(type.id())
                .setProperty(COMPANION_FIELD_VAR, COMPANION_VALUE)
                .setProperty(NUMERIC_FIELD_VAR, 1L)
                .nextPersisted();
        contentlet.setStringProperty(NUMERIC_FIELD_VAR, "N/A");

        final Map<String, Object> map = esMappingAPI.toMap(contentlet);

        final String key = numericKey(type);
        assertFalse("the numeric key is omitted", map.containsKey(key));
        assertFalse("so the unique SHA-256 entry is skipped, not computed over nothing",
                map.containsKey(key + "_sha256"));
        assertTrue("and the rest of the contentlet still indexes",
                map.containsKey((type.variable() + "." + COMPANION_FIELD_VAR).toLowerCase()));
    }

    // ----------------------------------------------------------------------------------------
    // Log capture
    // ----------------------------------------------------------------------------------------

    private static List<LogEvent> capturingMappingLogs(final Runnable action) {
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        final Logger logger = (Logger) LogManager.getLogger(ESMappingAPIImpl.class);
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        return appender.events;
    }

    private static List<LogEvent> eventsAtLevel(final List<LogEvent> events, final Level level) {
        final List<LogEvent> matching = new ArrayList<>();
        for (final LogEvent event : events) {
            if (level.equals(event.getLevel())) {
                matching.add(event);
            }
        }
        return matching;
    }

    private static String messagesOf(final List<LogEvent> events) {
        final List<String> messages = new ArrayList<>();
        for (final LogEvent event : events) {
            messages.add(event.getLevel() + ": " + event.getMessage().getFormattedMessage());
        }
        return messages.toString();
    }

    private static class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        CapturingAppender() {
            super("NumericFieldCapturingAppender", null, null, true, null);
        }

        @Override
        public void append(final LogEvent event) {
            events.add(event.toImmutable());
        }
    }

    // ----------------------------------------------------------------------------------------
    // End-to-end through a real index: the sort invariant and the absent-not-zero contract.
    //
    // These two are the reason the _dotraw finding matters. Everything above proves the emitted
    // MAP is right; only a real query proves the resulting ORDER is right.
    // ----------------------------------------------------------------------------------------

    /**
     * Indexes three convertible documents plus one whose value cannot be converted, then sorts by
     * that field ascending and descending.
     *
     * <p>Asserted on <strong>inode order</strong>, not on values: {@code search} returns
     * contentlets hydrated from the database, so their field values are the persisted ones and say
     * nothing about what the index holds.</p>
     *
     * <p>The unconvertible document is deliberately seeded with {@code 50} — between {@code 40}
     * and {@code 300}. If the index still carried a value for it, it would sort into that gap.
     * With both keys omitted it has no sort key and the engines place it last, leaving the three
     * convertible documents in strict numeric order. That is the invariant: raw text in
     * {@code _dotraw} would sort it after everything ascending ({@code 'N'} = 0x4E &gt;
     * {@code '9'} = 0x39) but an unpadded number would sort it before {@code 9}, silently
     * reordering the listing.</p>
     */
    @Test
    public void test_unconvertibleDocument_doesNotDisturbSortOrder() throws Exception {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);

        final String five = indexedWithNumber(type, 5L).getInode();
        final String forty = indexedWithNumber(type, 40L).getInode();
        final String threeHundred = indexedWithNumber(type, 300L).getInode();
        // Seeded at 50 so that a leaked index value would land it between 40 and 300.
        final String unconvertible = indexedWithUnconvertibleString(type, 50L, "N/A");

        final String sortField =
                type.variable().toLowerCase() + "." + NUMERIC_FIELD_VAR.toLowerCase();
        final String query = "+contentType:" + type.variable();

        final List<String> ascending = inodesInSortOrder(query, sortField + " asc");
        assertEquals("the three convertible documents must be in numeric order, with the "
                        + "unconvertible one carrying no sort key at all: " + ascending,
                List.of(five, forty, threeHundred), withoutInode(ascending, unconvertible));
        // Deliberately NOT asserting where the unconvertible document itself lands. Both
        // `nextPersisted()` and `addContentToIndex` enqueue their index write as a commit
        // listener while a transaction is open, so which of the two wins is not deterministic:
        // if the save's listener runs last it overwrites the document with the clean database
        // version (the 50 seed) and the position means nothing. What IS deterministic — and is
        // the invariant that matters — is that the convertible documents keep strict numeric
        // order, asserted above and below. The absence of a fabricated value in the index is
        // proven separately by `test_unconvertibleDocument_doesNotMatchARangeQuery`.

        final List<String> descending = inodesInSortOrder(query, sortField + " desc");
        assertEquals("and descending must be the exact reverse: " + descending,
                List.of(threeHundred, forty, five), withoutInode(descending, unconvertible));
    }

    /**
     * The unconvertible document must not answer a range query that happens to include zero —
     * which is what emitting a {@code 0} sentinel under the numeric key would have caused, with
     * only a WARN to reveal it. Seeded at {@code 50}, outside the queried range, so a leaked
     * index value cannot be mistaken for the fabricated zero.
     */
    @Test
    public void test_unconvertibleDocument_doesNotMatchARangeQuery() throws Exception {
        final ContentType type = typeWithNumericTextField(DataTypes.INTEGER);

        final Contentlet inRange = indexedWithNumber(type, 7L);
        indexedWithUnconvertibleString(type, 50L, "N/A");

        final String field = type.variable().toLowerCase() + "." + NUMERIC_FIELD_VAR.toLowerCase();
        final List<Contentlet> hits = APILocator.getContentletAPI().search(
                "+contentType:" + type.variable() + " +" + field + ":[0 TO 10]",
                100, 0, null, APILocator.systemUser(), false);

        assertEquals("only the genuine in-range document may match: " + inodesOf(hits),
                1, hits.size());
        assertEquals(inRange.getInode(), hits.get(0).getInode());
    }

    private static Contentlet indexedWithNumber(final ContentType type, final Number value)
            throws Exception {
        return new ContentletDataGen(type.id())
                .setProperty(COMPANION_FIELD_VAR, COMPANION_VALUE)
                .setProperty(NUMERIC_FIELD_VAR, value)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
    }

    /**
     * The bad value cannot be saved through the API — {@code validateContentlet} rejects a value
     * whose type contradicts its column with {@code [BADTYPE]}, which is exactly why no
     * user-facing path can create this data. So the contentlet is persisted with a valid
     * {@code seed} and then re-indexed with the bad value substituted in memory.
     * {@code addContentToIndex} indexes the instance it is handed without reloading it, so the
     * substituted value is what reaches the index — while the database keeps {@code seed}.
     *
     * @return the inode of the re-indexed contentlet
     */
    private static String indexedWithUnconvertibleString(final ContentType type,
            final Number seed, final String badValue) throws Exception {
        final Contentlet contentlet = indexedWithNumber(type, seed);
        contentlet.setStringProperty(NUMERIC_FIELD_VAR, badValue);
        contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletIndexAPI().addContentToIndex(contentlet, false);
        return contentlet.getInode();
    }

    private static List<String> inodesInSortOrder(final String query, final String sortBy)
            throws Exception {
        return inodeListOf(APILocator.getContentletAPI()
                .search(query, 100, 0, sortBy, APILocator.systemUser(), false));
    }

    private static List<String> withoutInode(final List<String> inodes, final String excluded) {
        final List<String> remaining = new ArrayList<>(inodes);
        remaining.remove(excluded);
        return remaining;
    }

    private static List<String> inodeListOf(final List<Contentlet> contentlets) {
        final List<String> inodes = new ArrayList<>();
        for (final Contentlet contentlet : contentlets) {
            inodes.add(contentlet.getInode());
        }
        return inodes;
    }

    private static String inodesOf(final List<Contentlet> contentlets) {
        return inodeListOf(contentlets).toString();
    }
}
