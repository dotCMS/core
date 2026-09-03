import type { Injector } from '@angular/core';

import { Editor } from '@tiptap/core';

import type { DotMessageService } from '@dotcms/data-access';

import { createEditorExtensions } from './editor-extensions';

import {
    EMOJI_CHARACTER_SAMPLE,
    NON_EMOJI_CONTROL_SAMPLE,
    PICTOGRAPHIC_SAMPLE,
    TEXT_PRESENTATION_SAMPLE
} from '../testing/emoji.fixtures';

import type { SlashMenuService } from '../components/slash-menu/slash-menu.service';

/**
 * #37340 — the emoji extension's `appendTransaction` replaced any character in the Unicode emoji
 * set with a standalone `emoji` node built with NO marks. Two consequences, both persisted:
 *
 *  1. A single linked phrase became `text(link) + emoji(no marks) + text(link)` — two `<a>`
 *     elements where the author created one. WCAG 2.2 Level A failures (1.3.1, 2.4.4, 4.1.2).
 *  2. The stored node holds only a shortcode, no character, so renderers with no `emoji` branch
 *     dropped it entirely.
 *
 * The fix stops the conversion while KEEPING the node registered, so content already saved with
 * `emoji` nodes still parses. These specs pin both halves of that: nothing new is ever created,
 * and the old shape still loads.
 */
describe('emoji extension — characters stay in the text node (#37340)', () => {
    const injector = { get: jest.fn() } as unknown as Injector;
    const messageService = { get: (key: string) => key } as unknown as DotMessageService;

    /**
     * `editor-extensions.spec.ts` gets away with `{} as SlashMenuService` because it only calls
     * `flattenExtensions`/`getSchema`. These specs mount a real `Editor`, and
     * `slash-command.extension.ts` calls into the service from `addProseMirrorPlugins`, so the
     * surface it touches has to exist.
     */
    const menuService = {
        attachEditor: jest.fn(),
        detachEditor: jest.fn(),
        filterItems: jest.fn().mockReturnValue([]),
        open: jest.fn(),
        update: jest.fn(),
        close: jest.fn(),
        handleKeyDown: jest.fn().mockReturnValue(false)
    } as unknown as SlashMenuService;

    /**
     * Restricting to `paragraph` keeps table/image/dotContent out, so `createEditorExtensions`
     * never resolves anything from the injector — same trick the sibling spec uses. `link` and
     * `emoji` are registered regardless of Allowed Blocks (#37175), which is exactly what these
     * specs need.
     */
    const BASE_BLOCKS = ['paragraph'];

    /** Build a real editor over the real extension assembly, so wiring is covered too. */
    const buildEditor = (content: unknown, allowedBlocks: string[] = BASE_BLOCKS): Editor =>
        new Editor({
            extensions: createEditorExtensions(
                menuService,
                allowedBlocks,
                injector,
                messageService
            ),
            content: content as never
        });

    const LINK = { type: 'link', attrs: { href: 'https://dotcms.com' } };

    const inlineNodes = (editor: Editor) =>
        (editor.getJSON().content?.[0]?.content ?? []) as Array<{
            type: string;
            text?: string;
            marks?: Array<{ type: string }>;
        }>;

    const nodeTypes = (editor: Editor) => inlineNodes(editor).map((n) => n.type);

    const anchorCount = (editor: Editor) => (editor.getHTML().match(/<a[\s>]/g) ?? []).length;

    /** Force `appendTransaction` to run — it only fires when the document actually changes. */
    const touch = (editor: Editor) => {
        editor.chain().focus('end').insertContent(' ').run();
    };

    /**
     * Type character by character through `handleTextInput`, which is where ProseMirror runs
     * input rules. `insertContent` dispatches a plain transaction and never triggers them, so an
     * emoticon test built on it passes without exercising the emoticon path at all.
     */
    const typeText = (editor: Editor, text: string) => {
        for (const char of text) {
            const { state, view } = editor;
            const { from, to } = state.selection;
            const handled = view.someProp('handleTextInput', (fn) => fn(view, from, to, char));

            if (!handled) {
                view.dispatch(state.tr.insertText(char, from, to));
            }
        }
    };

    let editors: Editor[] = [];
    const track = (editor: Editor) => {
        editors.push(editor);

        return editor;
    };

    afterEach(() => {
        editors.forEach((editor) => editor.destroy());
        editors = [];
    });

    // ---------------------------------------------------------------- T006 / AC-002

    describe('AC-002: no `emoji` node is created for any character in the class', () => {
        it.each([...EMOJI_CHARACTER_SAMPLE])(
            'keeps %s inside the text node',
            (character: string) => {
                const editor = track(buildEditor(`<p>before ${character} after</p>`));

                touch(editor);

                expect(nodeTypes(editor)).not.toContain('emoji');
                expect(inlineNodes(editor)).toHaveLength(1);
                expect(inlineNodes(editor)[0].text).toContain(character);
            }
        );

        it.each([...NON_EMOJI_CONTROL_SAMPLE])(
            'leaves the non-emoji control character %s untouched',
            (character: string) => {
                const editor = track(buildEditor(`<p>before ${character} after</p>`));

                touch(editor);

                expect(nodeTypes(editor)).toEqual(['text']);
                expect(inlineNodes(editor)[0].text).toContain(character);
            }
        );
    });

    // ---------------------------------------------------------------- T007 / AC-001, AC-003

    describe('AC-001/AC-003: the link stays a single run', () => {
        const linkedDoc = (text: string) => ({
            type: 'doc',
            content: [
                { type: 'paragraph', content: [{ type: 'text', marks: [LINK], text }] }
            ]
        });

        it.each([
            ['middle', 'dotCMS Copyright © All rights reserved'],
            ['start', '© dotCMS Copyright All rights reserved'],
            ['end', 'dotCMS Copyright All rights reserved ©']
        ])('keeps one text node with one link mark — symbol at the %s', (_position, text) => {
            const editor = track(buildEditor(linkedDoc(text)));

            touch(editor);

            const nodes = inlineNodes(editor);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('text');
            expect(nodes[0].marks?.filter((m) => m.type === 'link')).toHaveLength(1);
            expect(anchorCount(editor)).toBe(1);
        });

        it('does not split the link for a pictographic emoji either', () => {
            const editor = track(buildEditor(linkedDoc('Launch 🚀 today')));

            touch(editor);

            expect(nodeTypes(editor)).toEqual(['text']);
            expect(anchorCount(editor)).toBe(1);
        });

        it('applies a link across a whole line containing an emoji without splitting it', () => {
            const editor = track(buildEditor('<p>Hello 🙂 World</p>'));

            editor.chain().selectAll().setLink({ href: 'https://dotcms.com' }).run();

            expect(nodeTypes(editor)).toEqual(['text']);
            expect(anchorCount(editor)).toBe(1);
        });
    });

    // ---------------------------------------------------------------- T008 / AC-004

    describe('AC-004: pasted characters behave the same as typed ones', () => {
        it('plain-text paste keeps the character in the text node', () => {
            const editor = track(buildEditor('<p>dotCMS </p>'));

            editor.chain().focus('end').insertContent('©').run();

            expect(nodeTypes(editor)).not.toContain('emoji');
        });

        it('HTML paste keeps the character in the text node', () => {
            const editor = track(buildEditor('<p>dotCMS </p>'));

            editor.chain().focus('end').insertContent('<span>©</span>').run();

            expect(nodeTypes(editor)).not.toContain('emoji');
        });

        it('an &copy; HTML entity resolves to the character, not an emoji node', () => {
            const editor = track(buildEditor('<p>dotCMS &copy; 2026</p>'));

            touch(editor);

            expect(nodeTypes(editor)).not.toContain('emoji');
            expect(inlineNodes(editor)[0].text).toContain('©');
        });
    });

    // ---------------------------------------------------------------- T009 / AC-005

    describe('AC-005: the toolbar picker inserts a literal character', () => {
        it('inserts the native character into the surrounding text node', () => {
            const editor = track(buildEditor('<p>ship it </p>'));

            // Exactly what emoji-picker.component.ts does: insertContent(emoji.native).
            editor.chain().focus('end').insertContent('🚀').run();

            expect(nodeTypes(editor)).not.toContain('emoji');
            expect(editor.getText()).toContain('🚀');
        });

        it('yields a single <a> when inserted inside a link', () => {
            const editor = track(
                buildEditor({
                    type: 'doc',
                    content: [
                        {
                            type: 'paragraph',
                            content: [{ type: 'text', marks: [LINK], text: 'ship it' }]
                        }
                    ]
                })
            );

            // Mid-run, not at the end — inserting at the end cannot split a link, so an
            // end-insertion assertion would pass even with the defect present.
            editor.chain().focus().setTextSelection(5).insertContent('🚀').run();

            expect(anchorCount(editor)).toBe(1);
            expect(nodeTypes(editor)).not.toContain('emoji');
        });
    });

    // ---------------------------------------------------------------- T010 / AC-006

    describe('AC-006: `:)` emoticons insert a literal character', () => {
        // `enableEmoticons` is gated on `has('emoji')`, so the input rule only exists when
        // `emoji` is in Allowed Blocks. Testing without it would pass trivially by never
        // exercising the emoticon path at all.
        const EMOTICON_BLOCKS = ['paragraph', 'emoji'];

        it('creates no emoji node', () => {
            const editor = track(buildEditor('<p>nice</p>', EMOTICON_BLOCKS));

            editor.commands.focus('end');
            typeText(editor, ' :) ');

            expect(nodeTypes(editor)).not.toContain('emoji');
        });

        it('leaves linked text as a single run', () => {
            const editor = track(
                buildEditor({
                    type: 'doc',
                    content: [
                        {
                            type: 'paragraph',
                            content: [{ type: 'text', marks: [LINK], text: 'nice' }]
                        }
                    ]
                }, EMOTICON_BLOCKS)
            );

            editor.commands.focus('end');
            typeText(editor, ' :) ');

            expect(inlineNodes(editor).filter((n) => n.type === 'text')).toHaveLength(1);
            expect(anchorCount(editor)).toBe(1);
        });
    });

    // ---------------------------------------------------------------- T011 / AC-007

    describe('AC-007: behavior is identical regardless of Allowed Blocks', () => {
        it.each([
            ['emoji allowed', ['paragraph', 'emoji']],
            ['emoji not allowed', ['paragraph']]
        ])('%s', (_label, allowedBlocks) => {
            const editor = track(buildEditor('<p>dotCMS © 2026</p>', allowedBlocks as string[]));

            touch(editor);

            expect(nodeTypes(editor)).not.toContain('emoji');
            expect(inlineNodes(editor)[0].text).toContain('©');
        });
    });

    // ---------------------------------------------------------------- T012 / AC-008 + back-compat

    describe('AC-008: rendered as text, never as a fallback image', () => {
        it.each([...TEXT_PRESENTATION_SAMPLE.slice(0, 5), ...PICTOGRAPHIC_SAMPLE])(
            'emits no <img> for %s',
            (character: string) => {
                const editor = track(buildEditor(`<p>${character}</p>`));

                touch(editor);

                expect(editor.getHTML()).not.toContain('<img');
                expect(editor.getHTML()).not.toContain('emoji"');
            }
        );
    });

    describe('backward compatibility: the `emoji` node stays registered', () => {
        it('keeps `emoji` in the schema so stored content still parses', () => {
            const editor = track(buildEditor('<p>x</p>'));

            expect(editor.schema.nodes['emoji']).toBeDefined();
        });

        it('loads a document containing a stored `emoji` node without dropping content', () => {
            const editor = track(
                buildEditor({
                    type: 'doc',
                    content: [
                        {
                            type: 'paragraph',
                            content: [
                                { type: 'text', text: 'dotCMS ' },
                                { type: 'emoji', attrs: { name: 'copyright' } },
                                { type: 'text', text: ' 2026' }
                            ]
                        }
                    ]
                })
            );

            // The node must survive the round-trip — removing the registration would drop it
            // silently, the failure mode documented for `aiContent` in the lib's CLAUDE.md.
            expect(nodeTypes(editor)).toContain('emoji');
            expect(editor.getText()).toContain('dotCMS');
        });
    });
});
