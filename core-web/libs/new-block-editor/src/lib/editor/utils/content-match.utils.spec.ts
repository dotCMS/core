import { Injector } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Editor, type JSONContent } from '@tiptap/core';
import { Node as PMNode, type Schema } from '@tiptap/pm/model';

import { contentMatchesEditorDocument } from './content-match.utils';
import { preserveUnknownNodesInDocument } from './unknown-block.utils';

import { EditorPopoverService } from '../services/editor-popover.service';
import {
    ALPHABETICAL_KEY_ORDER_BODY,
    BROKEN_BODY,
    CONTROL_BODY,
    DIFFERENT_BODY,
    LIST_TEXT_ALIGN_BODY
} from '../testing/block-editor.fixtures';
import { buildEditorSchema } from '../testing/schema.testing';

/**
 * #36985 — the comparator must answer on document STRUCTURE, not on bytes.
 *
 * Each case is one row of `contracts/content-match.contract.md` (Contract B). Rows B1, B3, B4,
 * B5, B8 and B10 are the behaviour change and must fail before the fix; every other row is a
 * control that must keep its current verdict, so an over-permissive implementation is caught.
 *
 * Fixtures are parsed with the editor's REAL schema. A hand-written schema would agree with
 * whatever fixture it was written against and prove nothing.
 */
describe('contentMatchesEditorDocument — #36985', () => {
    let schema: Schema;
    let injector: Injector;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [{ provide: EditorPopoverService, useValue: {} }]
        });
        injector = TestBed.inject(Injector);
        schema = buildEditorSchema(injector);
    });

    afterEach(() => {
        TestBed.resetTestingModule();
    });

    /**
     * A stand-in for the editor carrying `body` as its current document, parsed through the real
     * schema — i.e. already normalized, exactly as a live editor's document would be.
     */
    const editorHolding = (body: JSONContent): Editor => {
        const doc = PMNode.fromJSON(schema, body);

        return {
            schema,
            state: { doc },
            getHTML: () => '<p></p>',
            getJSON: () => doc.toJSON()
        } as unknown as Editor;
    };

    /** The editor always holds the CONTROL document — every fixture is the same document. */
    const editor = () => editorHolding(CONTROL_BODY);

    describe('legacy shapes must compare equal', () => {
        // B1 + B3 — root `chartCount` and absent `indent`. MUST FAIL at Red.
        it('matches a legacy body carrying chartCount and no indent', () => {
            expect(contentMatchesEditorDocument(editor(), BROKEN_BODY)).toBe(true);
        });

        // B4 — the live divergence: legacy still writes listItem.textAlign today.
        it('matches a body whose listItem nodes carry textAlign', () => {
            const withList = editorHolding(LIST_TEXT_ALIGN_BODY);
            expect(contentMatchesEditorDocument(withList, LIST_TEXT_ALIGN_BODY)).toBe(true);
        });

        // B5 — what /api/v1/content returns. Breaks CURRENT content, not just old content.
        it('matches a body whose attrs are in alphabetical key order', () => {
            expect(contentMatchesEditorDocument(editor(), ALPHABETICAL_KEY_ORDER_BODY)).toBe(true);
        });
    });

    describe('controls — these must not move', () => {
        // B2
        it('matches a body already in the current shape', () => {
            expect(contentMatchesEditorDocument(editor(), CONTROL_BODY)).toBe(true);
        });

        // B6 — the guard against an over-permissive fix.
        it('does NOT match a genuinely different document', () => {
            expect(contentMatchesEditorDocument(editor(), DIFFERENT_BODY)).toBe(false);
        });

        // B9 — fail closed on input that genuinely cannot be deserialized, so the caller still
        // loads it and the field never blanks (#37145). Note this is now the *defensive* path:
        // since #37175 an unknown mark is preserved rather than fatal — see the unknown-mark
        // case below — so this uses a structurally invalid document instead.
        it('does NOT match a document that cannot be deserialized at all', () => {
            const malformed = {
                type: 'doc',
                content: [{ type: 'paragraph', content: [{ type: 'text' }] }]
            } as unknown as JSONContent;

            expect(contentMatchesEditorDocument(editor(), malformed)).toBe(false);
        });
    });

    describe('unknown node types', () => {
        const withUnknownNode: JSONContent = {
            type: 'doc',
            content: [
                {
                    type: 'paragraph',
                    attrs: { textAlign: 'left', indent: 0 },
                    content: [{ type: 'text', text: 'before' }]
                },
                { type: 'someCustomBlock', attrs: { foo: 'bar' } }
            ]
        };

        /**
         * A real editor's document holds the `dotUnsupportedBlock` PLACEHOLDER, not the raw
         * unknown node — the load path runs `preserveUnknownNodesInDocument` before parsing,
         * and `Node.fromJSON` would otherwise throw `RangeError: Unknown node type`. The holder
         * has to model that, or the test asserts against a document that cannot exist.
         */
        const holderWithPlaceholder = () =>
            editorHolding(
                preserveUnknownNodesInDocument(
                    JSON.parse(JSON.stringify(withUnknownNode)) as JSONContent,
                    new Set(Object.keys(schema.nodes)),
                    new Set(Object.keys(schema.marks))
                ) as JSONContent
            );

        // B7 — object entry point already works today.
        it('matches through the object entry point', () => {
            expect(contentMatchesEditorDocument(holderWithPlaceholder(), withUnknownNode)).toBe(
                true
            );
        });

        // B8 — the string entry point skips the placeholder transform today. MUST FAIL at Red.
        it('matches through the string entry point too', () => {
            expect(
                contentMatchesEditorDocument(
                    holderWithPlaceholder(),
                    JSON.stringify(withUnknownNode)
                )
            ).toBe(true);
        });
    });

    describe('unknown mark types (#37175)', () => {
        /**
         * Unknown MARKS used to abort `Node.fromJSON` outright — `RangeError: There is no mark
         * type X in this schema` — which is why an earlier revision of this contract expected
         * "not equal" for them. #37175 gave marks the same placeholder treatment nodes already
         * had (`dotUnsupportedMark`), so they now round-trip and must compare EQUAL, exactly
         * like unknown nodes. Measured: the raw JSON still throws if parsed directly, but the
         * comparator preserves first, so it parses and matches.
         */
        const withUnknownMark: JSONContent = {
            type: 'doc',
            content: [
                {
                    type: 'paragraph',
                    attrs: { textAlign: 'left', indent: 0 },
                    content: [
                        { type: 'text', marks: [{ type: 'someMarkNobodyRegistered' }], text: 'x' }
                    ]
                }
            ]
        };

        const holderWithMarkPlaceholder = () =>
            editorHolding(
                preserveUnknownNodesInDocument(
                    JSON.parse(JSON.stringify(withUnknownMark)) as JSONContent,
                    new Set(Object.keys(schema.nodes)),
                    new Set(Object.keys(schema.marks))
                ) as JSONContent
            );

        it('matches a body carrying an unknown mark, through the object entry point', () => {
            expect(contentMatchesEditorDocument(holderWithMarkPlaceholder(), withUnknownMark)).toBe(
                true
            );
        });

        it('matches a body carrying an unknown mark, through the string entry point', () => {
            expect(
                contentMatchesEditorDocument(
                    holderWithMarkPlaceholder(),
                    JSON.stringify(withUnknownMark)
                )
            ).toBe(true);
        });
    });

    describe('value shapes', () => {
        // B10 — the UVE side panel hands over a bare array. MUST FAIL at Red.
        it('matches a bare array of nodes', () => {
            const asArray = CONTROL_BODY.content as JSONContent[];
            expect(contentMatchesEditorDocument(editor(), asArray)).toBe(true);
        });

        // B11 — the JSP showdown fallback is not Block Editor JSON.
        it('compares an HTML string against getHTML()', () => {
            const holder = {
                schema,
                state: { doc: PMNode.fromJSON(schema, CONTROL_BODY) },
                getHTML: () => '<p>hello</p>',
                getJSON: () => CONTROL_BODY
            } as unknown as Editor;

            expect(contentMatchesEditorDocument(holder, '<p>hello</p>')).toBe(true);
            expect(contentMatchesEditorDocument(holder, '<p>different</p>')).toBe(false);
        });
    });
});
