import { Injector } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Editor, getSchema } from '@tiptap/core';
import Link from '@tiptap/extension-link';
import { Node as PMNode } from '@tiptap/pm/model';
import StarterKit from '@tiptap/starter-kit';

import { EditorPopoverService } from '../services/editor-popover.service';
import {
    createTestEditor,
    docText,
    hasEmojiNode,
    inlineNodes,
    pasteHTML,
    pasteText,
    placeCursor,
    typeText
} from '../testing/editor.testing';
import {
    AFFECTED_CHARACTER_SAMPLE,
    HREF,
    REPORTED_PAYLOAD,
    REPORTED_SYMBOLS
} from '../testing/emoji.fixtures';

/**
 * #37340 — the Block Editor must never create an `emoji` node again.
 *
 * The upstream extension creates them from five independent paths (research.md R1). Four are
 * authoring rules; the fifth is `parseHTML`, which is not a rule but mints nodes just the same when
 * emoji HTML is pasted between fields. Closing only the one the issue names leaves `:copyright:`,
 * `:)` and copy-paste still producing the defect.
 *
 * Every assertion here is on the DOCUMENT, not the DOM. The editor renders a mark run as one
 * element regardless of how many nodes span it, which is exactly how the original ProseMirror
 * assumption survived three drafts (research.md R10).
 */
describe('DotEmoji — nothing creates an emoji node (#37340)', () => {
    let injector: Injector;
    let editor: Editor;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [{ provide: EditorPopoverService, useValue: {} }]
        });
        injector = TestBed.inject(Injector);
    });

    afterEach(() => {
        editor?.destroy();
        TestBed.resetTestingModule();
    });

    const linkedParagraph = (text: string) => ({
        type: 'doc',
        content: [
            {
                type: 'paragraph',
                content: [
                    {
                        type: 'text',
                        marks: [{ type: 'link', attrs: { href: HREF } }],
                        text
                    }
                ]
            }
        ]
    });

    describe('AC-001 — the reported reproduction', () => {
        it('typing © inside linked text leaves ONE text node carrying ONE link mark', () => {
            editor = createTestEditor(injector, {
                content: linkedParagraph('dotCMS Copyright All rights reserved')
            });

            // Between "Copyright " and "All" — position 1 is the paragraph's first inline offset.
            placeCursor(editor, 1 + 'dotCMS Copyright '.length);
            typeText(editor, '©');

            expect(hasEmojiNode(editor)).toBe(false);

            const nodes = inlineNodes(editor);
            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('text');
            expect(nodes[0].text).toContain('©');
            expect(nodes[0].marks?.map((m) => m.type)).toEqual(['link']);
        });
    });

    describe('AC-002 — the whole affected class, not three literals', () => {
        it.each(AFFECTED_CHARACTER_SAMPLE)(
            'typing %s into paragraph text creates no emoji node',
            (char) => {
                editor = createTestEditor(injector);
                typeText(editor, `before ${char} after`);

                expect(hasEmojiNode(editor)).toBe(false);
                expect(docText(editor)).toContain(char);
            }
        );

        it.each(AFFECTED_CHARACTER_SAMPLE)(
            'pasting %s as plain text creates no emoji node',
            (char) => {
                editor = createTestEditor(injector);
                pasteText(editor, `before ${char} after`);

                expect(hasEmojiNode(editor)).toBe(false);
                expect(docText(editor)).toContain(char);
            }
        );
    });

    describe('AC-003 — start and end of linked text, not only mid-run', () => {
        it.each(REPORTED_SYMBOLS)('%s at the START of a link keeps one text node', (char) => {
            editor = createTestEditor(injector, { content: linkedParagraph('dotCMS Copyright') });
            placeCursor(editor, 1);
            typeText(editor, char);

            expect(hasEmojiNode(editor)).toBe(false);
            expect(inlineNodes(editor)).toHaveLength(1);
        });

        it.each(REPORTED_SYMBOLS)('%s at the END of a link keeps one text node', (char) => {
            editor = createTestEditor(injector, { content: linkedParagraph('dotCMS Copyright') });
            placeCursor(editor, 1 + 'dotCMS Copyright'.length);
            typeText(editor, char);

            expect(hasEmojiNode(editor)).toBe(false);
            expect(inlineNodes(editor)).toHaveLength(1);
        });
    });

    describe('AC-004 — every paste shape', () => {
        it('plain-text paste of © creates no node', () => {
            editor = createTestEditor(injector);
            pasteText(editor, 'dotCMS © 2026');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toContain('©');
        });

        it('HTML paste of © creates no node', () => {
            editor = createTestEditor(injector);
            pasteHTML(editor, '<p>dotCMS © 2026</p>');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toContain('©');
        });

        it('HTML-entity paste of &copy; creates no node', () => {
            editor = createTestEditor(injector);
            pasteHTML(editor, '<p>dotCMS &copy; 2026</p>');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toContain('©');
        });
    });

    describe('AC-005 — the toolbar picker', () => {
        /**
         * The picker already inserts `emoji.native`, a literal character
         * (`emoji-picker.component.ts:48`). It only ever produced nodes because the conversion
         * turned that character straight back into one, so this asserts the picker's own call.
         */
        it('inserting the native character inside a link yields one text node with the link mark', () => {
            editor = createTestEditor(injector, {
                content: linkedParagraph('dotCMS Copyright All rights reserved')
            });

            placeCursor(editor, 1 + 'dotCMS Copyright '.length);
            editor.chain().focus().insertContent('🚀').run();

            expect(hasEmojiNode(editor)).toBe(false);

            const nodes = inlineNodes(editor);
            expect(nodes).toHaveLength(1);
            expect(nodes[0].marks?.map((m) => m.type)).toEqual(['link']);
            expect(nodes[0].text).toContain('🚀');
        });
    });

    describe('AC-006 — :shortcode: entry', () => {
        it('typing :copyright: inserts © as text', () => {
            editor = createTestEditor(injector);
            typeText(editor, ':copyright:');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toBe('©');
        });

        it('pasting :copyright: inserts © as text', () => {
            editor = createTestEditor(injector);
            pasteText(editor, ':copyright:');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toContain('©');
        });

        it('an unknown shortcode is left exactly as typed', () => {
            editor = createTestEditor(injector);
            typeText(editor, ':not_a_real_shortcode:');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toBe(':not_a_real_shortcode:');
        });

        it('typing :copyright: inside a link keeps one text node with the link mark', () => {
            editor = createTestEditor(injector, { content: linkedParagraph('dotCMS  2026') });
            placeCursor(editor, 1 + 'dotCMS '.length);
            typeText(editor, ':copyright:');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(inlineNodes(editor)).toHaveLength(1);
            expect(inlineNodes(editor)[0].marks?.map((m) => m.type)).toEqual(['link']);
        });
    });

    describe('AC-007 — :) emoticons', () => {
        it('typing ":) " inserts the character as text', () => {
            editor = createTestEditor(injector);
            typeText(editor, ':) ');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toContain('🙂');
        });

        /**
         * Upstream's emoticon regex captures the trailing space and swallows it. An author typing
         * "hi :) there" should not silently lose the space they typed.
         */
        it('preserves the whitespace the author typed around the emoticon', () => {
            editor = createTestEditor(injector);
            typeText(editor, 'hi :) there');

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toBe('hi 🙂 there');
        });
    });

    describe('AC-011 — no image fallback for new content', () => {
        /**
         * jsdom's `isEmojiSupported()` is always false, so the extension's `fallbackImage` path is
         * the DEFAULT here rather than an edge case (research.md R10, T004). If a node were still
         * being created, this assertion would catch it as a `cdn.jsdelivr.net` image embed.
         */
        it('emits no <img alt="… emoji"> for newly authored content', () => {
            editor = createTestEditor(injector);
            typeText(editor, 'dotCMS © 2026');

            const html = editor.getHTML();
            expect(html).not.toContain('emoji"');
            expect(html).not.toContain('cdn.jsdelivr.net');
            expect(html).toContain('©');
        });
    });

    describe('AC-012 — HTML paste cannot mint a node', () => {
        it('pasting an emoji span carrying the character lands as text', () => {
            editor = createTestEditor(injector);
            pasteHTML(editor, 
                '<p><span data-type="emoji" data-name="copyright">©</span></p>'
            );

            expect(hasEmojiNode(editor)).toBe(false);
            expect(docText(editor)).toContain('©');
        });

        /**
         * The shape that makes two defenses necessary. With `parseHTML` neutralized and nothing
         * else, the span is skipped and its inner `<img>` is left exposed for `DotImage` to claim
         * as a dotCMS image node pointing at a third-party CDN — worse than the bug being fixed
         * (research.md R3, confirmed live in R10/T004).
         */
        it('pasting the fallbackImage span lands as text, NOT as a dotCMS image', () => {
            editor = createTestEditor(injector);
            pasteHTML(editor, 
                '<p><span data-type="emoji" data-name="copyright">' +
                    '<img src="https://cdn.jsdelivr.net/npm/emoji-datasource-apple/img/apple/64/00a9-fe0f.png" ' +
                    'alt="copyright emoji"></span></p>'
            );

            expect(hasEmojiNode(editor)).toBe(false);
            expect(editor.getHTML()).not.toContain('cdn.jsdelivr.net');
            expect(docText(editor)).toContain('©');
        });
    });

    describe('AC-020 — the registration is load-bearing', () => {
        /**
         * The guard for AC-013. Neutralizing `parseHTML` must not weaken the reason the extension
         * stays registered, so this asserts the failure mode we are protecting against still
         * exists: without the registration, TipTap cannot resolve the stored node at all.
         *
         * This is the #37145 / `AIContent` mechanism — the one that blanked a whole field.
         */
        it('a schema without the emoji extension cannot parse a stored emoji node', () => {
            // A realistic editor MINUS the emoji registration — StarterKit plus the link mark the
            // payload carries. Everything the fixture needs except the one thing under test.
            const schemaWithout = getSchema([StarterKit, Link]);

            expect(() => PMNode.fromJSON(schemaWithout, REPORTED_PAYLOAD)).toThrow(
                /emoji/i
            );
        });

        it('the editor DOES resolve a stored emoji node, because the extension stays registered', () => {
            editor = createTestEditor(injector, { content: REPORTED_PAYLOAD });

            // Pre-heal (US2) this is still an `emoji` node; the point here is that it PARSED.
            expect(editor.state.doc.content.size).toBeGreaterThan(0);
            expect(docText(editor)).toContain('dotCMS Copyright');
        });
    });
});
