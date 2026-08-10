package com.dotcms.rendering.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integrity tests for {@link HtmlMinifier}.
 * <p>
 * {@link HtmlMinifierTest} asserts exact output, which only covers cases somebody thought to write
 * down. This class asserts an <b>invariant</b> instead: minified markup must be
 * <i>semantically identical</i> to what went in, so a corruption nobody anticipated still fails,
 * within the limits of what the comparisons below can observe. Those limits are real, and the
 * "Known limitation" note further down says where they bite.
 * <p>
 * The oracle in {@link #integrityFailures(String, String)} compares four things, each of which maps to
 * a way minification is known to go wrong:
 * <ol>
 *   <li><b>Attribute values</b>, byte-for-byte. Collapsing whitespace inside a quoted value
 *       silently rewrites form values, JSON data attributes and accessible text.</li>
 *   <li><b>Raw text regions</b> ({@code script}, {@code style}, {@code pre}, {@code textarea}),
 *       byte-for-byte. Touching a {@code script} body can break JavaScript automatic semicolon
 *       insertion; touching {@code pre} or {@code textarea} changes rendered output.</li>
 *   <li><b>Visible text</b>, whitespace-normalised. Catches words being joined together when a
 *       significant space is dropped.</li>
 *   <li><b>Element structure</b>. Catches markup being restructured, truncated or dropped.</li>
 * </ol>
 * Plus idempotence, which matters because LIVE mode can minify on write and again through
 * {@code eval()}.
 * <p>
 * <b>Known limitation.</b> This oracle is not a browser. jsoup has no CSS model, so the visible-text
 * comparison cannot observe a spacing change beside an element that contributes <i>no text of its
 * own</i>: dropping the space in {@code <b>a</b> <svg></svg>} or
 * {@code hola <script>x</script> mundo} leaves {@link Document#text()} identical, and the change
 * goes unseen. Two content bugs of exactly that shape reached review before anyone noticed. The
 * {@code option} allowance in {@link #visibleText(Document)} is the same limitation surfacing.
 * <p>
 * {@link #test_minify_keeps_whitespace_beside_every_significant_neighbour()} is the compensating
 * control: it asserts adjacency directly, so it does not care whether the element renders any text.
 * <p>
 * New bug classes should be added as a corpus entry or a fixture here rather than as another
 * exact-output assertion, so the invariant does the work.
 *
 * @see HtmlMinifierTest
 */
public class HtmlMinifierIntegrityTest {

    /**
     * Real rendered pages, captured from the demo site, kept under
     * {@code src/test/resources/com/dotcms/rendering/util/html-corpus}. Real markup exercises
     * combinations nobody writes by hand: the home page carries an 11KB inline {@code <style>}
     * block and 630 attribute values, and the member page an inline script that relies on automatic
     * semicolon insertion.
     * <p>
     * {@code icons-and-media.html} is the one page here that was built rather than captured. Neither
     * demo page contains an {@code <svg>}, {@code <iframe>}, {@code <canvas>}, {@code <video>} or
     * {@code <audio>}, so real markup could not have caught the whitespace bugs around those
     * elements. It also carries an ideographic space, JSON in single-quoted attributes and a
     * {@code <template>}.
     */
    private static final String[] CORPUS =
            {"demo-home.html", "demo-members.html", "icons-and-media.html"};

    /**
     * Markup that targets a specific way minification can corrupt a page. Each entry is a case that
     * a previous implementation got wrong, or that is one small change away from being got wrong.
     */
    private static final String[][] FIXTURES = {
            {"whitespace run in attribute value", "<input value=\"a    b\">"},
            {"newline in attribute value", "<img alt=\"a\nb\">"},
            {"tab in attribute value", "<div title=\"a\tb\">x</div>"},
            {"JSON in a single-quoted attribute", "<div data-config='{\"key\": \"a   b\"}'>x</div>"},
            {"angle bracket inside a quoted value", "<div title=\"a > b\">x</div>"},
            {"unquoted attribute value", "<a href=/foo/bar >x</a>"},
            {"literal greater-than in text", "<p>Home > About</p>"},
            {"literal less-than in text", "<p>3 < 4</p>"},
            {"ideographic space U+3000", "<p>a　b</p>"},
            {"thin space U+2009", "<p>a b</p>"},
            {"no-break space U+00A0", "<p>a b</p>"},
            {"inline elements separated by space", "<span>a</span> <span>b</span>"},
            {"text adjacent to inline markup", "<p>Hello <b>world</b> again</p>"},
            {"script relying on ASI", "<script>\nvar a=1\nvar b=2\n</script>"},
            {"script containing an HTML comment", "<script>\n// <!-- hide\nvar a=1;\n// -->\n</script>"},
            {"pre with significant indentation", "<div>\n<pre>line1\n   line2</pre>\n</div>"},
            {"textarea with leading whitespace", "<textarea>\n  a\n  b</textarea>"},
            {"style block", "<style>\n.a { color: red }\n</style>"},
            {"uppercase preserved tag", "<PRE>a\n b</PRE>"},
            {"tag whose name prefixes a preserved tag", "<scriptural>\n  x</scriptural>"},
            {"space that is not a comment opener", "<div>< !-- not a comment --></div>"},
            {"text that is not a comment terminator", "<div>a -- > b</div>"},
            {"commented-out preserved tag", "<div>\n  <!-- <pre>x\n y</pre> -->\n  <p>real</p>\n</div>"},
            {"commented-out closing body", "<body>\n<p>x</p>\n</body>\n<!-- </body> -->\n</html>"},
            {"conditional comment", "<!--[if IE]>\n  <link href=\"ie.css\">\n<![endif]-->"},
            {"degenerate close tag", "<div>\n</>\n</div>"},
            {"void element with self-closing slash", "<div>\n<br />\n</div>"},
            {"nested inline inside block", "<ul>\n <li>a <em>b</em> c</li>\n <li>d</li>\n</ul>"},
            {"table markup", "<table>\n <tr>\n  <td>a</td>\n  <td>b</td>\n </tr>\n</table>"},
            {"entity adjacent to whitespace", "<p>a &amp; b</p>"},

            // Weak-lookup cases. Finding a preserved region means finding two boundaries, and
            // anything that merely *looks* like one is a chance to get it wrong -- the same shape of
            // bug as a body-tag search that matched a commented-out <body>. Every entry below holds
            // a script body whose newlines are load bearing (automatic semicolon insertion), so
            // misjudging either boundary corrupts JavaScript rather than just losing bytes.
            {"script with attributes",
                    "<script type=\"text/javascript\">\nvar a=1\nvar b=2\n</script>"},
            {"script tag name in upper case",
                    "<SCRIPT>\nvar a=1\nvar b=2\n</SCRIPT>"},
            {"greater-than inside the script's own attribute",
                    "<script data-tpl=\"a>b\">\nvar a=1\nvar b=2\n</script>"},
            {"opening script tag broken across lines",
                    "<script\n    type=\"text/javascript\">\nvar a=1\nvar b=2\n</script>"},
            {"whitespace inside the closing script tag",
                    "<script>\nvar a=1\nvar b=2\n</script >"},
            {"script markup inside an attribute value, not real markup",
                    "<div data-tpl=\"<script>x</script>\">\n  <p>y</p>\n</div>"},
            {"commented-out script",
                    "<div>\n  <!-- <script>\nvar a=1\nvar b=2\n</script> -->\n  <p>real</p>\n</div>"},
            {"script inside a retained conditional comment",
                    "<!--[if IE]><script>\nvar a=1\nvar b=2\n</script><![endif]-->"},
            {"text that resembles a closing tag inside a script",
                    "<script>\nvar s = \"</div>\"\nvar b=2\n</script>"},
            {"two adjacent scripts",
                    "<script>\nvar a=1\nvar b=2\n</script>\n<script>\nvar c=3\nvar d=4\n</script>"},
            {"empty script with a src attribute", "<div>\n<script src=\"a.js\"></script>\n</div>"},
            {"unclosed script", "<div>\n<script>\nvar a=1\nvar b=2\n"},
            {"style whose body contains a brace and a quote",
                    "<style>\n.a[data-x=\"1\"] { content: \"}\" }\n</style>"},

            // A removed comment is transparent to whitespace collapsing: what decides whether the
            // space before it survives is the content on the far side, not the comment. Getting
            // this wrong joins words, which the visible-text comparison catches.
            {"comment between inline elements, space before it only",
                    "<span>a</span> <!--c--><span>b</span>"},
            {"comment between text and inline markup",
                    "<p>a <!--c--><b>b</b></p>"},
            {"consecutive comments between inline elements",
                    "<span>a</span> <!--c--><!--d--><span>b</span>"},
            {"comment with whitespace on both sides",
                    "<span>a</span> <!--c--> <span>b</span>"},
            {"comment before a block element",
                    "<span>a</span> <!--c--><div>b</div>"},
            {"unterminated comment between inline elements",
                    "<span>a</span> <!--c<span>b</span>"},

            // display:none elements are transparent to whitespace collapsing in the same way a
            // removed comment is. Note the oracle cannot see most of these on its own -- that is the
            // known limitation in the class doc -- so they are here for the record and it is
            // test_minify_keeps_whitespace_beside_every_significant_neighbour that guards them.
            {"script between text", "hola <script>var x=1</script> mundo"},
            {"style between inline elements", "<b>a</b> <style>.x{}</style> <b>b</b>"},
            {"template between inline elements", "<b>a</b> <template><i>t</i></template> <b>b</b>"},
            {"noscript between inline elements", "<b>a</b> <noscript>n</noscript> <b>b</b>"},
            {"inline svg after text", "Ver <svg width=\"8\"></svg>"},
            {"inline svg with children", "<p>a <svg><circle r=\"1\"></circle></svg> b</p>"},
            {"iframe between inline elements", "<b>a</b> <iframe src=\"x\"></iframe> <b>b</b>"},
            {"canvas between inline elements", "<b>a</b> <canvas></canvas> <b>b</b>"},

            // A conditional comment is markup only to browsers nobody ships; to everything else it
            // paints nothing, so it is as transparent as a removed comment. Every conditional-comment
            // case in both suites used to sit in isolation, which is precisely why the whitespace
            // beside one went unchecked -- the adjacency test cannot reach it either, being driven by
            // tag names.
            {"conditional comment between text", "a <!--[if IE]>x<![endif]-->b"},
            {"conditional comment between inline elements",
                    "<b>a</b> <!--[if IE]><b>x</b><![endif]--><b>b</b>"},
            {"conditional comment with whitespace on both sides",
                    "a <!--[if IE]>x<![endif]--> b"},
            {"downlevel-revealed conditional comment",
                    "<b>a</b> <!--[if !IE]>--><b>x</b><!--<![endif]-->"},
            {"hidden dialog between text", "a <dialog>x</dialog> b"},
    };

    /**
     * Every tag beside which whitespace is painted, held here as an <b>independent specification</b>
     * of the HTML rendering model rather than read from {@link HtmlMinifier}.
     * <p>
     * Inline-level elements sit in an inline formatting context, so whitespace between them is
     * rendered. Elements with {@code display: none} generate no box, so the whitespace on either
     * side of them separates whatever surrounds them and is also rendered. Both classes therefore
     * belong here.
     * <p>
     * Keeping this list separate from the implementation's is the point: the two must agree, and a
     * tag present in one but missing from the other fails this test instead of quietly changing what
     * pages render. Reading the implementation's own set would make the test agree with any bug.
     */
    private static final String[] WHITESPACE_SIGNIFICANT_NEIGHBOURS = {
            "a", "abbr", "acronym", "audio", "b", "bdi", "bdo", "big", "br", "button", "canvas",
            "cite", "code", "data", "datalist", "del", "dfn", "em", "embed", "font", "i", "iframe",
            "img", "input", "ins", "kbd", "label", "map", "mark", "math", "meter", "nobr",
            "noscript", "object", "output", "picture", "progress", "q", "rp", "rt", "rtc", "ruby",
            "dialog", "s", "samp", "script", "select", "slot", "small", "span", "strike", "strong",
            "style",
            "sub", "sup", "svg", "template", "textarea", "time", "tt", "u", "var", "video", "wbr"};

    /**
     * Block-level tags, as the negative control. Whitespace beside these is <i>not</i> rendered, so
     * it must be removed. Without this, widening the significant set until everything is preserved
     * would pass the test above while minifying nothing.
     */
    private static final String[] BLOCK_NEIGHBOURS = {
            "address", "article", "aside", "blockquote", "caption", "dd", "details", "div", "dl",
            "dt", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5",
            "h6", "header", "hr", "legend", "li", "main", "nav", "ol", "p", "pre", "section",
            "summary", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul"};

    private static final String ELEMENT_BARE = "<%1$s></%1$s>";
    private static final String DOCUMENT_BARE = "alpha %s omega";

    /**
     * The layouts each tag is checked in, as {@code {description, element format, document format}}.
     * One empty element between two bare words is the easy case; these add the variations that a real
     * template actually produces, so a fix that only works in the simplest position is caught.
     */
    private static final String[][] NEIGHBOUR_SHAPES = {
            {"bare", ELEMENT_BARE, DOCUMENT_BARE},
            {"with attributes", "<%1$s class=\"c\" data-x=\"1\"></%1$s>", DOCUMENT_BARE},
            {"with children", "<%1$s><i>x</i></%1$s>", DOCUMENT_BARE},
            {"inside a block element", ELEMENT_BARE, "<p>alpha %s omega</p>"},
            {"nested two levels deep", ELEMENT_BARE, "<div><section>alpha %s omega</section></div>"},
            {"indented across lines", ELEMENT_BARE, "<div>\n    alpha %s omega\n</div>"},
            {"followed by more markup", ELEMENT_BARE, "<p>alpha %s omega</p>\n<p>tail</p>"},
            {"beside inline markup", ELEMENT_BARE, "<p><b>alpha</b> %s <b>omega</b></p>"},
    };

    @BeforeClass
    public static void prepare() {
        Config.initializeConfig();
    }

    /**
     * Given: markup that targets a known corruption mode.
     * Expected: minifying it changes formatting only, never content.
     */
    @Test
    public void test_minify_preserves_integrity_of_fixtures() {
        final List<String> failures = new ArrayList<>();
        for (final String[] fixture : FIXTURES) {
            failures.addAll(integrityFailures(fixture[0], fixture[1]));
        }
        assertNoFailures(FIXTURES.length + " fixtures", failures);
    }

    /**
     * Given: real rendered pages from the demo site.
     * Expected: minifying them changes formatting only, never content. This is the case that
     * hand-written fixtures cannot cover, because real templates combine features in ways nobody
     * writes deliberately.
     */
    @Test
    public void test_minify_preserves_integrity_of_real_pages() throws IOException {
        final List<String> failures = new ArrayList<>();
        for (final String name : CORPUS) {
            final String html = readCorpus(name);
            assertTrue(name + " should be a substantial page", html.length() > 5000);
            failures.addAll(integrityFailures(name, html));
        }
        assertNoFailures(CORPUS.length + " real pages", failures);
    }

    /**
     * Given: real pages, and every place in them where a word is separated from a
     * whitespace-significant element by whitespace, {@code Home <svg>} being the common shape.
     * Expected: every one of those separations survives.
     * <p>
     * The adjacency test above proves the property on constructed markup;
     * {@link #integrityFailures(String, String)} cannot prove it on a real page at all, because the
     * elements in question render no text of their own. This closes that: it counts the separations
     * in the input and requires the output to still have them, which needs no CSS model and no
     * judgement about which whitespace was removable.
     */
    @Test
    public void test_minify_keeps_word_to_element_separations_in_real_pages() throws IOException {
        final List<String> failures = new ArrayList<>();
        for (final String name : CORPUS) {
            final String original = readCorpus(name);
            final String minified = HtmlMinifier.minify(original);
            for (final String tag : WHITESPACE_SIGNIFICANT_NEIGHBOURS) {
                // A letter or digit, whitespace, then the element: a word visibly separated from it.
                final Pattern separated = Pattern.compile("[\\p{L}\\p{N}]\\s+<" + tag + "\\b",
                        Pattern.CASE_INSENSITIVE);
                final long before = separated.matcher(original).results().count();
                final long after = separated.matcher(minified).results().count();
                failures.addAll(after < before
                        ? List.of(String.format("%s: %d of %d <%s> elements lost the space "
                                + "separating them from the preceding word", name, before - after,
                                before, tag))
                        : List.of());
            }
        }
        assertNoFailures(CORPUS.length + " real pages", failures);
    }

    /**
     * Given: real rendered pages.
     * Expected: minification actually removes a worthwhile amount. Guards the opposite failure from
     * the rest of this class: an over-cautious change that keeps integrity by doing nothing.
     */
    @Test
    public void test_minify_actually_reduces_size() throws IOException {
        for (final String name : CORPUS) {
            final String html = readCorpus(name);
            final int before = html.length();
            final int after = HtmlMinifier.minify(html).length();
            final double saved = 100.0 * (before - after) / before;
            assertTrue(String.format("%s: expected >15%% reduction, got %.1f%% (%d -> %d)",
                    name, saved, before, after), saved > 15.0);
        }
    }

    /**
     * Given: two words separated by whitespace, with one element of every whitespace-significant kind
     * placed in that gap.
     * Expected: the words stay separated.
     * <p>
     * This asserts <i>adjacency</i> rather than text, which is what makes it see the cases the
     * visible-text comparison is blind to: it never asks the element in the middle to render
     * anything. It is also data driven, so a tag added to
     * {@link #WHITESPACE_SIGNIFICANT_NEIGHBOURS} is covered without writing another assertion.
     */
    @Test
    public void test_minify_keeps_whitespace_beside_every_significant_neighbour() {
        final List<String> failures = new ArrayList<>();
        for (final String tag : WHITESPACE_SIGNIFICANT_NEIGHBOURS) {
            for (final String[] shape : NEIGHBOUR_SHAPES) {
                final String separator = separatorAround(tag, shape[1], shape[2]);
                failures.addAll(separator.isEmpty()
                        ? List.of(String.format("<%s> (%s): the space between the words was removed, "
                                + "so they render joined", tag, shape[0]))
                        : List.of());
            }
        }
        assertNoFailures(WHITESPACE_SIGNIFICANT_NEIGHBOURS.length + " tags x "
                + NEIGHBOUR_SHAPES.length + " shapes", failures);
    }

    /**
     * Given: the same two words with a block-level element in the gap.
     * Expected: the whitespace is removed, because a block element ends the line either side of it so
     * nothing is painted there. The negative half of the test above: it stops the significant set
     * being widened until the minifier stops minifying.
     */
    @Test
    public void test_minify_removes_whitespace_beside_block_neighbours() {
        final List<String> failures = new ArrayList<>();
        for (final String tag : BLOCK_NEIGHBOURS) {
            final String separator = separatorAround(tag, ELEMENT_BARE, DOCUMENT_BARE);
            failures.addAll(separator.isEmpty() ? List.of()
                    : List.of(String.format("<%s>: expected the whitespace to be removed, kept %s",
                            tag, abbreviate(separator))));
        }
        assertNoFailures(BLOCK_NEIGHBOURS.length + " block-level tags", failures);
    }

    /**
     * Minifies a document holding {@code alpha <tag>...</tag> omega} and reports what is left
     * separating the two words once the element in the middle is taken back out. Plain words rather
     * than elements on either side, so nothing depends on the wrapper's own tag being handled
     * correctly.
     *
     * @param tag the tag under test
     * @param elementFormat how to build the element, {@code %1$s} being the tag name
     * @param documentFormat how to build the surrounding document, {@code %s} being the element
     * @return the surviving separator, empty when the words ended up joined
     */
    private static String separatorAround(final String tag, final String elementFormat,
            final String documentFormat) {

        final String element = String.format(elementFormat, tag);
        final String minified = HtmlMinifier.minify(String.format(documentFormat, element));

        // Non-greedy and unanchored, so the same expression works whether or not the words sit
        // inside wrapping markup.
        final Matcher matcher = Pattern.compile("alpha(.*?)omega", Pattern.DOTALL).matcher(minified);
        assertTrue(tag + ": unexpected shape, the words themselves were altered: " + minified,
                matcher.find());

        final String between = matcher.group(1);
        assertTrue(tag + ": the element under test was rewritten, so this shape proves nothing: "
                + minified, between.contains(element));

        // Tags have to come out as well as the element. Markup between the words contributes no
        // spacing of its own, so leaving it in would make the separator look non-empty and the check
        // would pass whatever the minifier did -- which is exactly what the
        // "beside inline markup" shape did until this line existed.
        return between.replace(element, "").replaceAll("<[^>]*>", "");
    }

    /**
     * Given: no configuration for the feature flag.
     * Expected: minification is off. The feature must never become active because a default
     * changed somewhere else.
     */
    @Test
    public void test_flag_is_off_by_default() {
        final boolean original =
                Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, false);
        try {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, null);
            assertFalse("HTML minification must be opt-in", HtmlMinifier.isEnabled());
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, original);
        }
    }

    /**
     * Reports every way in which minifying {@code original} changed its meaning rather than just its
     * formatting.
     * <p>
     * Returns the mismatches instead of asserting on the first one, so that a run over many inputs
     * reports all of them at once. Stopping at the first failure hides how widespread a regression
     * is, and hides whether the later inputs would have caught it too.
     *
     * @param label a description used in failure messages
     * @param original the markup to check
     * @return one entry per mismatch, empty when the markup survived intact
     */
    private static List<String> integrityFailures(final String label, final String original) {

        final String minified = HtmlMinifier.minify(original);

        // Both sides go through the same parser, so any normalisation the parser applies cancels
        // out and only differences the minifier introduced remain.
        final Document before = parse(original);
        final Document after = parse(minified);

        final List<String> failures = new ArrayList<>();
        compare(failures, label, "attribute values", attributeValues(before), attributeValues(after));
        compare(failures, label, "whitespace-sensitive regions", rawRegions(before), rawRegions(after));
        compare(failures, label, "visible text", visibleText(before), visibleText(after));
        compare(failures, label, "element structure", structure(before), structure(after));
        compare(failures, label, "idempotence", minified, HtmlMinifier.minify(minified));
        return failures;
    }

    /**
     * Records a mismatch between what went in and what came out, if there is one.
     */
    private static void compare(final List<String> failures, final String label,
            final String aspect, final Object expected, final Object actual) {
        failures.addAll(Objects.equals(expected, actual) ? List.of()
                : List.of(String.format("[%s] %s changed%n     expected: %s%n     actual  : %s",
                        label, aspect, abbreviate(expected), abbreviate(actual))));
    }

    /**
     * @return the value as a string, shortened so one failure cannot bury the others
     */
    private static String abbreviate(final Object value) {
        final String text = String.valueOf(value).replace("\n", "\\n");
        return text.length() <= 220 ? text : text.substring(0, 220) + "... (" + text.length() + " chars)";
    }

    /**
     * Fails with every mismatch listed, rather than only the first.
     */
    private static void assertNoFailures(final String scope, final List<String> failures) {
        assertTrue(String.format("minification altered content in %d place(s) across %s:%n%n%s%n",
                        failures.size(), scope, String.join(System.lineSeparator(), failures)),
                failures.isEmpty());
    }

    /**
     * Parses with the HTML parser but without pretty-printing, so {@link Document#html()} and the
     * raw-text accessors reflect the input rather than jsoup's own indentation.
     */
    private static Document parse(final String html) {
        final Document document = Jsoup.parse(html, "", Parser.htmlParser());
        document.outputSettings().prettyPrint(false);
        return document;
    }

    /**
     * @return every attribute as {@code tag|name=value}, in document order. Values are compared
     * exactly, which is what catches whitespace inside a quoted value being collapsed.
     */
    private static List<String> attributeValues(final Document document) {
        final List<String> values = new ArrayList<>();
        for (final Element element : document.getAllElements()) {
            for (final Attribute attribute : element.attributes()) {
                values.add(element.tagName() + "|" + attribute.getKey() + "=" + attribute.getValue());
            }
        }
        return values;
    }

    /**
     * @return the concatenated contents of every element whose text is whitespace sensitive. For
     * {@code script} and {@code style} that is the data node; for {@code pre} and {@code textarea}
     * it is the un-normalised text.
     */
    private static List<String> rawRegions(final Document document) {
        final List<String> regions = new ArrayList<>();
        for (final Element element : document.select("script, style")) {
            regions.add(element.tagName() + ":" + element.data());
        }
        for (final Element element : document.select("pre, textarea")) {
            regions.add(element.tagName() + ":" + element.wholeText());
        }
        return regions;
    }

    /**
     * @return the tag name of every element in document order. Text nodes are excluded, since
     * dropping an insignificant whitespace-only text node is exactly what minification is for.
     */
    private static List<String> structure(final Document document) {
        final List<String> tags = new ArrayList<>();
        for (final Element element : document.getAllElements()) {
            tags.add(element.tagName());
        }
        return tags;
    }

    /**
     * @return the document's rendered text, flattened and whitespace-normalised, so that legitimate
     * collapsing is ignored but joined or lost words are not
     */
    private static String visibleText(final Document document) {

        final Document copy = document.clone();

        // A browser renders each <option> as a discrete item in a control, so whitespace between
        // them is never painted and removing it is correct. jsoup has no CSS model and treats
        // options as inline, so it concatenates their text and that correct removal reads as two
        // words being joined. Insert an explicit separator, on both sides of the comparison, to put
        // the boundary back. Whitespace *within* an option's own text is still compared.
        for (final Element element : copy.select("option, optgroup")) {
            element.after(new TextNode(" "));
        }

        return collapse(copy.text());
    }

    /**
     * @return the text with every whitespace run reduced to one space and the ends trimmed, so that
     * legitimate collapsing is ignored but joined or lost words are not
     */
    private static String collapse(final String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String readCorpus(final String name) throws IOException {
        try (InputStream stream = HtmlMinifierIntegrityTest.class
                .getResourceAsStream("/com/dotcms/rendering/util/html-corpus/" + name)) {
            assertNotNull("missing corpus resource: " + name, stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
