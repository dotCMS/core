package com.dotcms.tiptap;

import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bidirectional converter between Tiptap JSON (ProseMirror document model) and Markdown.
 * Functionally analogous to the @tiptap/markdown package
 * (https://github.com/ueberdosis/tiptap/tree/main/packages/markdown).
 *
 * Supported nodes: doc, paragraph, heading, blockquote, bulletList, orderedList, listItem,
 * codeBlock, horizontalRule, hardBreak, image, table, tableRow, tableHeader, tableCell.
 * Supported marks: bold, italic, strike, code, link.
 *
 * Markdown parsing uses commonmark-java with GFM tables and strikethrough extensions.
 */
public final class TiptapMarkdown {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<org.commonmark.Extension> EXTENSIONS = Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create()
    );

    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

    private TiptapMarkdown() { }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /** Parse markdown into a Tiptap JSON document node. */
    public static ObjectNode toTiptap(final String markdown) {
        final Node root = PARSER.parse(markdown == null ? "" : markdown);
        final TiptapBuilder builder = new TiptapBuilder();
        root.accept(builder);
        return builder.document();
    }

    /**
     * Serialize a Tiptap JSON document (or any fragment) to markdown.
     * Tiptap is extensible — projects routinely add custom node and mark
     * types beyond the schema this converter recognizes. Anything unknown is
     * logged once and skipped (its children are still rendered when present),
     * so the converter is safe to run against arbitrary editor content.
     */
    public static String toMarkdown(final JsonNode tiptap) {
        if (tiptap == null || tiptap.isNull()) {
            return "";
        }
        final MarkdownWriter w = new MarkdownWriter();
        w.renderNode(tiptap, null);
        return w.finish();
    }

    /** Convenience overload that parses a JSON string first. */
    public static String toMarkdown(final String tiptapJson) {
        try {
            return toMarkdown(MAPPER.readTree(tiptapJson));
        } catch (final java.io.IOException e) {
            throw new IllegalArgumentException("Invalid Tiptap JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience overload that accepts a dotCMS {@link com.dotmarketing.util.json.JSONObject}.
     * The object's {@code toString()} is parsed into a Jackson {@link JsonNode} and forwarded
     * to {@link #toMarkdown(JsonNode)}.
     */
    public static String toMarkdown(final com.dotmarketing.util.json.JSONObject tiptap) {
        if (tiptap == null) return "";
        return toMarkdown(tiptap.toString());
    }

    // =====================================================================
    // Markdown -> Tiptap JSON (commonmark Visitor)
    // =====================================================================

    private static final class TiptapBuilder extends AbstractVisitor {

        private final ObjectNode doc = MAPPER.createObjectNode();
        private final ArrayNode docContent = doc.putArray("content");
        private final Deque<ArrayNode> contentStack = new ArrayDeque<>();
        /** Active marks while we walk inline children. */
        private final Deque<ObjectNode> markStack = new ArrayDeque<>();

        TiptapBuilder() {
            doc.put("type", "doc");
            contentStack.push(docContent);
        }

        ObjectNode document() {
            return doc;
        }

        // ---- block nodes ------------------------------------------------

        @Override
        public void visit(final Document node) {
            visitChildren(node);
        }

        @Override
        public void visit(final Heading node) {
            final ObjectNode n = newNode("heading");
            n.putObject("attrs").put("level", node.getLevel());
            pushChildrenInto(n);
            visitChildren(node);
            contentStack.pop();
        }

        @Override
        public void visit(final Paragraph node) {
            // Tiptap renders bare image-only paragraphs as an image block in some schemas, but
            // ProseMirror's default schema wraps them in a paragraph too. We follow ProseMirror.
            final ObjectNode n = newNode("paragraph");
            pushChildrenInto(n);
            visitChildren(node);
            contentStack.pop();
        }

        @Override
        public void visit(final BlockQuote node) {
            final ObjectNode n = newNode("blockquote");
            pushChildrenInto(n);
            visitChildren(node);
            contentStack.pop();
        }

        @Override
        public void visit(final BulletList node) {
            final ObjectNode n = newNode("bulletList");
            pushChildrenInto(n);
            visitChildren(node);
            contentStack.pop();
        }

        @Override
        public void visit(final OrderedList node) {
            final ObjectNode n = newNode("orderedList");
            final ObjectNode attrs = n.putObject("attrs");
            attrs.put("start", node.getStartNumber());
            pushChildrenInto(n);
            visitChildren(node);
            contentStack.pop();
        }

        @Override
        public void visit(final ListItem node) {
            final ObjectNode n = newNode("listItem");
            pushChildrenInto(n);
            visitChildren(node);
            contentStack.pop();
        }

        @Override
        public void visit(final FencedCodeBlock node) {
            final ObjectNode n = newNode("codeBlock");
            final String info = node.getInfo();
            if (info != null && !info.isEmpty()) {
                n.putObject("attrs").put("language", info);
            }
            final ArrayNode arr = n.putArray("content");
            final String text = stripTrailingNewline(node.getLiteral());
            if (!text.isEmpty()) {
                arr.add(textNode(text));
            }
        }

        @Override
        public void visit(final IndentedCodeBlock node) {
            final ObjectNode n = newNode("codeBlock");
            final ArrayNode arr = n.putArray("content");
            final String text = stripTrailingNewline(node.getLiteral());
            if (!text.isEmpty()) {
                arr.add(textNode(text));
            }
        }

        @Override
        public void visit(final ThematicBreak node) {
            newNode("horizontalRule");
        }

        @Override
        public void visit(final HtmlBlock node) {
            // Preserve raw HTML inside a paragraph; Tiptap can render it via raw nodes.
            final ObjectNode p = newNode("paragraph");
            p.putArray("content").add(textNode(node.getLiteral()));
        }

        // ---- inline nodes -----------------------------------------------

        @Override
        public void visit(final Text node) {
            // Commonmark occasionally produces empty Text tokens (e.g. after consuming a link
            // closer); these add no content and only cause spurious round-trip differences.
            final String literal = node.getLiteral();
            if (literal == null || literal.isEmpty()) return;
            currentContent().add(textNode(literal));
        }

        @Override
        public void visit(final SoftLineBreak node) {
            // ProseMirror represents soft breaks as a space inside inline content.
            currentContent().add(textNode(" "));
        }

        @Override
        public void visit(final HardLineBreak node) {
            final ObjectNode br = MAPPER.createObjectNode();
            br.put("type", "hardBreak");
            currentContent().add(br);
        }

        @Override
        public void visit(final Emphasis node) {
            pushMark("italic", null);
            visitChildren(node);
            markStack.pop();
        }

        @Override
        public void visit(final StrongEmphasis node) {
            pushMark("bold", null);
            visitChildren(node);
            markStack.pop();
        }

        @Override
        public void visit(final Code node) {
            // Inline code: emit a text node with a `code` mark — content not visited further.
            final ObjectNode txt = textNode(node.getLiteral());
            final ArrayNode marks = txt.putArray("marks");
            for (final ObjectNode m : markStack) {
                marks.add(m.deepCopy());
            }
            final ObjectNode codeMark = MAPPER.createObjectNode();
            codeMark.put("type", "code");
            marks.add(codeMark);
            currentContent().add(txt);
        }

        @Override
        public void visit(final Link node) {
            final ObjectNode attrs = MAPPER.createObjectNode();
            attrs.put("href", emptyToNull(node.getDestination()));
            if (node.getTitle() != null && !node.getTitle().isEmpty()) {
                attrs.put("title", node.getTitle());
            }
            pushMark("link", attrs);
            visitChildren(node);
            markStack.pop();
        }

        @Override
        public void visit(final Image node) {
            final ObjectNode img = MAPPER.createObjectNode();
            img.put("type", "image");
            final ObjectNode attrs = img.putObject("attrs");
            attrs.put("src", emptyToNull(node.getDestination()));
            // Alt text is the textual content of the Image node's children.
            final String alt = collectText(node);
            if (!alt.isEmpty()) {
                attrs.put("alt", alt);
            }
            if (node.getTitle() != null && !node.getTitle().isEmpty()) {
                attrs.put("title", node.getTitle());
            }
            currentContent().add(img);
        }

        // ---- extensions: strikethrough, tables --------------------------

        @Override
        public void visit(final org.commonmark.node.CustomNode node) {
            if (node instanceof Strikethrough) {
                pushMark("strike", null);
                visitChildren(node);
                markStack.pop();
                return;
            }
            super.visit(node);
        }

        @Override
        public void visit(final org.commonmark.node.CustomBlock node) {
            if (node instanceof TableBlock) {
                emitTable((TableBlock) node);
                return;
            }
            super.visit(node);
        }

        private void emitTable(final TableBlock table) {
            final ObjectNode tableNode = newNode("table");
            pushChildrenInto(tableNode);
            for (Node section = table.getFirstChild(); section != null; section = section.getNext()) {
                final boolean isHead = section instanceof TableHead;
                for (Node row = section.getFirstChild(); row != null; row = row.getNext()) {
                    if (row instanceof TableRow) {
                        emitRow((TableRow) row, isHead);
                    }
                }
            }
            contentStack.pop();
        }

        private void emitRow(final TableRow row, final boolean headerRow) {
            final ObjectNode rowNode = newNode("tableRow");
            pushChildrenInto(rowNode);
            for (Node c = row.getFirstChild(); c != null; c = c.getNext()) {
                if (c instanceof TableCell) {
                    emitCell((TableCell) c, headerRow);
                }
            }
            contentStack.pop();
        }

        private void emitCell(final TableCell cell, final boolean headerRow) {
            final boolean asHeader = headerRow || cell.isHeader();
            final ObjectNode cellNode = newNode(asHeader ? "tableHeader" : "tableCell");
            final ObjectNode attrs = cellNode.putObject("attrs");
            attrs.put("colspan", 1);
            attrs.put("rowspan", 1);
            attrs.putNull("colwidth");
            // Wrap the cell's inline content in a paragraph as ProseMirror table schema requires.
            final ArrayNode cellContent = cellNode.putArray("content");
            final ObjectNode para = MAPPER.createObjectNode();
            para.put("type", "paragraph");
            final ArrayNode paraContent = para.putArray("content");
            cellContent.add(para);

            contentStack.push(paraContent);
            visitChildren(cell);
            contentStack.pop();

            if (paraContent.size() == 0) {
                para.remove("content");
            }
        }

        // ---- helpers ----------------------------------------------------

        private ObjectNode newNode(final String type) {
            final ObjectNode n = MAPPER.createObjectNode();
            n.put("type", type);
            currentContent().add(n);
            return n;
        }

        private ArrayNode currentContent() {
            return contentStack.peek();
        }

        private void pushChildrenInto(final ObjectNode parent) {
            contentStack.push(parent.putArray("content"));
        }

        private void pushMark(final String type, final ObjectNode attrs) {
            final ObjectNode mark = MAPPER.createObjectNode();
            mark.put("type", type);
            if (attrs != null && attrs.size() > 0) {
                mark.set("attrs", attrs);
            }
            markStack.push(mark);
        }

        private ObjectNode textNode(final String text) {
            final ObjectNode n = MAPPER.createObjectNode();
            n.put("type", "text");
            n.put("text", text);
            if (!markStack.isEmpty()) {
                final ArrayNode marks = n.putArray("marks");
                // markStack iterates head (top) first; outer marks were pushed first (at bottom).
                // Order doesn't affect rendering semantics but we emit outer-first for readability.
                final ObjectNode[] arr = markStack.toArray(new ObjectNode[0]);
                for (int i = arr.length - 1; i >= 0; i--) {
                    marks.add(arr[i].deepCopy());
                }
            }
            return n;
        }

        private static String collectText(final Node n) {
            final StringBuilder sb = new StringBuilder();
            n.accept(new AbstractVisitor() {
                @Override public void visit(final Text t) { sb.append(t.getLiteral()); }
                @Override public void visit(final Code c) { sb.append(c.getLiteral()); }
            });
            return sb.toString();
        }

        private static String emptyToNull(final String s) {
            return (s == null || s.isEmpty()) ? null : s;
        }

        private static String stripTrailingNewline(final String s) {
            if (s == null) return "";
            int end = s.length();
            while (end > 0 && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')) end--;
            return s.substring(0, end);
        }
    }

    // =====================================================================
    // Tiptap JSON -> Markdown
    // =====================================================================

    private static final class MarkdownWriter {

        private final StringBuilder out = new StringBuilder();
        private int listDepth = 0;
        /** Stack of list contexts: each entry is {kind: "bullet"|"ordered", index, start}. */
        private final Deque<ListCtx> listStack = new ArrayDeque<>();
        private final Deque<String> blockPrefix = new ArrayDeque<>();
        /** Unknown node/mark types we've already logged this conversion — log once each. */
        private final Set<String> loggedUnknown = new HashSet<>();

        private void noteUnknown(final String kind, final String type) {
            if (type == null || type.isEmpty()) return;
            final String key = kind + ":" + type;
            if (loggedUnknown.add(key)) {
                Logger.info(TiptapMarkdown.class,
                        "TiptapMarkdown: skipping unsupported " + kind + " type '" + type + "'");
            }
        }

        String finish() {
            // collapse trailing blank lines down to a single newline
            while (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
                out.deleteCharAt(out.length() - 1);
            }
            return out.toString();
        }

        // ----- block dispatch -------------------------------------------

        void renderNode(final JsonNode node, final JsonNode parent) {
            if (node == null) return;
            final String type = node.path("type").asText("");
            switch (type) {
                case "doc":
                    renderBlockChildren(node);
                    break;
                case "paragraph":
                    emitBlock(renderInline(node.path("content")));
                    break;
                case "heading":
                    final int level = Math.max(1, Math.min(6, node.path("attrs").path("level").asInt(1)));
                    emitBlock(repeat('#', level) + " " + renderInline(node.path("content")));
                    break;
                case "blockquote":
                    renderBlockquote(node);
                    break;
                case "bulletList":
                    renderList(node, false);
                    break;
                case "orderedList":
                    renderList(node, true);
                    break;
                case "listItem":
                    // Shouldn't be hit directly; handled inside renderList.
                    renderBlockChildren(node);
                    break;
                case "codeBlock":
                    renderCodeBlock(node);
                    break;
                case "horizontalRule":
                    emitBlock("---");
                    break;
                case "hardBreak":
                    out.append("  \n");
                    break;
                case "image":
                case "dotImage":
                    emitBlock(renderImage(node));
                    break;
                case "youtube": {
                    final String src = node.path("attrs").path("src").asText("");
                    if (!src.isEmpty()) {
                        emitBlock("[" + src + "](" + src + ")");
                    }
                    break;
                }
                case "table":
                    renderTable(node);
                    break;
                case "text":
                    out.append(escapeText(node.path("text").asText(""), false));
                    break;
                default:
                    // Unknown node — log once and render children if present, else drop.
                    noteUnknown("node", type);
                    if (node.has("content")) {
                        renderBlockChildren(node);
                    }
            }
        }

        private void renderBlockChildren(final JsonNode node) {
            final JsonNode content = node.path("content");
            if (!content.isArray()) return;
            for (final JsonNode child : content) {
                renderNode(child, node);
            }
        }

        private void emitBlock(final String text) {
            // Ensure block separation: blank line between blocks at top level / inside blockquotes,
            // newline only inside list items where the caller manages indentation.
            applyPrefix(text);
            ensureBlankLine();
        }

        private void applyPrefix(final String text) {
            if (blockPrefix.isEmpty()) {
                out.append(text);
                return;
            }
            // Combined prefix is the concatenation of the stack (outer-first).
            final StringBuilder pfx = new StringBuilder();
            // Iterate from bottom (outermost) to top (innermost).
            final String[] frames = blockPrefix.toArray(new String[0]);
            for (int i = frames.length - 1; i >= 0; i--) {
                pfx.append(frames[i]);
            }
            final String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) out.append('\n');
                out.append(pfx).append(lines[i]);
            }
        }

        private void ensureBlankLine() {
            if (out.length() == 0) return;
            // collapse to exactly one blank line between blocks
            if (out.charAt(out.length() - 1) != '\n') out.append('\n');
            // Inside a list item we want a single \n between successive blocks, not a blank line.
            // The caller (renderList) controls blank-line semantics for top-level / blockquote.
            out.append('\n');
        }

        // ----- specific block renderers ---------------------------------

        private void renderBlockquote(final JsonNode node) {
            blockPrefix.push("> ");
            try {
                final JsonNode content = node.path("content");
                if (content.isArray()) {
                    final Iterator<JsonNode> it = content.elements();
                    while (it.hasNext()) {
                        renderNode(it.next(), node);
                    }
                }
            } finally {
                blockPrefix.pop();
            }
        }

        private void renderList(final JsonNode node, final boolean ordered) {
            final JsonNode items = node.path("content");
            if (!items.isArray() || items.size() == 0) return;
            final int start = ordered ? node.path("attrs").path("start").asInt(1) : 1;
            listDepth++;
            int idx = 0;
            for (final JsonNode item : items) {
                final String marker = ordered ? ((start + idx) + ". ") : "- ";
                renderListItem(item, marker);
                idx++;
            }
            listDepth--;
            // List acts as a block — ensure trailing blank line at top level.
            if (listDepth == 0) ensureBlankLine();
        }

        private void renderListItem(final JsonNode item, final String marker) {
            // Render each child block; first child gets the marker, others get hanging indent.
            final JsonNode content = item.path("content");
            if (!content.isArray() || content.size() == 0) {
                applyPrefix(marker);
                out.append('\n');
                return;
            }
            final String indent = repeatStr(" ", marker.length());

            // Capture each child block into a String so we can prefix it.
            int i = 0;
            for (final JsonNode child : content) {
                final String rendered = captureChild(child, item);
                final String[] lines = rendered.split("\n", -1);
                for (int li = 0; li < lines.length; li++) {
                    final String prefix = (i == 0 && li == 0) ? marker : indent;
                    if (li > 0) out.append('\n');
                    // Suppress completely-blank trailing line that would otherwise add a stray indent.
                    if (li == lines.length - 1 && lines[li].isEmpty()) {
                        // Don't emit a prefix-only blank line.
                        continue;
                    }
                    applyPrefix(prefix + lines[li]);
                }
                out.append('\n');
                if (i < content.size() - 1) out.append('\n'); // blank line between blocks in a list item
                i++;
            }
        }

        /** Render a child block into a string without touching the main buffer (no surrounding blank lines). */
        private String captureChild(final JsonNode child, final JsonNode parent) {
            final StringBuilder saved = new StringBuilder(out);
            out.setLength(0);
            // Temporarily clear blockPrefix because the caller will prefix.
            final Deque<String> savedPfx = new ArrayDeque<>(blockPrefix);
            blockPrefix.clear();
            try {
                renderNode(child, parent);
                // Trim trailing newlines from the captured block.
                while (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
                    out.deleteCharAt(out.length() - 1);
                }
                return out.toString();
            } finally {
                out.setLength(0);
                out.append(saved);
                blockPrefix.clear();
                for (final Iterator<String> it = savedPfx.descendingIterator(); it.hasNext(); ) {
                    blockPrefix.push(it.next());
                }
            }
        }

        private void renderCodeBlock(final JsonNode node) {
            final String lang = node.path("attrs").path("language").asText("");
            final StringBuilder body = new StringBuilder();
            for (final JsonNode child : node.path("content")) {
                if ("text".equals(child.path("type").asText(""))) {
                    body.append(child.path("text").asText(""));
                }
            }
            final String fence = pickFence(body.toString());
            final StringBuilder sb = new StringBuilder();
            sb.append(fence);
            if (!lang.isEmpty()) sb.append(lang);
            sb.append('\n').append(body);
            if (body.length() == 0 || body.charAt(body.length() - 1) != '\n') sb.append('\n');
            sb.append(fence);
            emitBlock(sb.toString());
        }

        private String pickFence(final String body) {
            // Use a longer fence if the body itself contains triple backticks.
            int max = 2;
            int run = 0;
            for (int i = 0; i < body.length(); i++) {
                if (body.charAt(i) == '`') {
                    run++;
                    if (run > max) max = run;
                } else {
                    run = 0;
                }
            }
            return repeat('`', Math.max(3, max + 1));
        }

        private String renderImage(final JsonNode node) {
            final JsonNode attrs = node.path("attrs");
            final String alt = attrs.path("alt").asText("");
            final String src = attrs.path("src").asText("");
            final String title = attrs.path("title").asText("");
            final StringBuilder sb = new StringBuilder();
            sb.append("![").append(escapeLinkText(alt)).append("](").append(src);
            if (!title.isEmpty()) sb.append(" \"").append(title.replace("\"", "\\\"")).append('"');
            sb.append(')');
            return sb.toString();
        }

        private void renderTable(final JsonNode node) {
            // Tiptap table: rows of tableHeader/tableCell. First row is the header in GFM tables.
            final JsonNode rows = node.path("content");
            if (!rows.isArray() || rows.size() == 0) return;

            final List<List<String>> cells = new ArrayList<>();
            int maxCols = 0;
            for (final JsonNode row : rows) {
                final List<String> rowCells = new ArrayList<>();
                for (final JsonNode cell : row.path("content")) {
                    rowCells.add(renderCellInline(cell));
                }
                cells.add(rowCells);
                if (rowCells.size() > maxCols) maxCols = rowCells.size();
            }

            // Compute column widths for alignment (purely cosmetic).
            final int[] widths = new int[maxCols];
            for (final List<String> r : cells) {
                for (int c = 0; c < r.size(); c++) {
                    widths[c] = Math.max(widths[c], r.get(c).length());
                }
            }
            for (int c = 0; c < widths.length; c++) widths[c] = Math.max(3, widths[c]);

            final StringBuilder sb = new StringBuilder();
            // header row
            sb.append(buildRow(cells.get(0), widths)).append('\n');
            // separator
            sb.append('|');
            for (int c = 0; c < maxCols; c++) sb.append(' ').append(repeat('-', widths[c])).append(" |");
            sb.append('\n');
            // body rows
            for (int r = 1; r < cells.size(); r++) {
                sb.append(buildRow(cells.get(r), widths));
                if (r < cells.size() - 1) sb.append('\n');
            }
            emitBlock(sb.toString());
        }

        private String buildRow(final List<String> rowCells, final int[] widths) {
            final StringBuilder sb = new StringBuilder("|");
            for (int c = 0; c < widths.length; c++) {
                final String v = c < rowCells.size() ? rowCells.get(c) : "";
                sb.append(' ').append(padRight(v, widths[c])).append(" |");
            }
            return sb.toString();
        }

        private String renderCellInline(final JsonNode cell) {
            // Cell contains paragraph(s) of inline content; render and replace newlines with <br>.
            final StringBuilder sb = new StringBuilder();
            for (final JsonNode block : cell.path("content")) {
                if ("paragraph".equals(block.path("type").asText(""))) {
                    if (sb.length() > 0) sb.append("<br>");
                    sb.append(renderInline(block.path("content")));
                }
            }
            // Pipes are already escaped by escapeText() during inline rendering.
            return sb.toString();
        }

        // ----- inline rendering with mark tracking ----------------------

        private String renderInline(final JsonNode nodes) {
            if (nodes == null || !nodes.isArray()) return "";
            final StringBuilder sb = new StringBuilder();
            final List<String> active = new ArrayList<>();
            final Map<String, ObjectNode> activeAttrs = new LinkedHashMap<>();

            for (int i = 0; i < nodes.size(); i++) {
                final JsonNode n = nodes.get(i);
                final String type = n.path("type").asText("");
                if ("text".equals(type)) {
                    final List<String> wanted = new ArrayList<>();
                    final Map<String, ObjectNode> wantedAttrs = new LinkedHashMap<>();
                    final JsonNode marks = n.path("marks");
                    if (marks.isArray()) {
                        // Render order: link (outermost) > bold > italic > strike > code (innermost).
                        // Marks that don't render in markdown (e.g. underline) are skipped entirely
                        // so they don't disturb the open/close bookkeeping. Log unknown marks once.
                        final List<JsonNode> sorted = new ArrayList<>();
                        marks.forEach(m -> {
                            final String mt = m.path("type").asText("");
                            if (rendersInMarkdown(mt)) {
                                sorted.add(m);
                            } else if (!isSilentlyDroppedMark(mt)) {
                                noteUnknown("mark", mt);
                            }
                        });
                        sorted.sort((a, b) -> rank(a.path("type").asText("")) - rank(b.path("type").asText("")));
                        for (final JsonNode m : sorted) {
                            final String mt = m.path("type").asText("");
                            wanted.add(mt);
                            wantedAttrs.put(mt, m.has("attrs") && m.get("attrs").isObject()
                                    ? (ObjectNode) m.get("attrs") : null);
                        }
                    }
                    // Close marks that aren't wanted, in LIFO order — but first lift any trailing
                    // whitespace out of the marked span. Markdown emphasis cannot close after a
                    // space (`*x *` is not valid italic-close), so leaving it inline would cause
                    // the reader to scan past for a real closer and apply emphasis twice.
                    final String trailingWs = extractTrailingWhitespace(sb);
                    closeMarksDownTo(sb, active, activeAttrs, wanted, wantedAttrs);
                    if (!trailingWs.isEmpty()) sb.append(trailingWs);

                    // For opening: leading whitespace in the incoming text must be emitted
                    // BEFORE the opening delimiter for the same reason (`* x*` won't open).
                    // Code escaping is determined by whether the OUTGOING text will be inside
                    // code marks (either still-active or about-to-open), not by current state.
                    final boolean willBeInCode = wanted.contains("code");
                    String incoming = escapeText(n.path("text").asText(""), willBeInCode);
                    final String leadingWs = leadingWhitespace(incoming);
                    if (!leadingWs.isEmpty()) {
                        sb.append(leadingWs);
                        incoming = incoming.substring(leadingWs.length());
                    }
                    openMarksUpTo(sb, active, activeAttrs, wanted, wantedAttrs);
                    sb.append(incoming);
                } else if ("hardBreak".equals(type)) {
                    final String tws = extractTrailingWhitespace(sb);
                    closeAllMarks(sb, active, activeAttrs);
                    if (!tws.isEmpty()) sb.append(tws);
                    sb.append("  \n");
                } else if ("image".equals(type) || "dotImage".equals(type)) {
                    final String tws = extractTrailingWhitespace(sb);
                    closeAllMarks(sb, active, activeAttrs);
                    if (!tws.isEmpty()) sb.append(tws);
                    sb.append(renderImage(n));
                } else if ("youtube".equals(type)) {
                    // No native markdown for YouTube — emit a plain link to the video.
                    final String tws = extractTrailingWhitespace(sb);
                    closeAllMarks(sb, active, activeAttrs);
                    if (!tws.isEmpty()) sb.append(tws);
                    final String src = n.path("attrs").path("src").asText("");
                    if (!src.isEmpty()) sb.append('[').append(src).append("](").append(src).append(')');
                } else {
                    // Unknown inline node — log once and drop, after closing any active marks.
                    noteUnknown("inline node", type);
                    final String tws = extractTrailingWhitespace(sb);
                    closeAllMarks(sb, active, activeAttrs);
                    if (!tws.isEmpty()) sb.append(tws);
                }
            }
            final String finalTws = extractTrailingWhitespace(sb);
            closeAllMarks(sb, active, activeAttrs);
            if (!finalTws.isEmpty()) sb.append(finalTws);
            return sb.toString();
        }

        /** Only marks with real markdown syntax participate in open/close tracking. */
        private static boolean rendersInMarkdown(final String type) {
            switch (type) {
                case "bold": case "italic": case "strike": case "code": case "link":
                    return true;
                default:
                    return false; // underline, highlight, etc. have no markdown — drop silently
            }
        }

        /**
         * Known marks with no markdown representation that we deliberately drop
         * silently — distinct from arbitrary user-defined marks, which should be
         * logged so operators see them.
         */
        private static boolean isSilentlyDroppedMark(final String type) {
            switch (type) {
                case "underline": case "highlight": case "subscript": case "superscript":
                case "textStyle": case "color":
                    return true;
                default:
                    return false;
            }
        }

        /** Pop and return any trailing whitespace currently in the buffer (may be empty). */
        private static String extractTrailingWhitespace(final StringBuilder sb) {
            int end = sb.length();
            while (end > 0) {
                final char c = sb.charAt(end - 1);
                if (c == ' ' || c == '\t') end--;
                else break;
            }
            final String ws = sb.substring(end);
            if (!ws.isEmpty()) sb.setLength(end);
            return ws;
        }

        private static String leadingWhitespace(final String s) {
            int i = 0;
            while (i < s.length()) {
                final char c = s.charAt(i);
                if (c == ' ' || c == '\t') i++;
                else break;
            }
            return s.substring(0, i);
        }

        private static int rank(final String mark) {
            switch (mark) {
                case "link":   return 0;
                case "bold":   return 1;
                case "italic": return 2;
                case "strike": return 3;
                case "code":   return 4;
                default:       return 5;
            }
        }

        private void closeMarksDownTo(final StringBuilder sb,
                                      final List<String> active, final Map<String, ObjectNode> activeAttrs,
                                      final List<String> wanted, final Map<String, ObjectNode> wantedAttrs) {
            // Walk the active stack from the innermost outward and close anything that
            // either isn't in `wanted` or whose attrs changed (e.g. different link href).
            while (!active.isEmpty()) {
                final String top = active.get(active.size() - 1);
                final boolean stillWanted = wanted.contains(top)
                        && sameAttrs(activeAttrs.get(top), wantedAttrs.get(top));
                if (!stillWanted) {
                    sb.append(closeMark(top, activeAttrs.get(top)));
                    active.remove(active.size() - 1);
                    activeAttrs.remove(top);
                } else {
                    // If the top is wanted but a mark *below* it differs, we must still close
                    // the top to get to the inner mismatch. Detect that by checking the rest.
                    final List<String> below = active.subList(0, active.size() - 1);
                    boolean innerMismatch = false;
                    for (final String m : below) {
                        if (!wanted.contains(m) || !sameAttrs(activeAttrs.get(m), wantedAttrs.get(m))) {
                            innerMismatch = true; break;
                        }
                    }
                    if (innerMismatch) {
                        sb.append(closeMark(top, activeAttrs.get(top)));
                        active.remove(active.size() - 1);
                        activeAttrs.remove(top);
                    } else {
                        break;
                    }
                }
            }
        }

        private void openMarksUpTo(final StringBuilder sb,
                                   final List<String> active, final Map<String, ObjectNode> activeAttrs,
                                   final List<String> wanted, final Map<String, ObjectNode> wantedAttrs) {
            for (final String m : wanted) {
                if (active.contains(m)) continue;
                sb.append(openMark(m, wantedAttrs.get(m)));
                active.add(m);
                activeAttrs.put(m, wantedAttrs.get(m));
            }
        }

        private void closeAllMarks(final StringBuilder sb, final List<String> active,
                                   final Map<String, ObjectNode> activeAttrs) {
            for (int i = active.size() - 1; i >= 0; i--) {
                sb.append(closeMark(active.get(i), activeAttrs.get(active.get(i))));
            }
            active.clear();
            activeAttrs.clear();
        }

        private static String openMark(final String type, final ObjectNode attrs) {
            switch (type) {
                case "bold":   return "**";
                case "italic": return "*";
                case "strike": return "~~";
                case "code":   return "`";
                case "link":   return "[";
                default:       return "";
            }
        }

        private static String closeMark(final String type, final ObjectNode attrs) {
            switch (type) {
                case "bold":   return "**";
                case "italic": return "*";
                case "strike": return "~~";
                case "code":   return "`";
                case "link":
                    final String href = attrs == null ? "" : attrs.path("href").asText("");
                    final String title = attrs == null ? "" : attrs.path("title").asText("");
                    final StringBuilder sb = new StringBuilder("](").append(href);
                    if (!title.isEmpty()) sb.append(" \"").append(title.replace("\"", "\\\"")).append('"');
                    sb.append(')');
                    return sb.toString();
                default:
                    return "";
            }
        }

        private static boolean sameAttrs(final ObjectNode a, final ObjectNode b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }

        // ----- escaping --------------------------------------------------

        private static String escapeText(final String s, final boolean inCode) {
            if (inCode) return s; // literal inside ` ... `
            final StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                final char c = s.charAt(i);
                switch (c) {
                    case '\\': case '`': case '*': case '_':
                    case '{': case '}': case '[': case ']':
                    case '(': case ')': case '#': case '+':
                    case '-': case '!': case '|': case '<': case '>':
                        sb.append('\\').append(c); break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }

        private static String escapeLinkText(final String s) {
            return s.replace("[", "\\[").replace("]", "\\]");
        }

        // ----- string utils ---------------------------------------------

        private static String repeat(final char c, final int n) {
            final char[] a = new char[Math.max(0, n)];
            Arrays.fill(a, c);
            return new String(a);
        }

        private static String repeatStr(final String s, final int n) {
            final StringBuilder sb = new StringBuilder(s.length() * Math.max(0, n));
            for (int i = 0; i < n; i++) sb.append(s);
            return sb.toString();
        }

        private static String padRight(final String s, final int width) {
            if (s.length() >= width) return s;
            return s + repeat(' ', width - s.length());
        }

        // ----- list context ---------------------------------------------

        private static final class ListCtx {
            final String kind; int idx; final int start;
            ListCtx(final String kind, final int start) { this.kind = kind; this.start = start; this.idx = start; }
        }
    }
}
