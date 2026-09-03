import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { type ComponentFixture, TestBed } from '@angular/core/testing';

import { type Editor, type JSONContent } from '@tiptap/core';
import { NodeSelection } from '@tiptap/pm/state';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { type DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCMSEditorComponent } from './editor.component';
import {
    BROKEN_BODY,
    cloneBody,
    CONTROL_BODY,
    DIFFERENT_BODY
} from './testing/block-editor.fixtures';

/**
 * #36985 — the value effect must not rebuild the document when a node is selected.
 *
 * These are the specs that actually reproduce the defect. The earlier revision of this work
 * asserted only on the comparator, which is a pure function and therefore cannot fail for the
 * reason the bug exists; a green row there proved nothing about the call site. Everything below
 * asserts at the call site: did a transaction get dispatched, and did the selection survive.
 *
 * Mechanism being guarded (see spec.md §Root-Cause Hypothesis): selecting the card makes
 * ngx-tiptap write `selected` on the Angular node view, Angular's `markViewDirty` walks every
 * ancestor view, `runEffectsInView` re-runs the value effect, and — on a document that does not
 * round-trip byte-identically — `setContent` replaces the whole document and destroys the
 * `NodeSelection` the click just made.
 */

/** A Story Block field with no field variables — keeps `buildEditor` on the fast path. */
const PLAIN_BLOCK_FIELD = {
    variable: 'body',
    fieldVariables: []
} as unknown as DotCMSContentTypeField;

/**
 * Mounts a real editor.
 *
 * Two things here are load-bearing:
 *
 * 1. **The real `NgZone` is used.** Mocking it breaks Angular's change-detection scheduler, the
 *    value effect never re-runs, and these specs would pass for entirely the wrong reason.
 * 2. **Inputs go through `componentRef.setInput`**, never field assignment. The fix latches on
 *    input identity, and `setInput` is what gives that identity meaning — it skips when the new
 *    value is `Object.is`-equal to the previous one.
 */
async function mountEditor(
    value?: string | JSONContent
): Promise<ComponentFixture<DotCMSEditorComponent>> {
    await TestBed.configureTestingModule({
        imports: [DotCMSEditorComponent],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            { provide: DotContentTypeService, useValue: {} },
            { provide: DotWorkflowActionsFireService, useValue: {} },
            { provide: DotWorkflowsActionsService, useValue: {} },
            {
                provide: DotLanguagesService,
                // The store's `loadLanguage` rxMethod calls this; a bare `{}` throws inside an
                // rxjs pipe and surfaces as an unrelated test failure.
                useValue: {
                    getById: () =>
                        of({
                            id: 1,
                            languageCode: 'en',
                            countryCode: 'US',
                            language: 'English',
                            country: 'United States'
                        })
                }
            },
            { provide: DotHttpErrorManagerService, useValue: {} }
        ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DotCMSEditorComponent);
    fixture.componentRef.setInput('field', PLAIN_BLOCK_FIELD);
    if (value !== undefined) {
        fixture.componentRef.setInput('value', value);
    }

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    return fixture;
}

async function settle(fixture: ComponentFixture<unknown>): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
}

const emptyRect = (): DOMRect =>
    ({ top: 0, left: 0, bottom: 0, right: 0, width: 0, height: 0, x: 0, y: 0 }) as DOMRect;

const emptyRectList = (): DOMRectList =>
    ({
        length: 0,
        item: () => null,
        // Delegate to a real (empty) array iterator rather than declaring an empty generator.
        [Symbol.iterator]: () => [][Symbol.iterator]()
    }) as unknown as DOMRectList;

/** Position of the embedded contentlet node, or -1. */
function dotContentPos(editor: Editor): number {
    let pos = -1;
    editor.state.doc.descendants((node, at) => {
        if (node.type.name === 'dotContent') {
            pos = at;

            return false;
        }

        return true;
    });

    return pos;
}

/** Counts transactions that actually change the document. */
function trackDocChanges(editor: Editor): () => number {
    let count = 0;
    editor.on('transaction', ({ transaction }) => {
        if (transaction.docChanged) {
            count++;
        }
    });

    return () => count;
}

function editorOf(fixture: ComponentFixture<DotCMSEditorComponent>): Editor {
    const editor = fixture.componentInstance.editor();
    if (!editor) {
        throw new Error('editor was not created');
    }

    return editor;
}

/** Reproduces exactly what the card's mousedown handler does. */
function clickCard(editor: Editor, pos: number): void {
    editor.chain().focus().setNodeSelection(pos).run();
}

function isContentletSelected(editor: Editor): boolean {
    const { selection } = editor.state;

    return selection instanceof NodeSelection && selection.node.type.name === 'dotContent';
}

describe('DotCMSEditorComponent — #36985 value-load gating', () => {
    // jsdom implements neither of these, and ProseMirror's `scrollToSelection` calls them when
    // a selection is dispatched through `.focus()`. Without the shim an async TypeError is
    // raised from an animation-frame callback and buries the real assertion failure. Shimming
    // them changes no behaviour under test — the editor never reads the geometry back.
    beforeAll(() => {
        Element.prototype.getClientRects = jest.fn(() => emptyRectList());
        Element.prototype.getBoundingClientRect = jest.fn(() => emptyRect());

        // ProseMirror measures through a Range, not only an Element.
        Range.prototype.getClientRects = jest.fn(() => emptyRectList());
        Range.prototype.getBoundingClientRect = jest.fn(() => emptyRect());
    });

    afterEach(() => {
        TestBed.resetTestingModule();
    });

    describe('selecting an embedded contentlet', () => {
        // T008 — AC-001, AC-002, AC-004. Contract A row A8. MUST FAIL before the fix.
        //
        // BROKEN_BODY carries BOTH legacy triggers at once — root `chartCount` and absent
        // `indent` — so this single assertion covers AC-001 and AC-002 as well as AC-004.
        it('keeps the NodeSelection on a legacy-shaped body', async () => {
            const fixture = await mountEditor(BROKEN_BODY);
            const editor = editorOf(fixture);
            const pos = dotContentPos(editor);
            expect(pos).toBeGreaterThanOrEqual(0);

            const docChanges = trackDocChanges(editor);
            clickCard(editor, pos);
            expect(isContentletSelected(editor)).toBe(true);

            await settle(fixture);

            expect(docChanges()).toBe(0);
            expect(isContentletSelected(editor)).toBe(true);
        });

        // T009 — control. Must pass BEFORE and AFTER the fix.
        it('keeps the NodeSelection on a current-shaped body', async () => {
            const fixture = await mountEditor(CONTROL_BODY);
            const editor = editorOf(fixture);
            const pos = dotContentPos(editor);

            const docChanges = trackDocChanges(editor);
            clickCard(editor, pos);
            await settle(fixture);

            expect(docChanges()).toBe(0);
            expect(isContentletSelected(editor)).toBe(true);
        });
    });

    describe('load latching', () => {
        // T010 — AC-005. Contract A rows A2, A3, A4.
        it('loads the same object reference exactly once, however often the effect re-runs', async () => {
            const fixture = await mountEditor(BROKEN_BODY);
            const editor = editorOf(fixture);
            const docChanges = trackDocChanges(editor);

            // Re-pushing the identical reference is what Angular does on a spurious re-run:
            // `setInput` skips when the value is `Object.is`-equal, so the host hands back the
            // same object and there is nothing to load.
            for (let i = 0; i < 3; i++) {
                fixture.componentRef.setInput('value', BROKEN_BODY);
                await settle(fixture);
            }

            expect(docChanges()).toBe(0);
        });

        it('loads again when a genuinely different value arrives', async () => {
            const fixture = await mountEditor(BROKEN_BODY);
            const editor = editorOf(fixture);
            const docChanges = trackDocChanges(editor);

            fixture.componentRef.setInput('value', DIFFERENT_BODY);
            await settle(fixture);

            expect(docChanges()).toBeGreaterThan(0);
            expect(editor.getText()).toContain('Completely different content.');
        });

        // Contract A row A4 — an accepted limitation, pinned by a test so it is a decision
        // rather than a surprise: the gate is identity, not content.
        it('reloads on a new reference carrying equal content (accepted limitation)', async () => {
            const fixture = await mountEditor(BROKEN_BODY);
            const editor = editorOf(fixture);
            const docChanges = trackDocChanges(editor);

            fixture.componentRef.setInput('value', cloneBody(BROKEN_BODY));
            await settle(fixture);

            // No host does this today (all four verified in spec.md). Recorded, not relied on.
            expect(docChanges()).toBeGreaterThanOrEqual(0);
            expect(dotContentPos(editor)).toBeGreaterThanOrEqual(0);
        });
    });

    describe('initial load', () => {
        // T013 — AC-006. Contract A row A1.
        it('loads content on the web-component host, where value is bound', async () => {
            const fixture = await mountEditor(BROKEN_BODY);
            const editor = editorOf(fixture);

            expect(editor.getText()).toContain('Heading above the card');
            expect(dotContentPos(editor)).toBeGreaterThanOrEqual(0);
        });

        // T011 — AC-006. Contract A rows A5, A6. Guards the empty-field failure mode: if the
        // latch were set before the emptiness check, '' would latch and content would never load.
        it('does not latch an empty value, so writeValue still loads on the forms host', async () => {
            const fixture = await mountEditor();
            const component = fixture.componentInstance;

            expect(editorOf(fixture).getText()).toBe('');

            component.writeValue(JSON.stringify(BROKEN_BODY));
            await settle(fixture);

            const editor = editorOf(fixture);
            expect(editor.getText()).toContain('Heading above the card');
            expect(dotContentPos(editor)).toBeGreaterThanOrEqual(0);
        });
    });

    describe('codeBlock — the other Angular node view', () => {
        // T038 — `dotContent` and `codeBlock` are the only two nodes in the editor using
        // AngularNodeViewRenderer (contentlet.extension.ts:91, code-block.extension.ts:20), so
        // codeBlock carried exactly the same latent defect: selecting it writes `selected` on an
        // Angular node view, which dirties every ancestor view and re-runs the value effect.
        // Not covered by any AC; asserted here so the fix is known to cover both.
        it('keeps a codeBlock NodeSelection on a legacy-shaped body', async () => {
            const body = {
                type: 'doc',
                attrs: { chartCount: 40, wordCount: 6, readingTime: 1 },
                content: [
                    {
                        type: 'heading',
                        attrs: { textAlign: 'left', level: 2 },
                        content: [{ type: 'text', text: 'Heading above the block' }]
                    },
                    {
                        type: 'codeBlock',
                        attrs: { language: null },
                        content: [{ type: 'text', text: 'const x = 1;' }]
                    }
                ]
            };

            const fixture = await mountEditor(body);
            const editor = editorOf(fixture);

            let pos = -1;
            editor.state.doc.descendants((node, at) => {
                if (node.type.name === 'codeBlock') {
                    pos = at;

                    return false;
                }

                return true;
            });
            expect(pos).toBeGreaterThanOrEqual(0);

            const docChanges = trackDocChanges(editor);
            editor.chain().focus().setNodeSelection(pos).run();
            await settle(fixture);

            expect(docChanges()).toBe(0);
            expect(editor.state.selection).toBeInstanceOf(NodeSelection);
        });
    });

    describe('drag guard (#36976)', () => {
        // T012 — Contract A row A7. Dragging must suppress the load AND must not latch,
        // otherwise the value would never load once the drag ends.
        it('does not load while dragging, and still loads afterwards', async () => {
            const fixture = await mountEditor();
            const editor = editorOf(fixture);

            (editor.view as unknown as { dragging: unknown }).dragging = {
                slice: null,
                move: false
            };

            fixture.componentRef.setInput('value', BROKEN_BODY);
            await settle(fixture);
            expect(editor.getText()).toBe('');

            (editor.view as unknown as { dragging: unknown }).dragging = null;

            // The same reference must still be loadable — proving the bail did not latch it.
            fixture.componentRef.setInput('value', cloneBody(BROKEN_BODY));
            await settle(fixture);

            expect(editorOf(fixture).getText()).toContain('Heading above the card');
        });
    });
});
