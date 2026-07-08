package com.dotcms.tiptap;

import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Converts an HTML fragment into a Tiptap JSON (ProseMirror document) node, server-side, without
 * a browser DOM. Companion to {@link TiptapMarkdown}: where that class handles the Markdown leg of
 * the save path (see {@code MapToContentletPopulator}), this handles the HTML leg. The emitted node
 * shapes are deliberately identical to {@link TiptapMarkdown}'s output — that class's
 * {@code TiptapBuilder} is the authoritative reference for the shape of every node and mark — so the
 * two ingestion paths produce documents the dotCMS Block Editor loads the same way.
 *
 * <h3>Scope</h3>
 * Output is restricted to the same primitive node/mark set the Markdown leg can produce
 * (see {@link TiptapMarkdown#isMarkdownRepresentable(String)}): paragraphs, headings, blockquotes,
 * bullet/ordered lists, code blocks, tables, horizontal rules, hard breaks, {@code dotImage}, and the
 * bold/italic/strike/code/link marks. Anything outside that set degrades gracefully — dangerous
 * elements are dropped, unknown-but-safe elements are made transparent (their text is kept) — and the
 * converter never throws (a bad fragment yields an empty document, mirroring
 * {@link TiptapMarkdown#toTiptap(String)} on {@code null}).
 *
 * <h3>Sanitization</h3>
 * HTML from non-interactive clients is untrusted. This converter drops executable/active-content
 * subtrees entirely (their text does not leak), copies only an allow-list of attributes (so event
 * handlers are never read), and validates {@code href}/{@code src} against a scheme allow-list while
 * rejecting the characters that would let a value break out of an HTML attribute downstream. It does
 * <b>not</b> attempt to make free-text ({@code alt}/{@code title}) or text-node content safe for an
 * HTML render surface that fails to escape — that is the render layer's responsibility and is tracked
 * separately; the structural sanitization here adds no new attack surface over the Markdown leg.
 *
 * @author hassandotcms
 */
public final class TiptapHtml {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Guards the recursive walker against a stack overflow on pathologically nested HTML. */
    private static final int MAX_DEPTH = 128;

    /** Elements whose entire subtree (including text) is discarded — active/executable content. */
    private static final Set<String> DROP_SUBTREE = Set.of(
            "script", "style", "iframe", "object", "embed", "noscript", "svg", "math", "template",
            "base", "meta", "link", "head", "title", "textarea", "select", "option", "button",
            "form", "input", "applet", "frame", "frameset", "canvas", "audio", "video", "source",
            "track", "param", "map", "area");

    /** Inline elements that map to a Tiptap mark. */
    private static final Set<String> BOLD_TAGS = Set.of("strong", "b");
    private static final Set<String> ITALIC_TAGS = Set.of("em", "i");
    private static final Set<String> STRIKE_TAGS = Set.of("s", "strike", "del");

    /**
     * Block-level elements with no Tiptap equivalent that still act as a visual block boundary
     * (a paragraph break), as opposed to inline wrappers like {@code span}. Their children are
     * rendered as separate blocks rather than merged into the surrounding inline run.
     */
    private static final Set<String> TRANSPARENT_BLOCK = Set.of(
            "div", "section", "article", "header", "footer", "main", "aside", "nav", "figure",
            "figcaption", "dl", "dt", "dd", "address", "details", "summary", "fieldset", "hgroup",
            "picture");

    /** Sane ceiling for a single cell's span; malformed HTML can carry absurd values. */
    private static final int MAX_SPAN = 1000;

    private TiptapHtml() { }

    /**
     * Parse an HTML fragment into a Tiptap document node ({@code {"type":"doc","content":[...]}}).
     * Never throws: {@code null}/blank input and unparseable content yield an empty document.
     */
    public static ObjectNode toTiptap(final String html) {
        final ObjectNode doc = MAPPER.createObjectNode();
        doc.put("type", "doc");
        final ArrayNode content = doc.putArray("content");
        if (html == null || html.isBlank()) {
            return doc;
        }
        // Empty base URI: never absolutize hrefs/srcs — store them exactly as authored.
        final Element body = Jsoup.parseBodyFragment(html, "").body();
        renderBlocks(body, content, new ArrayDeque<>(), 0);
        return doc;
    }

    // =====================================================================
    // Block context: children become a sequence of block nodes; loose inline
    // content is gathered into implicit paragraphs.
    // =====================================================================

    private static void renderBlocks(final Element parent, final ArrayNode sink,
                                     final Deque<ObjectNode> marks, final int depth) {
        if (depth > MAX_DEPTH) {
            noteDepthExceeded();
            return;
        }
        final InlineRun run = new InlineRun();
        for (final Node child : parent.childNodes()) {
            dispatchBlock(child, sink, run, marks, depth);
        }
        run.flushInto(sink);
    }

    private static void dispatchBlock(final Node child, final ArrayNode sink, final InlineRun run,
                                      final Deque<ObjectNode> marks, final int depth) {
        if (depth > MAX_DEPTH) {
            noteDepthExceeded();
            return;
        }
        if (child instanceof TextNode) {
            run.addText(((TextNode) child).getWholeText(), marks);
            return;
        }
        if (!(child instanceof Element)) {
            return; // comments, doctype, etc.
        }
        final Element e = (Element) child;
        final String tag = e.normalName();

        if (DROP_SUBTREE.contains(tag)) {
            return;
        }
        final ObjectNode mark = markFor(tag, e);
        if (mark != null || isMarkTag(tag)) {
            // A mark element may itself contain block content (e.g. <b><p>x</p></b>): recurse in the
            // SAME block context so those blocks lift out correctly, with the mark still active.
            if (mark != null) {
                marks.push(mark);
            }
            for (final Node grand : e.childNodes()) {
                dispatchBlock(grand, sink, run, marks, depth + 1);
            }
            if (mark != null) {
                marks.pop();
            }
            return;
        }
        if ("br".equals(tag)) {
            run.addHardBreak();
            return;
        }
        if ("img".equals(tag)) {
            final ObjectNode img = imageNode(e, marks);
            if (img != null) {
                run.add(img);
            }
            return;
        }
        if (TRANSPARENT_BLOCK.contains(tag)) {
            // Unmapped block element: acts as a paragraph boundary, its children are their own blocks.
            run.flushInto(sink);
            renderBlocks(e, sink, marks, depth + 1);
            return;
        }
        if (isHandledBlock(tag)) {
            run.flushInto(sink);
            emitBlock(tag, e, sink, marks, depth);
            return;
        }
        // Unknown inline element (span, u, sub, sup, font, ...): transparent, keep the text.
        for (final Node grand : e.childNodes()) {
            dispatchBlock(grand, sink, run, marks, depth + 1);
        }
    }

    private static void emitBlock(final String tag, final Element e, final ArrayNode sink,
                                  final Deque<ObjectNode> marks, final int depth) {
        switch (tag) {
            case "p":
                emitInlineContainer("paragraph", null, e, sink, marks, depth, true);
                break;
            case "h1": case "h2": case "h3": case "h4": case "h5": case "h6":
                final ObjectNode hAttrs = MAPPER.createObjectNode();
                hAttrs.put("level", tag.charAt(1) - '0');
                emitInlineContainer("heading", hAttrs, e, sink, marks, depth, false);
                break;
            case "blockquote":
                emitBlockContainer("blockquote", e, sink, marks, depth);
                break;
            case "ul":
                emitList("bulletList", e, sink, marks, depth);
                break;
            case "ol":
                emitList("orderedList", e, sink, marks, depth);
                break;
            case "pre":
                emitCodeBlock(e, sink);
                break;
            case "hr":
                final ObjectNode hr = MAPPER.createObjectNode();
                hr.put("type", "horizontalRule");
                sink.add(hr);
                break;
            case "table":
                emitTable(e, sink, marks, depth);
                break;
            default:
                // Not reached: isHandledBlock gates entry.
                break;
        }
    }

    /**
     * Inline-only container (paragraph, heading). Block descendants cannot nest here, so they are
     * flattened to their inline content (text preserved) rather than lifted — keeping the node valid
     * against the editor's {@code inline*} content model. An empty paragraph is kept (the editor
     * allows it); an empty heading is dropped.
     */
    private static void emitInlineContainer(final String type, final ObjectNode attrs, final Element e,
                                            final ArrayNode sink, final Deque<ObjectNode> marks,
                                            final int depth, final boolean keepIfEmpty) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", type);
        if (attrs != null) {
            node.set("attrs", attrs);
        }
        final ArrayNode content = MAPPER.createArrayNode();
        final InlineRun run = new InlineRun(content);
        renderInline(e, run, marks, depth + 1);
        run.finish();
        if (content.size() > 0) {
            node.set("content", content);
            sink.add(node);
        } else if (keepIfEmpty) {
            sink.add(node);
        }
    }

    /** Block container that requires at least one block child (blockquote). Dropped when empty. */
    private static void emitBlockContainer(final String type, final Element e, final ArrayNode sink,
                                           final Deque<ObjectNode> marks, final int depth) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", type);
        final ArrayNode content = MAPPER.createArrayNode();
        renderBlocks(e, content, marks, depth + 1);
        if (content.size() > 0) {
            node.set("content", content);
            sink.add(node);
        }
    }

    private static void emitList(final String type, final Element e, final ArrayNode sink,
                                 final Deque<ObjectNode> marks, final int depth) {
        final ObjectNode list = MAPPER.createObjectNode();
        list.put("type", type);
        if ("orderedList".equals(type)) {
            list.putObject("attrs").put("start", intAttr(e, "start", 1, 1, Integer.MAX_VALUE));
        }
        final ArrayNode items = MAPPER.createArrayNode();
        ObjectNode currentItem = null;
        for (final Node child : e.childNodes()) {
            if (child instanceof Element && "li".equals(((Element) child).normalName())) {
                currentItem = emitListItem((Element) child, marks, depth);
                items.add(currentItem);
            } else {
                // Stray content directly under ul/ol (common in legacy WYSIWYG, incl. nested lists):
                // attach it to the current item, creating one if the list has not opened an item yet.
                if (currentItem == null) {
                    currentItem = newListItem();
                    items.add(currentItem);
                }
                final ArrayNode itemContent = (ArrayNode) currentItem.get("content");
                final InlineRun stray = new InlineRun();
                dispatchBlock(child, itemContent, stray, marks, depth + 1);
                stray.flushInto(itemContent);
                normalizeListItem(currentItem);
            }
        }
        if (items.size() > 0) {
            list.set("content", items);
            sink.add(list);
        }
    }

    private static ObjectNode emitListItem(final Element li, final Deque<ObjectNode> marks,
                                           final int depth) {
        final ObjectNode item = newListItem();
        renderBlocks(li, (ArrayNode) item.get("content"), marks, depth + 1);
        normalizeListItem(item);
        return item;
    }

    private static ObjectNode newListItem() {
        final ObjectNode item = MAPPER.createObjectNode();
        item.put("type", "listItem");
        item.putArray("content");
        return item;
    }

    /** listItem content is {@code paragraph block*}: guarantee a leading paragraph. */
    private static void normalizeListItem(final ObjectNode item) {
        final ArrayNode content = (ArrayNode) item.get("content");
        if (content.size() == 0 || !"paragraph".equals(content.get(0).path("type").asText())) {
            content.insert(0, emptyParagraph());
        }
    }

    private static void emitCodeBlock(final Element pre, final ArrayNode sink) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "codeBlock");
        final String language = codeLanguage(pre);
        if (language != null) {
            node.putObject("attrs").put("language", language);
        }
        final ArrayNode content = node.putArray("content");
        // Code is verbatim: no whitespace normalization, no marks, no child elements.
        final String text = pre.wholeText();
        if (!text.isEmpty()) {
            content.add(textNode(text, null));
        }
        sink.add(node);
    }

    private static void emitTable(final Element table, final ArrayNode sink,
                                  final Deque<ObjectNode> marks, final int depth) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "table");
        final ArrayNode rows = MAPPER.createArrayNode();
        // Collect this table's own <tr> (direct, or one level down through thead/tbody/tfoot) —
        // NOT descendant rows belonging to a nested table, which are handled when that cell recurses.
        final List<Element> trs = directRows(table);
        final int totalRows = trs.size();
        for (int i = 0; i < totalRows; i++) {
            final int remainingRows = totalRows - i;
            final ObjectNode row = emitTableRow(trs.get(i), remainingRows, marks, depth);
            if (row != null) {
                rows.add(row);
            }
        }
        if (rows.size() > 0) {
            node.set("content", rows);
            sink.add(node);
        }
    }

    /** This table's own rows: direct {@code <tr>} children plus those inside its section wrappers. */
    private static List<Element> directRows(final Element table) {
        final java.util.List<Element> rows = new java.util.ArrayList<>();
        for (final Element child : table.children()) {
            final String tag = child.normalName();
            if ("tr".equals(tag)) {
                rows.add(child);
            } else if ("thead".equals(tag) || "tbody".equals(tag) || "tfoot".equals(tag)) {
                for (final Element section : child.children()) {
                    if ("tr".equals(section.normalName())) {
                        rows.add(section);
                    }
                }
            }
        }
        return rows;
    }

    private static ObjectNode emitTableRow(final Element tr, final int remainingRows,
                                           final Deque<ObjectNode> marks, final int depth) {
        final ObjectNode row = MAPPER.createObjectNode();
        row.put("type", "tableRow");
        final ArrayNode cells = MAPPER.createArrayNode();
        for (final Element cell : tr.children()) {
            final String tag = cell.normalName();
            final boolean header = "th".equals(tag);
            if (!header && !"td".equals(tag)) {
                continue;
            }
            cells.add(emitTableCell(cell, header, remainingRows, marks, depth));
        }
        if (cells.size() == 0) {
            return null;
        }
        row.set("content", cells);
        return row;
    }

    private static ObjectNode emitTableCell(final Element cell, final boolean header,
                                            final int remainingRows, final Deque<ObjectNode> marks,
                                            final int depth) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", header ? "tableHeader" : "tableCell");
        final ObjectNode attrs = node.putObject("attrs");
        attrs.put("colspan", intAttr(cell, "colspan", 1, 1, MAX_SPAN));
        // Clamp rowspan so a malformed value cannot exceed the table's geometry.
        attrs.put("rowspan", intAttr(cell, "rowspan", 1, 1, Math.max(1, remainingRows)));
        attrs.putNull("colwidth");
        // Cell content is block+: render its children as blocks, guaranteeing at least one paragraph.
        final ArrayNode content = node.putArray("content");
        renderBlocks(cell, content, marks, depth + 1);
        if (content.size() == 0) {
            content.add(emptyParagraph());
        }
        return node;
    }

    // =====================================================================
    // Inline context: children become inline content (text, marks, img, br).
    // A block descendant is flattened to its inline content (kept valid).
    // =====================================================================

    private static void renderInline(final Element parent, final InlineRun run,
                                     final Deque<ObjectNode> marks, final int depth) {
        if (depth > MAX_DEPTH) {
            noteDepthExceeded();
            return;
        }
        for (final Node child : parent.childNodes()) {
            if (child instanceof TextNode) {
                run.addText(((TextNode) child).getWholeText(), marks);
                continue;
            }
            if (!(child instanceof Element)) {
                continue;
            }
            final Element e = (Element) child;
            final String tag = e.normalName();
            if (DROP_SUBTREE.contains(tag)) {
                continue;
            }
            if ("br".equals(tag)) {
                run.addHardBreak();
                continue;
            }
            if ("img".equals(tag)) {
                final ObjectNode img = imageNode(e, marks);
                if (img != null) {
                    run.add(img);
                }
                continue;
            }
            final ObjectNode mark = markFor(tag, e);
            if (mark != null) {
                marks.push(mark);
                renderInline(e, run, marks, depth + 1);
                marks.pop();
                continue;
            }
            // Mark tag whose attributes made it invalid (e.g. link with a rejected href), or any other
            // inline/block wrapper: transparent — keep the text, drop the wrapper.
            renderInline(e, run, marks, depth + 1);
        }
    }

    // =====================================================================
    // Nodes, marks, attributes
    // =====================================================================

    private static ObjectNode imageNode(final Element img, final Deque<ObjectNode> marks) {
        final String src = sanitizeUrl(img.attr("src"));
        if (src == null) {
            return null; // no safe source -> drop the image entirely
        }
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "dotImage");
        final ObjectNode attrs = node.putObject("attrs");
        attrs.put("src", src);
        // alt/title are free text, not URLs: stored verbatim. HTML-escaping them for an unescaped
        // render surface is the render layer's responsibility (see the class-level Sanitization note).
        putIfPresent(attrs, "alt", img.attr("alt"));
        putIfPresent(attrs, "title", img.attr("title"));
        // dotImage is an atomic inline node; it carries no marks of its own.
        return node;
    }

    /** Returns the mark for a mark-producing tag, or {@code null} if the tag is not one (or is a
     *  link whose href failed validation — in which case the anchor degrades to plain text). */
    private static ObjectNode markFor(final String tag, final Element e) {
        if (BOLD_TAGS.contains(tag)) {
            return mark("bold", null);
        }
        if (ITALIC_TAGS.contains(tag)) {
            return mark("italic", null);
        }
        if (STRIKE_TAGS.contains(tag)) {
            return mark("strike", null);
        }
        if ("code".equals(tag)) {
            return mark("code", null);
        }
        if ("a".equals(tag)) {
            final String href = sanitizeUrl(e.attr("href"));
            if (href == null) {
                return null; // unsafe/empty link -> keep the text, drop the mark
            }
            final ObjectNode attrs = MAPPER.createObjectNode();
            attrs.put("href", href);
            putIfPresent(attrs, "title", e.attr("title"));
            return mark("link", attrs);
        }
        return null;
    }

    private static boolean isMarkTag(final String tag) {
        return BOLD_TAGS.contains(tag) || ITALIC_TAGS.contains(tag) || STRIKE_TAGS.contains(tag)
                || "code".equals(tag) || "a".equals(tag);
    }

    private static boolean isHandledBlock(final String tag) {
        switch (tag) {
            case "p": case "h1": case "h2": case "h3": case "h4": case "h5": case "h6":
            case "blockquote": case "ul": case "ol": case "pre": case "hr": case "table":
                return true;
            default:
                return false;
        }
    }

    private static ObjectNode mark(final String type, final ObjectNode attrs) {
        final ObjectNode m = MAPPER.createObjectNode();
        m.put("type", type);
        if (attrs != null && attrs.size() > 0) {
            m.set("attrs", attrs);
        }
        return m;
    }

    private static ObjectNode textNode(final String text, final Deque<ObjectNode> marks) {
        final ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "text");
        n.put("text", text);
        if (marks != null && !marks.isEmpty()) {
            final ArrayNode arr = n.putArray("marks");
            // Emit outer-first: the deque head is the innermost (last pushed) mark.
            final ObjectNode[] stack = marks.toArray(new ObjectNode[0]);
            for (int i = stack.length - 1; i >= 0; i--) {
                arr.add(stack[i].deepCopy());
            }
        }
        return n;
    }

    private static ObjectNode emptyParagraph() {
        final ObjectNode p = MAPPER.createObjectNode();
        p.put("type", "paragraph");
        return p;
    }

    private static void putIfPresent(final ObjectNode attrs, final String key, final String value) {
        if (value != null && !value.isEmpty()) {
            attrs.put(key, value);
        }
    }

    private static int intAttr(final Element e, final String name, final int def,
                               final int min, final int max) {
        final String raw = e.attr(name);
        if (raw == null || raw.isEmpty()) {
            return def;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(raw.trim())));
        } catch (final NumberFormatException ex) {
            return def;
        }
    }

    private static String codeLanguage(final Element pre) {
        // Convention: <pre><code class="language-xxx">. Read it off the first code child if present.
        final Element code = pre.selectFirst("code");
        final Element source = code != null ? code : pre;
        for (final String cls : source.classNames()) {
            if (cls.startsWith("language-") && cls.length() > "language-".length()) {
                return cls.substring("language-".length());
            }
        }
        return null;
    }

    /**
     * Validate a URL for a stored {@code href}/{@code src}: reject characters that could break out of
     * an HTML attribute downstream and enforce a scheme allow-list (relative and protocol-relative
     * URLs are allowed; absolute URLs must be http/https/mailto). Returns the trimmed URL, or
     * {@code null} to reject.
     */
    private static String sanitizeUrl(final String raw) {
        if (raw == null) {
            return null;
        }
        final String url = raw.strip();
        if (url.isEmpty()) {
            return null;
        }
        // Reject control chars, whitespace (incl. tab/newline used to smuggle "java\tscript:"), and the
        // characters that would let the value escape its attribute context in an unescaped renderer.
        for (int i = 0; i < url.length(); i++) {
            final char c = url.charAt(i);
            if (c < 0x20 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '`'
                    || Character.isWhitespace(c)) {
                return null;
            }
        }
        final int colon = url.indexOf(':');
        if (colon < 0) {
            return url; // relative, fragment, protocol-relative (//host), or query — no scheme
        }
        final int slash = url.indexOf('/');
        if (slash >= 0 && slash < colon) {
            return url; // colon lives in a path segment (e.g. a/b:c) — not a scheme
        }
        final String scheme = url.substring(0, colon).toLowerCase(Locale.ROOT);
        return (scheme.equals("http") || scheme.equals("https") || scheme.equals("mailto"))
                ? url : null;
    }

    private static void noteDepthExceeded() {
        Logger.warn(TiptapHtml.class,
                "TiptapHtml: HTML nesting exceeded " + MAX_DEPTH + " levels; deeper content skipped.");
    }

    // =====================================================================
    // Inline run accumulator — turns a stream of inline pieces into a clean
    // inline content array (no empty text nodes, single spaces preserved).
    // =====================================================================

    private static final class InlineRun {

        private final ArrayNode target;
        private boolean empty = true;
        /** A single separator space seen but not yet emitted (dropped if nothing non-space follows). */
        private boolean pendingSpace = false;

        /** For a block context: content is flushed into implicit paragraphs. */
        InlineRun() {
            this.target = MAPPER.createArrayNode();
        }

        /** For an inline container: content is written straight into the container's array. */
        InlineRun(final ArrayNode target) {
            this.target = target;
        }

        void addText(final String rawIn, final Deque<ObjectNode> marks) {
            if (rawIn == null || rawIn.isEmpty()) {
                return;
            }
            final String norm = rawIn.replaceAll("\\s+", " ");
            final String core = norm.strip();
            if (core.isEmpty()) {
                // Whitespace-only: remember a pending space so inline siblings stay separated.
                if (!empty) {
                    pendingSpace = true;
                }
                return;
            }
            String text = core;
            if ((norm.charAt(0) == ' ' || pendingSpace) && !empty) {
                text = " " + text;
            }
            pendingSpace = norm.charAt(norm.length() - 1) == ' ';
            target.add(textNode(text, marks));
            empty = false;
        }

        void addHardBreak() {
            // Emitted bare, matching TiptapMarkdown's hardBreak (marks are not carried on breaks).
            final ObjectNode br = MAPPER.createObjectNode();
            br.put("type", "hardBreak");
            target.add(br);
            empty = false;
            pendingSpace = false;
        }

        void add(final ObjectNode inlineNode) {
            // Preserve a separator space that fell between text and this atomic inline node
            // (e.g. "foo <img> bar" must not become "foo[img]"). The space node is non-empty.
            if (pendingSpace && !empty) {
                target.add(textNode(" ", null));
            }
            target.add(inlineNode);
            empty = false;
            pendingSpace = false;
        }

        /** Block context: wrap accumulated inline content in a paragraph and reset. */
        void flushInto(final ArrayNode sink) {
            if (empty || target.size() == 0) {
                reset();
                return;
            }
            final ObjectNode para = MAPPER.createObjectNode();
            para.put("type", "paragraph");
            para.set("content", target.deepCopy());
            sink.add(para);
            reset();
        }

        /** Inline-container context: nothing to wrap; content already lives in the target array. */
        void finish() {
            // no-op: pending trailing space is intentionally dropped
        }

        private void reset() {
            target.removeAll();
            empty = true;
            pendingSpace = false;
        }
    }
}
