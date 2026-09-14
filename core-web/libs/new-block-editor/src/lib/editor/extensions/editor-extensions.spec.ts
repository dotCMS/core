import { vi } from 'vitest';

import type { Injector } from '@angular/core';

import { Extension, flattenExtensions, getSchema } from '@tiptap/core';
import Link from '@tiptap/extension-link';
import { Node as PMNode } from '@tiptap/pm/model';

import type { DotMessageService } from '@dotcms/data-access';

import { createEditorExtensions } from './editor-extensions';
import { DotLink } from './link.extension';

import {
    preserveUnknownNodesInDocument,
    UNKNOWN_BLOCK_MARK_NAME,
    UNKNOWN_BLOCK_NODE_NAME
} from '../utils/unknown-block.utils';

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
    const injector = { get: vi.fn() } as unknown as Injector;
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
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
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

        /**
         * #36351 — this assertion is the INVERSE of what it was, for the same reason the emoji
         * one below is.
         *
         * `link` is not selectable in Allowed Blocks either, so `has('link')` was true ONLY on a
         * field with no restriction at all. Restricting any block silently disabled auto-linking
         * and link-on-paste — and hid the toolbar button — with no admin having chosen it and no
         * control anywhere to undo it. The gate could only ever misfire, so it is gone; these
         * flags are now unconditional.
         */
        it('keeps the implicit LINK authoring paths on a restricted field', () => {
            const extensions = restricted();

            expect(byName(extensions, 'link')?.options.autolink).toBe(true);
            expect(byName(extensions, 'link')?.options.linkOnPaste).toBe(true);
        });

        /**
         * #37340 AC-008 — this assertion is the INVERSE of what it was, deliberately.
         *
         * `emoji` is not selectable in Allowed Blocks: the option list comes from
         * `getEditorBlockOptions()`, which offers block nodes only, and `link`/`emoji`/`youtube`
         * were excluded by #37175 itself. So `has('emoji')` was true ONLY on a field with no
         * restriction at all — meaning restricting ANY block silently removed `:)` (and the
         * toolbar's emoji button) from that field, with no admin having chosen it.
         *
         * That is not a restriction anyone configured; it is a gate that could only misfire. And
         * since emoji are now plain characters an author can always type, there is nothing left
         * for it to restrict even in principle.
         */
        it('keeps emoticon entry on a restricted field, because emoji cannot be restricted', () => {
            expect(byName(restricted(), 'emoji')?.options.enableEmoticons).toBe(true);
        });

        it('keeps the implicit authoring paths on an unrestricted field', () => {
            const extensions = unrestricted();

            expect(byName(extensions, 'link')?.options.autolink).toBe(true);
            expect(byName(extensions, 'link')?.options.linkOnPaste).toBe(true);
            expect(byName(extensions, 'emoji')?.options.enableEmoticons).toBe(true);
        });
    });

    /**
     * #37175 AC5 — the failure mode the two registered marks only papered over. Any mark the
     * schema does not declare aborts `Node.fromJSON` for the WHOLE document, so registering
     * `link` and `highlight` fixed the two known offenders, not the class of bug. The realistic
     * sources are content that did not come from this editor: an API write, a migration from
     * another CMS (`textStyle`, `color`, `fontFamily` are the usual suspects), or a version
     * downgrade.
     */
    describe('unknown marks no longer abort the document (#37175 AC5)', () => {
        const RESTRICTED = ['bulletList', 'orderedList', 'codeBlock'];

        const schema = () =>
            getSchema(createEditorExtensions(menuService, RESTRICTED, injector, messageService));

        /** Two paragraphs so a partial load is distinguishable from a total abort. */
        const storedDoc = (mark: Record<string, unknown>) => ({
            type: 'doc',
            content: [
                {
                    type: 'paragraph',
                    content: [{ type: 'text', marks: [mark], text: 'imported copy' }]
                },
                {
                    type: 'paragraph',
                    content: [{ type: 'text', text: 'plain sibling' }]
                }
            ]
        });

        const knownNames = (target: ReturnType<typeof schema>) => ({
            nodes: new Set(Object.keys(target.nodes)),
            marks: new Set(Object.keys(target.marks))
        });

        it('registers the unsupported-mark placeholder', () => {
            expect(Object.keys(schema().marks)).toContain(UNKNOWN_BLOCK_MARK_NAME);
        });

        it('is the exact throw the fix has to prevent', () => {
            expect(() => PMNode.fromJSON(schema(), storedDoc({ type: 'textStyle' }))).toThrow(
                /no mark type textStyle/
            );
        });

        it('loads the whole document once the unknown mark is preserved', () => {
            const target = schema();
            const { nodes, marks } = knownNames(target);

            const doc = PMNode.fromJSON(
                target,
                preserveUnknownNodesInDocument(
                    storedDoc({ type: 'textStyle', attrs: { color: '#ff0000' } }),
                    nodes,
                    marks
                )
            );

            // Before the fix this was an empty doc: 0 characters, both paragraphs gone.
            expect(doc.childCount).toBe(2);
            expect(doc.textContent).toBe('imported copyplain sibling');
        });

        it('keeps the decorated text editable, carrying the payload for the save path', () => {
            const target = schema();
            const { nodes, marks } = knownNames(target);
            const original = { type: 'textStyle', attrs: { color: '#ff0000' } };

            const doc = PMNode.fromJSON(
                target,
                preserveUnknownNodesInDocument(storedDoc(original), nodes, marks)
            );
            const [mark] = doc.firstChild?.firstChild?.marks ?? [];

            expect(mark.type.name).toBe(UNKNOWN_BLOCK_MARK_NAME);
            expect(mark.attrs['originalMark']).toEqual(original);
        });

        it('survives a mark with no attrs at all', () => {
            const target = schema();
            const { nodes, marks } = knownNames(target);

            const doc = PMNode.fromJSON(
                target,
                preserveUnknownNodesInDocument(storedDoc({ type: 'someUnknownMark' }), nodes, marks)
            );

            expect(doc.textContent).toBe('imported copyplain sibling');
        });
    });

    /**
     * #36351 — the replacement for the `#37175 AC3` describe that used to live here.
     *
     * That block tested `DotLink.addPasteRules()`, an override which suppressed TipTap's URL
     * paste rule whenever `link` was not an allowed block. #37175 added it because
     * `linkOnPaste: false` alone did not close the link-on-paste path. #36351 removes the gate
     * that fed it, so the override is gone and with it the only dotCMS code those tests could
     * exercise — what remains is upstream TipTap's own paste rule, which is not ours to test.
     *
     * What IS still worth pinning is that `DotLink` does not reintroduce a suppression. This is
     * structural on purpose: a behavioural test would have to drive a real paste through
     * ProseMirror to assert a rule that upstream already guarantees, while this fails the moment
     * someone adds an `addPasteRules` override again — which is exactly the regression to catch.
     *
     * The authoring-path flags themselves are covered above by
     * `keeps the implicit LINK authoring paths on a restricted field`.
     */
    describe('link-on-paste is never gated (#36351)', () => {
        it('does not override the base paste rules', () => {
            expect(DotLink.config.addPasteRules).toBe(Link.config.addPasteRules);
        });

        it('leaves the authoring flags on regardless of allowedBlocks', () => {
            const linkFor = (allowedBlocks: string[] | undefined) =>
                flattenExtensions(
                    createEditorExtensions(menuService, allowedBlocks, injector, messageService)
                ).find((ext) => ext.name === 'link');

            for (const allowedBlocks of [undefined, ['bulletList', 'orderedList'], ['image']]) {
                expect(linkFor(allowedBlocks)?.options.autolink).toBe(true);
                expect(linkFor(allowedBlocks)?.options.linkOnPaste).toBe(true);
            }
        });
    });
});
