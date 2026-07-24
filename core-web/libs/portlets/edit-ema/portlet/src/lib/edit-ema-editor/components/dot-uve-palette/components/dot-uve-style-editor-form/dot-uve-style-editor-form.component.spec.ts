import { InferInputSignals } from '@openng/spectator';
import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError, timer } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { computed, signal } from '@angular/core';
import { fakeAsync, flush, flushMicrotasks, tick } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';

import { Accordion, AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { mergeMap, tap } from 'rxjs/operators';

import { DotMessageService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSPageAsset } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/types/internal';

import { DotUveStyleEditorFormComponent } from './dot-uve-style-editor-form.component';

import { DotPageApiService } from '../../../../../services/dot-page-api/dot-page-api.service';
import {
    STYLE_EDITOR_INPUT_IDLE_SAVE_TIME,
    STYLE_EDITOR_SAVE_DEBOUNCE_TIME
} from '../../../../../shared/consts';
import { UVE_STATUS } from '../../../../../shared/enums';
import { ActionPayload, SelectedContentlet } from '../../../../../shared/models';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { PageData } from '../../../../../store/features/editor/models';
import { PageType } from '../../../../../store/models';

// Workaround: the `schema` input alias causes a compilation error when used directly.
const SCHEMA_INPUT_KEY = 'schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>;

/** Wrap an ActionPayload in the SelectedContentlet shape consumed by `editorSelected`. */
const toSelected = (payload: ActionPayload): SelectedContentlet => ({
    bounds: { x: 0, y: 0, width: 0, height: 0 },
    payload
});

type MockUveStore = {
    currentIndex: ReturnType<typeof signal<number>>;
    editorSelected: ReturnType<typeof signal<SelectedContentlet | null>>;
    pageAsset: ReturnType<typeof computed<DotCMSPageAsset | null>>;
    pageType: ReturnType<typeof signal<PageType>>;
    $pageData: ReturnType<typeof signal<PageData>>;
    saveStyleEditor: jest.Mock;
    rollbackPageAssetResponse: jest.Mock;
    addCurrentPageToHistory: jest.Mock;
    setPageAsset: jest.Mock;
    setUveStatus: jest.Mock;
    pageReload: jest.Mock;
};

const createMockSchema = (): StyleEditorFormSchema => ({
    contentType: 'test-content-type',
    sections: [
        {
            title: 'Typography',
            fields: [
                {
                    id: 'font-size',
                    label: 'Font Size',
                    type: 'input',
                    config: { inputType: 'number' }
                },
                {
                    id: 'font-family',
                    label: 'Font Family',
                    type: 'dropdown',
                    config: {
                        options: [
                            { label: 'Arial', value: 'Arial' },
                            { label: 'Helvetica', value: 'Helvetica' }
                        ]
                    }
                }
            ]
        },
        {
            title: 'Text Decoration',
            fields: [
                {
                    id: 'text-decoration',
                    label: 'Text Decoration',
                    type: 'checkboxGroup',
                    config: {
                        options: [
                            { label: 'Underline', value: 'underline' },
                            { label: 'Overline', value: 'overline' }
                        ]
                    }
                },
                {
                    id: 'alignment',
                    label: 'Alignment',
                    type: 'radio',
                    config: {
                        options: [
                            { label: 'Left', value: 'left' },
                            { label: 'Right', value: 'right' }
                        ]
                    }
                }
            ]
        }
    ]
});

const createMockPageAsset = (fontSize: number): DotCMSPageAsset =>
    ({
        page: {
            identifier: 'test-page',
            title: 'Test Page'
        },
        containers: {
            'test-container': {
                contentlets: {
                    'uuid-test-uuid': [
                        {
                            identifier: 'test-id',
                            inode: 'test-inode',
                            title: 'Test',
                            contentType: 'test-content-type',
                            dotStyleProperties: {
                                'font-size': fontSize,
                                'font-family': 'Arial',
                                'text-decoration': { underline: false, overline: false },
                                alignment: 'left'
                            }
                        }
                    ]
                }
            }
        }
    }) as unknown as DotCMSPageAsset;

describe('DotUveStyleEditorFormComponent', () => {
    let spectator: Spectator<DotUveStyleEditorFormComponent>;
    let mockUveStore: MockUveStore;

    const createComponent = createComponentFactory({
        component: DotUveStyleEditorFormComponent,
        imports: [AccordionModule, ButtonModule],
        providers: [
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotPageApiService),
            mockProvider(HttpClient),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            {
                provide: UVEStore,
                useFactory: () => mockUveStore
            }
        ]
    });

    const createTestComponent = () =>
        createComponent({ props: { [SCHEMA_INPUT_KEY]: createMockSchema() } });

    beforeEach(() => {
        const pageAssetSignal = signal<DotCMSPageAsset | null>(null);

        // Seed a minimal selection so $reloadSchemaEffect's contentletId gate
        // passes and the form gets built. Tests that need a different selection
        // (or none) override this signal explicitly.
        const defaultSelected = toSelected({
            contentlet: {
                identifier: 'test-id',
                inode: 'test-inode',
                title: 'Test',
                contentType: 'test-content-type'
            },
            container: {
                acceptTypes: 'test',
                identifier: 'test-container',
                maxContentlets: 1,
                uuid: 'test-uuid'
            },
            language_id: '1',
            pageContainers: [],
            pageId: 'test-page'
        });

        mockUveStore = {
            currentIndex: signal(0),
            editorSelected: signal<SelectedContentlet | null>(defaultSelected),
            pageAsset: computed(() => {
                const pageAsset = pageAssetSignal();
                return pageAsset ? { ...pageAsset, clientResponse: pageAsset } : null;
            }),
            pageType: signal(PageType.HEADLESS),
            $pageData: signal<PageData>({
                containers: [],
                personalization: 'dot:default',
                id: 'test-page',
                languageId: 1,
                personaTag: undefined
            }),
            saveStyleEditor: jest.fn().mockReturnValue(of({})),
            rollbackPageAssetResponse: jest.fn().mockReturnValue(true),
            addCurrentPageToHistory: jest.fn(),
            setPageAsset: jest.fn((payload: { pageAsset: DotCMSPageAsset | null }) => {
                pageAssetSignal.set(payload?.pageAsset ?? null);
            }),
            setUveStatus: jest.fn(),
            pageReload: jest.fn()
        };

        spectator = createTestComponent();
        spectator.detectChanges();
    });

    describe('component initialization', () => {
        it('should create the component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize form when schema is provided', fakeAsync(() => {
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.component.$form()).toBeTruthy();
            expect(spectator.component.$form()).toBeInstanceOf(FormGroup);
        }));

        it('should compute sections from schema', () => {
            const sections = spectator.component.$sections();
            expect(sections.length).toBe(2);
            expect(sections[0].title).toBe('Typography');
            expect(sections[1].title).toBe('Text Decoration');
        });
    });

    describe('form structure', () => {
        beforeEach(fakeAsync(() => {
            flushMicrotasks();
            spectator.detectChanges();
        }));

        it('should render accordion with sections', () => {
            expect(spectator.query(Accordion)).toBeDefined();
        });

        it('should render accordion tabs for each section', () => {
            expect(spectator.queryAll('p-accordion-panel').length).toBe(2);
        });
    });

    describe('field rendering', () => {
        beforeEach(fakeAsync(() => {
            flushMicrotasks();
            spectator.detectChanges();
        }));

        it('should render input field component', () => {
            expect(spectator.query('dot-uve-style-editor-field-input')).toBeTruthy();
        });

        it('should render dropdown field component', () => {
            expect(spectator.query('dot-uve-style-editor-field-dropdown')).toBeTruthy();
        });

        it('should render checkbox group field component', () => {
            expect(spectator.query('dot-uve-style-editor-field-checkbox-group')).toBeTruthy();
        });

        it('should render radio field component', () => {
            expect(spectator.query('dot-uve-style-editor-field-radio')).toBeTruthy();
        });
    });

    describe('initial values from contentlet styleProperties', () => {
        it('should populate form from pageAsset dotStyleProperties (not from editorSelected payload)', fakeAsync(() => {
            // Source of truth: pageAsset holds the persisted dotStyleProperties
            mockUveStore.setPageAsset({
                pageAsset: {
                    page: { identifier: 'test-page', title: 'Test Page' },
                    containers: {
                        'test-container': {
                            contentlets: {
                                'uuid-test-uuid': [
                                    {
                                        identifier: 'test-id',
                                        inode: 'test-inode',
                                        title: 'Test',
                                        contentType: 'test-content-type',
                                        dotStyleProperties: {
                                            'font-size': 20,
                                            'font-family': 'Helvetica',
                                            'text-decoration': { underline: false, overline: true },
                                            alignment: 'right'
                                        }
                                    }
                                ]
                            }
                        }
                    }
                } as unknown as DotCMSPageAsset
            });

            // editorSelected payload intentionally has NO dotStyleProperties to prove
            // the form reads from pageAsset, not from the click/bounds postMessage payload
            mockUveStore.editorSelected.set(
                toSelected({
                    contentlet: {
                        identifier: 'test-id',
                        inode: 'test-inode',
                        title: 'Test',
                        contentType: 'test-content-type'
                    },
                    container: {
                        acceptTypes: 'test',
                        identifier: 'test-container',
                        maxContentlets: 1,
                        uuid: 'test-uuid'
                    },
                    language_id: '1',
                    pageContainers: [],
                    pageId: 'test-page'
                })
            );

            spectator = createTestComponent();
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            const form = spectator.component.$form();
            expect(form?.get('font-size')?.value).toBe(20);
            expect(form?.get('font-family')?.value).toBe('Helvetica');
            expect(form?.get('alignment')?.value).toBe('right');

            const textDecorationGroup = form?.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(true);
        }));
    });

    describe('rollback and form restoration', () => {
        beforeEach(fakeAsync(() => {
            mockUveStore.editorSelected.set(
                toSelected({
                    contentlet: {
                        identifier: 'test-id',
                        inode: 'test-inode',
                        title: 'Test',
                        contentType: 'test-content-type',
                        dotStyleProperties: {
                            'font-size': 16,
                            'font-family': 'Arial',
                            'text-decoration': { underline: false, overline: false },
                            alignment: 'left'
                        }
                    },
                    container: {
                        acceptTypes: 'test',
                        identifier: 'test-container',
                        maxContentlets: 1,
                        uuid: 'test-uuid'
                    },
                    language_id: '1',
                    pageContainers: [],
                    pageId: 'test-page'
                })
            );

            mockUveStore.setPageAsset({ pageAsset: createMockPageAsset(16) });

            spectator = createTestComponent();
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();
        }));

        /** Configures saveStyleEditor to always fail and roll the store back to `rolledBackFontSize`. */
        const makeRollbackOnSaveFailure = (rolledBackFontSize: number) => {
            mockUveStore.saveStyleEditor.mockReturnValue(
                timer(0).pipe(
                    mergeMap(() => {
                        mockUveStore.setPageAsset({
                            pageAsset: createMockPageAsset(rolledBackFontSize)
                        });
                        return throwError(() => new Error('Save failed'));
                    })
                )
            );
        };

        /**
         * Patches font-size (a continuous input field) and commits it via blur/Enter,
         * advances past the coalescing window, flushes microtasks, then ticks once so
         * the delayed error (and rollback) run.
         */
        const triggerSaveAndWait = (fontSize: number) => {
            spectator.component.$form()?.patchValue({ 'font-size': fontSize });
            spectator.component.onInputCommit(); // input fields save on blur/Enter, not per keystroke
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME + 100);
            spectator.detectChanges();
            flushMicrotasks();
            tick(0); // Let saveStyleEditor's timer(0) emit so rollback runs before error handler
            spectator.detectChanges();
            // Drain the pending input idle-commit timer; its emission is deduped
            // by distinctUntilChanged (same value as the blur commit), so no extra save.
            flush();
        };

        it('should restore form values after rollback on save failure', fakeAsync(() => {
            expect(spectator.component.$form()?.get('font-size')?.value).toBe(16);

            makeRollbackOnSaveFailure(16);
            triggerSaveAndWait(20);

            expect(spectator.component.$form()?.get('font-size')?.value).toBe(16);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalled();
        }));

        it('should handle consecutive rollback failures correctly', fakeAsync(() => {
            makeRollbackOnSaveFailure(16);

            triggerSaveAndWait(20);
            expect(spectator.component.$form()?.get('font-size')?.value).toBe(16);

            triggerSaveAndWait(24);
            expect(spectator.component.$form()?.get('font-size')?.value).toBe(16);
        }));

        it('should rebuild form instance on rollback (not patch existing form)', fakeAsync(() => {
            const initialForm = spectator.component.$form();
            expect(initialForm?.get('font-size')?.value).toBe(16);

            makeRollbackOnSaveFailure(16);
            triggerSaveAndWait(20);

            const rebuiltForm = spectator.component.$form();
            expect(rebuiltForm).not.toBe(initialForm);
            expect(rebuiltForm?.get('font-size')?.value).toBe(16);
        }));

        it('should capture activeContentlet at form change time and pass it to saveStyleProperties', fakeAsync(() => {
            const originalActiveContentlet = mockUveStore.editorSelected()?.payload;
            expect(originalActiveContentlet).toBeTruthy();

            mockUveStore.saveStyleEditor.mockReturnValue(of({}));

            spectator.component.$form()?.patchValue({ 'font-size': 20 });
            spectator.component.onInputCommit(); // commit captures the contentlet at this moment

            // Change activeContentlet AFTER the commit but BEFORE the debounce fires.
            // The component must capture the contentlet at commit time, not at save time.
            mockUveStore.editorSelected.set(
                toSelected({
                    contentlet: {
                        identifier: 'new-test-id',
                        inode: 'new-test-inode',
                        title: 'New Test',
                        contentType: 'test-content-type',
                        dotStyleProperties: { 'font-size': 30 }
                    },
                    container: {
                        acceptTypes: 'test',
                        identifier: 'new-test-container',
                        maxContentlets: 1,
                        uuid: 'new-test-uuid'
                    },
                    language_id: '1',
                    pageContainers: [],
                    pageId: 'new-test-page'
                })
            );

            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME + 100);
            spectator.detectChanges();
            flush(); // drain the input idle-commit timer (deduped — same value as the commit)

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);

            const saveCall = mockUveStore.saveStyleEditor.mock.calls[0][0];
            expect(saveCall.contentletIdentifier).toBe(
                originalActiveContentlet?.contentlet.identifier
            );
            expect(saveCall.containerIdentifier).toBe(
                originalActiveContentlet?.container.identifier
            );
            expect(saveCall.pageId).toBe(originalActiveContentlet?.pageId);
        }));
    });

    describe('traditional page', () => {
        beforeEach(() => {
            mockUveStore.pageType.set(PageType.TRADITIONAL);
            mockUveStore.editorSelected.set(
                toSelected({
                    contentlet: {
                        identifier: 'test-id',
                        inode: 'test-inode',
                        title: 'Test',
                        contentType: 'test-content-type',
                        dotStyleProperties: {
                            'font-size': 16,
                            'font-family': 'Arial',
                            'text-decoration': { underline: false, overline: false },
                            alignment: 'left'
                        }
                    },
                    container: {
                        acceptTypes: 'test',
                        identifier: 'test-container',
                        maxContentlets: 1,
                        uuid: 'test-uuid'
                    },
                    language_id: '1',
                    pageContainers: [],
                    pageId: 'test-page'
                })
            );
            mockUveStore.setPageAsset({ pageAsset: createMockPageAsset(16) });
        });

        it('should save once after the input is committed (blur/Enter)', fakeAsync(() => {
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            mockUveStore.saveStyleEditor.mockClear();

            const form = spectator.component.$form();
            form?.patchValue({ 'font-size': 20 });
            spectator.component.onInputCommit();

            // Save should not fire before the coalescing window elapses
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME - 1);
            expect(mockUveStore.saveStyleEditor).not.toHaveBeenCalled();

            // After the window, exactly one save fires for the latest value
            tick(1);
            spectator.detectChanges();
            flush(); // drain the idle-commit timer — deduped, must not double-save

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    contentletIdentifier: 'test-id',
                    containerIdentifier: 'test-container',
                    styleProperties: expect.objectContaining({ 'font-size': 20 })
                })
            );
        }));

        it('should coalesce a rapid burst of changes into a single save (switchMap dedup)', fakeAsync(() => {
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            mockUveStore.saveStyleEditor.mockClear();

            const form = spectator.component.$form();

            // Rapid burst of commits within the coalescing window — only the last value should save
            form?.patchValue({ 'font-size': 18 });
            spectator.component.onInputCommit();
            tick(100);
            form?.patchValue({ 'font-size': 22 });
            spectator.component.onInputCommit();
            tick(100);
            form?.patchValue({ 'font-size': 24 });
            spectator.component.onInputCommit();

            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();
            flush(); // drain the idle-commit timer — deduped, must not double-save

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    styleProperties: expect.objectContaining({ 'font-size': 24 })
                })
            );
        }));

        it('should call setUveStatus(LOADING) before save and pageReload on success', fakeAsync(() => {
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            mockUveStore.setUveStatus.mockClear();
            mockUveStore.pageReload.mockClear();

            const form = spectator.component.$form();
            form?.patchValue({ 'font-size': 20 });
            spectator.component.onInputCommit();

            // Wait for the coalescing window to elapse before save fires
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();

            expect(mockUveStore.setUveStatus).toHaveBeenCalledWith(UVE_STATUS.LOADING);

            // Simulate async success (saveStyleEditor returns of({}))
            tick(0);
            spectator.detectChanges();

            expect(mockUveStore.pageReload).toHaveBeenCalled();

            flush(); // drain the idle-commit timer (deduped — no extra save)
        }));

        it('should NOT call updateHeadlessIframeOptimistically (no optimistic update)', fakeAsync(() => {
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            mockUveStore.setPageAsset.mockClear();

            const form = spectator.component.$form();
            form?.patchValue({ 'font-size': 20 });
            spectator.detectChanges();

            // Traditional pages do not use optimistic iframe update; setPageAsset
            // is only called from #updateIframeOptimistically (headless path)
            expect(mockUveStore.setPageAsset).not.toHaveBeenCalled();

            // Drain the idle-commit timer scheduled by the input change. It triggers
            // the (traditional) save path, which never touches setPageAsset either.
            flush();
            expect(mockUveStore.setPageAsset).not.toHaveBeenCalled();
        }));

        it('should call setUveStatus(LOADED) on save error and restore form', fakeAsync(() => {
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            const rolledBackResponse = createMockPageAsset(16);
            mockUveStore.saveStyleEditor.mockReturnValue(
                timer(0).pipe(
                    tap(() => mockUveStore.setPageAsset({ pageAsset: rolledBackResponse })),
                    mergeMap(() => throwError(() => new Error('Save failed')))
                )
            );

            mockUveStore.setUveStatus.mockClear();

            const form = spectator.component.$form();
            form?.patchValue({ 'font-size': 20 });
            spectator.component.onInputCommit();

            // Coalescing window, then the timer(0) inside the mocked save
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            tick(0);
            spectator.detectChanges();

            expect(mockUveStore.setUveStatus).toHaveBeenCalledWith(UVE_STATUS.LOADED);
            expect(spectator.component.$form()?.get('font-size')?.value).toBe(16);

            flush(); // drain the idle-commit timer (deduped — no extra save)
        }));
    });

    describe('commit semantics (input debounce vs live preview)', () => {
        // Headless page with a real selection + pageAsset so both the live preview
        // (optimistic iframe update) and the save path have something to act on.
        beforeEach(fakeAsync(() => {
            mockUveStore.pageType.set(PageType.HEADLESS);
            mockUveStore.editorSelected.set(
                toSelected({
                    contentlet: {
                        identifier: 'test-id',
                        inode: 'test-inode',
                        title: 'Test',
                        contentType: 'test-content-type',
                        dotStyleProperties: {
                            'font-size': 16,
                            'font-family': 'Arial',
                            'text-decoration': { underline: false, overline: false },
                            alignment: 'left'
                        }
                    },
                    container: {
                        acceptTypes: 'test',
                        identifier: 'test-container',
                        maxContentlets: 1,
                        uuid: 'test-uuid'
                    },
                    language_id: '1',
                    pageContainers: [],
                    pageId: 'test-page'
                })
            );
            mockUveStore.setPageAsset({ pageAsset: createMockPageAsset(16) });

            spectator = createTestComponent();
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            mockUveStore.saveStyleEditor.mockClear();
        }));

        it('should NOT save while typing in an input — even with slow keystrokes', fakeAsync(() => {
            const form = spectator.component.$form();

            // Simulate slow typing: changes spaced well beyond the coalescing window
            // (but below the idle window — i.e. a human pausing between letters)
            form?.patchValue({ 'font-size': 1 });
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME + 50);
            form?.patchValue({ 'font-size': 12 });
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME + 50);
            form?.patchValue({ 'font-size': 123 });
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME + 50);

            // Not a single save despite the slow typing
            expect(mockUveStore.saveStyleEditor).not.toHaveBeenCalled();

            // Finishing the field (blur/Enter) commits exactly one save with the final value
            spectator.component.onInputCommit();
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();
            flush(); // drain the idle-commit timer — deduped, must not double-save

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    styleProperties: expect.objectContaining({ 'font-size': 123 })
                })
            );
        }));

        it('should save automatically after the user stops typing (idle), without blur/Enter', fakeAsync(() => {
            const form = spectator.component.$form();

            form?.patchValue({ 'font-size': 42 });

            // Just before the idle window elapses: still nothing
            tick(STYLE_EDITOR_INPUT_IDLE_SAVE_TIME - 1);
            expect(mockUveStore.saveStyleEditor).not.toHaveBeenCalled();

            // Idle window + coalescing window → exactly one save, no interaction needed
            tick(1 + STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    styleProperties: expect.objectContaining({ 'font-size': 42 })
                })
            );
        }));

        it('should update the iframe on every input keystroke (live preview) before any commit', fakeAsync(() => {
            mockUveStore.setPageAsset.mockClear();
            const form = spectator.component.$form();

            form?.patchValue({ 'font-size': 18 });
            form?.patchValue({ 'font-size': 20 });

            // Optimistic preview runs per change; save does not until commit
            expect(mockUveStore.setPageAsset).toHaveBeenCalled();
            expect(mockUveStore.saveStyleEditor).not.toHaveBeenCalled();

            // The pending idle commit eventually persists the value once
            tick(STYLE_EDITOR_INPUT_IDLE_SAVE_TIME + STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
        }));

        it('should save a discrete field change without an explicit commit', fakeAsync(() => {
            const form = spectator.component.$form();

            form?.patchValue({ 'font-family': 'Helvetica' });
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    styleProperties: expect.objectContaining({ 'font-family': 'Helvetica' })
                })
            );
        }));

        it('should persist an in-progress (uncommitted) input value when a discrete field commits', fakeAsync(() => {
            const form = spectator.component.$form();

            // Type into the input but do NOT blur/commit it...
            form?.patchValue({ 'font-size': 99 });
            // ...then change a discrete field — its change commits and must carry the typed value
            form?.patchValue({ 'font-family': 'Helvetica' });

            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();
            flush(); // drain the idle-commit timer — deduped, must not double-save

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    styleProperties: expect.objectContaining({
                        'font-size': 99,
                        'font-family': 'Helvetica'
                    })
                })
            );
        }));
    });

    describe('cross-contentlet safety (selection changes while a commit is pending)', () => {
        // Headless page editing contentlet A. Each test then moves the selection
        // to contentlet B (rebuilding the form) while a commit from A is pending,
        // and asserts the save can only ever target A.
        beforeEach(fakeAsync(() => {
            mockUveStore.pageType.set(PageType.HEADLESS);
            mockUveStore.editorSelected.set(
                toSelected({
                    contentlet: {
                        identifier: 'test-id',
                        inode: 'test-inode',
                        title: 'Test',
                        contentType: 'test-content-type',
                        dotStyleProperties: {
                            'font-size': 16,
                            'font-family': 'Arial',
                            'text-decoration': { underline: false, overline: false },
                            alignment: 'left'
                        }
                    },
                    container: {
                        acceptTypes: 'test',
                        identifier: 'test-container',
                        maxContentlets: 1,
                        uuid: 'test-uuid'
                    },
                    language_id: '1',
                    pageContainers: [],
                    pageId: 'test-page'
                })
            );
            mockUveStore.setPageAsset({ pageAsset: createMockPageAsset(16) });

            spectator = createTestComponent();
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            mockUveStore.saveStyleEditor.mockClear();
        }));

        /** Moves the selection to contentlet B and lets the form rebuild run. */
        const selectContentletB = () => {
            mockUveStore.editorSelected.set(
                toSelected({
                    contentlet: {
                        identifier: 'contentlet-b-id',
                        inode: 'contentlet-b-inode',
                        title: 'Contentlet B',
                        contentType: 'test-content-type'
                    },
                    container: {
                        acceptTypes: 'test',
                        identifier: 'container-b',
                        maxContentlets: 1,
                        uuid: 'uuid-b'
                    },
                    language_id: '1',
                    pageContainers: [],
                    pageId: 'test-page'
                })
            );
            spectator.detectChanges();
            flushMicrotasks(); // #buildForm sets the new form in a microtask
            spectator.detectChanges();
        };

        it('should target the contentlet captured at change time when the idle commit fires after a selection change', fakeAsync(() => {
            const formA = spectator.component.$form();

            // Type on contentlet A (no blur — idle timer pending, context frozen = A)
            formA?.patchValue({ 'font-size': 20 });

            // Selection moves to B before the idle window elapses; the form rebuilds
            selectContentletB();
            expect(spectator.component.$form()).not.toBe(formA);

            // The pending idle commit fires — it must save against A, never B
            tick(STYLE_EDITOR_INPUT_IDLE_SAVE_TIME + STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();
            flush();

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    contentletIdentifier: 'test-id',
                    containerIdentifier: 'test-container',
                    styleProperties: expect.objectContaining({ 'font-size': 20 })
                })
            );
        }));

        it('should ignore a blur on the new form in the stale (pre-rebuild) form stream', fakeAsync(() => {
            const formA = spectator.component.$form();

            // Type on contentlet A (no blur), then move the selection to B
            formA?.patchValue({ 'font-size': 20 });
            selectContentletB();

            // Blur arrives on B's (unchanged) form. The stale A stream must not
            // react to it: no save fires within the coalescing window.
            spectator.component.onInputCommit();
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME + 1);
            spectator.detectChanges();

            expect(mockUveStore.saveStyleEditor).not.toHaveBeenCalled();

            // A's pending idle commit still completes — against A, with A's values
            tick(STYLE_EDITOR_INPUT_IDLE_SAVE_TIME + STYLE_EDITOR_SAVE_DEBOUNCE_TIME);
            spectator.detectChanges();
            flush();

            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledWith(
                expect.objectContaining({
                    contentletIdentifier: 'test-id',
                    styleProperties: expect.objectContaining({ 'font-size': 20 })
                })
            );
        }));

        it('should not save on blur when the form has no changes', fakeAsync(() => {
            // Tabbing through a field without typing must not hit the API
            spectator.component.onInputCommit();
            tick(STYLE_EDITOR_SAVE_DEBOUNCE_TIME + 1);
            spectator.detectChanges();
            flush();

            expect(mockUveStore.saveStyleEditor).not.toHaveBeenCalled();
        }));
    });
});
