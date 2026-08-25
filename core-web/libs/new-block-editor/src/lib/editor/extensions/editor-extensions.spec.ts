import type { Injector } from '@angular/core';

import { Extension, flattenExtensions, getSchema } from '@tiptap/core';
import { Node as PMNode } from '@tiptap/pm/model';

import type { DotMessageService } from '@dotcms/data-access';

import { createEditorExtensions } from './editor-extensions';

import { UNKNOWN_BLOCK_NODE_NAME } from '../utils/unknown-block.utils';

import type { SlashMenuService } from '../components/slash-menu/slash-menu.service';

/**
 * These specs cover the extension-assembly seam that the `customBlocks` remote-extension
 * feature depends on (#36646): the editor must register exactly one `link` / `underline`
 * (StarterKit v3 bundles both) and must drop remote extensions whose names collide with a
 * built-in instead of double-registering them.
 *
 * They also pin the mark inventory against the legacy editor (#37145). A mark the legacy
 * editor registered but this one does not makes TipTap abort `Node.fromJSON` for the WHOLE
 * document and fall back to an empty doc — the field renders blank even though the stored
 * JSON is intact.
 */
describe('createEditorExtensions', () => {
    // A restricted list keeps table/codeBlock/image out, so the injector is never touched
    // during assembly — a bare stub is enough.
    const injector = { get: jest.fn() } as unknown as Injector;
    const menuService = {} as SlashMenuService;
    const messageService = { get: (key: string) => key } as unknown as DotMessageService;

    const build = (remote: Extension[] = []) =>
        flattenExtensions(
            createEditorExtensions(menuService, ['link'], injector, messageService, remote)
        ).map((ext) => ext.name);

    it('registers exactly one "link" (StarterKit link disabled, DotLink is the sole source)', () => {
        const names = build();

        expect(names.filter((name) => name === 'link')).toHaveLength(1);
    });

    it('registers exactly one "underline" (bundled by StarterKit)', () => {
        const names = build();

        expect(names.filter((name) => name === 'underline')).toHaveLength(1);
    });

    it('drops a remote extension whose name collides with a built-in and warns', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const remoteUnderline = Extension.create({ name: 'underline' });

        const names = build([remoteUnderline]);

        expect(names.filter((name) => name === 'underline')).toHaveLength(1);
        expect(warn).toHaveBeenCalledWith(expect.stringContaining('underline'));

        warn.mockRestore();
    });

    it('keeps a remote extension with a unique name', () => {
        const remoteCustom = Extension.create({ name: 'customNode' });

        const names = build([remoteCustom]);

        expect(names).toContain('customNode');
    });

    it('always registers the unsupported-block catch-all node', () => {
        expect(build()).toContain(UNKNOWN_BLOCK_NODE_NAME);
    });

    /**
     * #37145 — the legacy editor registers Highlight
     * (`libs/block-editor/.../dot-block-editor.component.ts` lines 35, 737), so content
     * authored there can carry `highlight` marks. Without the extension here, that content
     * cannot be deserialized at all.
     */
    it('registers the "highlight" mark at parity with the legacy editor', () => {
        const names = build();

        expect(names.filter((name) => name === 'highlight')).toHaveLength(1);
    });

    it('deserializes a legacy document containing highlight marks instead of emptying it', () => {
        const schema = getSchema(
            createEditorExtensions(menuService, ['link'], injector, messageService)
        );
        const legacyDoc = {
            type: 'doc',
            content: [
                {
                    type: 'paragraph',
                    content: [
                        {
                            type: 'text',
                            marks: [{ type: 'highlight' }, { type: 'bold' }],
                            text: 'Good Credit History:'
                        }
                    ]
                }
            ]
        };

        const doc = PMNode.fromJSON(schema, legacyDoc);

        expect(doc.textContent).toBe('Good Credit History:');
    });

    /**
     * #37175 — `link`, `emoji` and `youtube` are gated by `has()` but are not offered as
     * Allowed Blocks options, so every restricted field dropped them. `link` is a mark, and a
     * missing mark aborts `Node.fromJSON` for the whole document; `emoji`/`youtube` are nodes,
     * so they resurfaced as `Unsupported block (…)`. All three now register unconditionally and
     * gate their authoring paths instead.
     */
    describe('extensions gated by keys Allowed Blocks never offers (#37175)', () => {
        /** Mirrors what the settings UI produces — it cannot contain link/emoji/youtube. */
        const RESTRICTED = ['bulletList', 'orderedList', 'codeBlock'];

        const restricted = () =>
            createEditorExtensions(menuService, RESTRICTED, injector, messageService);
        const unrestricted = () =>
            createEditorExtensions(menuService, undefined, injector, messageService);
        const byName = (extensions: ReturnType<typeof restricted>, name: string) =>
            flattenExtensions(extensions).find((ext) => ext.name === name);

        it.each(['link', 'emoji', 'youtube'])('registers "%s" on a restricted field', (name) => {
            const names = flattenExtensions(restricted()).map((ext) => ext.name);

            expect(names.filter((registered) => registered === name)).toHaveLength(1);
        });

        it('deserializes a document containing link marks on a restricted field', () => {
            const schema = getSchema(restricted());
            const storedDoc = {
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        content: [
                            {
                                type: 'text',
                                marks: [{ type: 'link', attrs: { href: 'https://dotcms.com' } }],
                                text: 'Getting started'
                            }
                        ]
                    }
                ]
            };

            // Threw `RangeError: There is no mark type link in this schema` before the fix.
            const doc = PMNode.fromJSON(schema, storedDoc);

            expect(doc.textContent).toBe('Getting started');
            expect(doc.firstChild?.firstChild?.marks[0].type.name).toBe('link');
        });

        it('renders a stored emoji instead of an unsupported-block placeholder', () => {
            const schema = getSchema(restricted());
            const storedDoc = {
                type: 'doc',
                content: [
                    { type: 'paragraph', content: [{ type: 'emoji', attrs: { name: 'smile' } }] }
                ]
            };

            const doc = PMNode.fromJSON(schema, storedDoc);

            expect(doc.firstChild?.firstChild?.type.name).toBe('emoji');
        });

        // The schema keeps the extensions so stored content loads; these flags are what
        // actually enforce the restriction, alongside the toolbar's `@if (isAllowed(...))`.
        it('disables the implicit authoring paths when the block is not allowed', () => {
            const extensions = restricted();

            expect(byName(extensions, 'link')?.options.autolink).toBe(false);
            expect(byName(extensions, 'link')?.options.linkOnPaste).toBe(false);
            expect(byName(extensions, 'emoji')?.options.enableEmoticons).toBe(false);
        });

        it('keeps the implicit authoring paths on an unrestricted field', () => {
            const extensions = unrestricted();

            expect(byName(extensions, 'link')?.options.autolink).toBe(true);
            expect(byName(extensions, 'link')?.options.linkOnPaste).toBe(true);
            expect(byName(extensions, 'emoji')?.options.enableEmoticons).toBe(true);
        });
    });
});
