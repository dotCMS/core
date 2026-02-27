import { InferInputSignals } from '@ngneat/spectator';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { computed, signal } from '@angular/core';
import { fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';

import { Accordion, AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { DotMessageService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSPageAsset } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { DotUveStyleEditorFormComponent } from './dot-uve-style-editor-form.component';

import { DotPageApiService } from '../../../../../services/dot-page-api.service';
import { STYLE_EDITOR_DEBOUNCE_TIME } from '../../../../../shared/consts';
import { ActionPayload } from '../../../../../shared/models';
import { UVEStore } from '../../../../../store/dot-uve.store';

// Workaround: the `schema` input alias causes a compilation error when used directly.
const SCHEMA_INPUT_KEY = 'schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>;

type MockUveStore = {
    currentIndex: ReturnType<typeof signal<number>>;
    editorActiveContentlet: ReturnType<typeof signal<ActionPayload | null>>;
    pageAsset: ReturnType<typeof computed<DotCMSPageAsset | null>>;
    saveStyleEditor: jest.Mock;
    rollbackPageAssetResponse: jest.Mock;
    addCurrentPageToHistory: jest.Mock;
    setPageAsset: jest.Mock;
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

        mockUveStore = {
            currentIndex: signal(0),
            editorActiveContentlet: signal(null),
            pageAsset: computed(() => {
                const pageAsset = pageAssetSignal();
                return pageAsset ? { ...pageAsset, clientResponse: pageAsset } : null;
            }),
            saveStyleEditor: jest.fn().mockReturnValue(of({})),
            rollbackPageAssetResponse: jest.fn().mockReturnValue(true),
            addCurrentPageToHistory: jest.fn(),
            setPageAsset: jest.fn((response: DotCMSPageAsset | null) => {
                pageAssetSignal.set(response);
            })
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
            expect(spectator.queryAll('.uve-accordion-tab').length).toBe(2);
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
        it('should use styleProperties from activeContentlet when available', fakeAsync(() => {
            mockUveStore.editorActiveContentlet.set({
                contentlet: {
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
            mockUveStore.editorActiveContentlet.set({
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
            });

            mockUveStore.setPageAsset(createMockPageAsset(16));

            spectator = createTestComponent();
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();
        }));

        /** Configures saveStyleEditor to always fail and roll the store back to `rolledBackFontSize`. */
        const makeRollbackOnSaveFailure = (rolledBackFontSize: number) => {
            mockUveStore.saveStyleEditor.mockReturnValue(
                throwError(() => {
                    mockUveStore.setPageAsset(createMockPageAsset(rolledBackFontSize));
                    return new Error('Save failed');
                })
            );
        };

        /** Patches font-size, advances past the debounce, and flushes microtasks. */
        const triggerSaveAndWait = (fontSize: number) => {
            spectator.component.$form()?.patchValue({ 'font-size': fontSize });
            tick(STYLE_EDITOR_DEBOUNCE_TIME + 100);
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();
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
            const originalActiveContentlet = mockUveStore.editorActiveContentlet();
            expect(originalActiveContentlet).toBeTruthy();

            mockUveStore.saveStyleEditor.mockReturnValue(of({}));

            spectator.component.$form()?.patchValue({ 'font-size': 20 });

            // Change activeContentlet AFTER the form change but BEFORE the debounce fires.
            // The component must capture the contentlet at change time, not at save time.
            mockUveStore.editorActiveContentlet.set({
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
            });

            tick(STYLE_EDITOR_DEBOUNCE_TIME + 100);
            spectator.detectChanges();

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
});
