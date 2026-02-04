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
                    config: {
                        inputType: 'number'
                    }
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

describe('DotUveStyleEditorFormComponent', () => {
    let spectator: Spectator<DotUveStyleEditorFormComponent>;
    let mockUveStore: {
        currentIndex: ReturnType<typeof signal<number>>;
        activeContentlet: ReturnType<typeof signal<ActionPayload | null>>;
        graphqlResponse: ReturnType<typeof signal<DotCMSPageAsset | null>>;
        $customGraphqlResponse: ReturnType<typeof computed<DotCMSPageAsset | null>>;
        saveStyleEditor: jest.Mock;
        rollbackGraphqlResponse: jest.Mock;
        addHistory: jest.Mock;
        setGraphqlResponse: jest.Mock;
    };

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

    const createMockGraphQLResponse = (fontSize: number): DotCMSPageAsset =>
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
                                    'text-decoration': {
                                        underline: false,
                                        overline: false
                                    },
                                    alignment: 'left'
                                }
                            }
                        ]
                    }
                }
            }
        }) as unknown as DotCMSPageAsset;

    beforeEach(() => {
        const graphqlResponseSignal = signal<DotCMSPageAsset | null>(null);
        const customGraphqlResponseComputed = computed(() => graphqlResponseSignal());

        mockUveStore = {
            currentIndex: signal(0),
            activeContentlet: signal(null),
            graphqlResponse: graphqlResponseSignal,
            $customGraphqlResponse: customGraphqlResponseComputed,
            saveStyleEditor: jest.fn().mockReturnValue(of({})),
            rollbackGraphqlResponse: jest.fn().mockReturnValue(true),
            addHistory: jest.fn(),
            setGraphqlResponse: jest.fn((response: DotCMSPageAsset | null) => {
                graphqlResponseSignal.set(response);
            })
        };

        spectator = createComponent({
            props: {
                // This is a workaround to pass an input with alias.
                // The schema alias trigger a compilation error, and $schema dont work.
                ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                    createMockSchema()
            }
        });
        spectator.detectChanges();
    });

    describe('component initialization', () => {
        it('should create the component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize form when schema is provided', fakeAsync(() => {
            // Flush microtasks to allow form to be built
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
            // Ensure form is built before testing structure
            flushMicrotasks();
            spectator.detectChanges();
        }));

        it('should render accordion with sections', () => {
            const accordion = spectator.query(Accordion);
            expect(accordion).toBeDefined();
        });

        it('should render accordion tabs for each section', () => {
            const accordionTabs = spectator.queryAll('.uve-accordion-tab');
            expect(accordionTabs.length).toBe(2);
        });
    });

    describe('field rendering', () => {
        beforeEach(fakeAsync(() => {
            // Ensure form is built before testing field rendering
            flushMicrotasks();
            spectator.detectChanges();
        }));

        it('should render input field component', () => {
            const inputField = spectator.query('dot-uve-style-editor-field-input');
            expect(inputField).toBeTruthy();
        });

        it('should render dropdown field component', () => {
            const dropdownField = spectator.query('dot-uve-style-editor-field-dropdown');
            expect(dropdownField).toBeTruthy();
        });

        it('should render checkbox group field component', () => {
            const checkboxField = spectator.query('dot-uve-style-editor-field-checkbox-group');
            expect(checkboxField).toBeTruthy();
        });

        it('should render radio field component', () => {
            const radioField = spectator.query('dot-uve-style-editor-field-radio');
            expect(radioField).toBeTruthy();
        });
    });

    describe('initial values from contentlet styleProperties', () => {
        it('should use styleProperties from activeContentlet when available', fakeAsync(() => {
            const styleProperties = {
                'font-size': 20,
                'font-family': 'Helvetica',
                'text-decoration': {
                    underline: false,
                    overline: true
                },
                alignment: 'right'
            };

            // Set activeContentlet BEFORE creating component
            mockUveStore.activeContentlet.set({
                contentlet: {
                    identifier: 'test-id',
                    inode: 'test-inode',
                    title: 'Test',
                    contentType: 'test-content-type',
                    dotStyleProperties: styleProperties
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

            // Create a NEW component instance with the schema already set
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();

            // Flush microtasks to allow form to be built
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
        beforeEach(() => {
            // Set up activeContentlet with initial style properties
            // Include ALL fields from the schema to match the graphqlResponse structure
            mockUveStore.activeContentlet.set({
                contentlet: {
                    identifier: 'test-id',
                    inode: 'test-inode',
                    title: 'Test',
                    contentType: 'test-content-type',
                    dotStyleProperties: {
                        'font-size': 16,
                        'font-family': 'Arial',
                        'text-decoration': {
                            underline: false,
                            overline: false
                        },
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

            // Set initial graphqlResponse
            const initialResponse = createMockGraphQLResponse(16);
            mockUveStore.graphqlResponse.set(initialResponse);
        });

        it('should restore form values after rollback on save failure', fakeAsync(() => {
            // Create component with activeContentlet
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();

            // Flush microtasks to allow form to be built
            flushMicrotasks();
            spectator.detectChanges();

            let form = spectator.component.$form();
            expect(form?.get('font-size')?.value).toBe(16);

            // Mock saveStyleEditor to fail and simulate rollback by updating graphqlResponse
            const rolledBackResponse = createMockGraphQLResponse(16);
            mockUveStore.saveStyleEditor.mockReturnValue(
                throwError(() => {
                    // Simulate store's rollback behavior: update graphqlResponse to rolled-back state
                    mockUveStore.graphqlResponse.set(rolledBackResponse);
                    return new Error('Save failed');
                })
            );

            // Change form value (this triggers the save flow)
            // activeContentlet is captured at the time of form change (before debounce)
            form?.patchValue({ 'font-size': 20 });
            tick(STYLE_EDITOR_DEBOUNCE_TIME + 100); // Wait for debounce + error handling
            spectator.detectChanges(); // Ensure change detection runs after rollback

            // Flush microtasks after rollback to allow form to be rebuilt
            flushMicrotasks();
            spectator.detectChanges();

            // Get the NEW form reference after rollback (form is rebuilt, not patched)
            form = spectator.component.$form();

            // Verify form is restored to rolled-back value
            expect(form?.get('font-size')?.value).toBe(16);
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalled();
        }));

        it('should handle consecutive rollback failures correctly', fakeAsync(() => {
            // Create component with activeContentlet
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();

            // Flush microtasks to allow form to be built
            flushMicrotasks();
            spectator.detectChanges();

            const rolledBackResponse = createMockGraphQLResponse(16);

            // Mock saveStyleEditor to always fail and rollback to 16
            mockUveStore.saveStyleEditor.mockReturnValue(
                throwError(() => {
                    mockUveStore.graphqlResponse.set(rolledBackResponse);
                    return new Error('Save failed');
                })
            );

            // First failure: change from 16 to 20, then fail
            // activeContentlet is captured at the time of form change (before debounce)
            let form = spectator.component.$form();
            form?.patchValue({ 'font-size': 20 });
            tick(STYLE_EDITOR_DEBOUNCE_TIME + 100);
            spectator.detectChanges(); // Ensure change detection runs after rollback

            // Flush microtasks after first rollback to allow form to be rebuilt
            flushMicrotasks();
            spectator.detectChanges();

            // Get the NEW form reference after first rollback (form is rebuilt)
            form = spectator.component.$form();
            expect(form?.get('font-size')?.value).toBe(16); // Rolled back to 16

            // Second failure: Get fresh form reference before patching
            // This ensures we're patching the current form instance
            // activeContentlet is captured again at the time of form change
            form = spectator.component.$form();
            form?.patchValue({ 'font-size': 24 });
            tick(STYLE_EDITOR_DEBOUNCE_TIME + 100);
            spectator.detectChanges(); // Ensure change detection runs after rollback

            // Flush microtasks after second rollback to allow form to be rebuilt
            flushMicrotasks();
            spectator.detectChanges();

            // Get the NEW form reference after second rollback
            form = spectator.component.$form();
            expect(form?.get('font-size')?.value).toBe(16); // Should rollback to 16, not 24
        }));

        it('should rebuild form instance on rollback (not patch existing form)', fakeAsync(() => {
            // Create component with activeContentlet
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();

            // Flush microtasks to allow form to be built
            flushMicrotasks();
            spectator.detectChanges();

            // Get initial form reference
            const initialForm = spectator.component.$form();
            expect(initialForm?.get('font-size')?.value).toBe(16);

            // Mock saveStyleEditor to fail and simulate rollback
            const rolledBackResponse = createMockGraphQLResponse(16);
            mockUveStore.saveStyleEditor.mockReturnValue(
                throwError(() => {
                    mockUveStore.graphqlResponse.set(rolledBackResponse);
                    return new Error('Save failed');
                })
            );

            // Change form value to trigger save and rollback
            // activeContentlet is captured at the time of form change (before debounce)
            initialForm?.patchValue({ 'font-size': 20 });
            tick(STYLE_EDITOR_DEBOUNCE_TIME + 100);
            spectator.detectChanges(); // Ensure change detection runs after rollback

            // Flush microtasks after rollback to allow form to be rebuilt
            flushMicrotasks();
            spectator.detectChanges();

            // Get form reference after rollback
            const rebuiltForm = spectator.component.$form();

            // Verify form was REBUILT (new instance), not patched
            expect(rebuiltForm).not.toBe(initialForm);
            expect(rebuiltForm?.get('font-size')?.value).toBe(16);
        }));

        it('should capture activeContentlet at form change time and pass it to saveStyleProperties', fakeAsync(() => {
            // Create component with activeContentlet
            spectator = createComponent({
                props: {
                    ['schema' as keyof InferInputSignals<DotUveStyleEditorFormComponent>]:
                        createMockSchema()
                }
            });
            spectator.detectChanges();

            // Flush microtasks to allow form to be built
            flushMicrotasks();
            spectator.detectChanges();

            const initialActiveContentlet = mockUveStore.activeContentlet();
            expect(initialActiveContentlet).toBeTruthy();

            // Mock saveStyleEditor to succeed and verify it receives the correct activeContentlet
            mockUveStore.saveStyleEditor.mockReturnValue(of({}));

            // Change form value (this triggers the save flow)
            // activeContentlet is captured at the time of form change (before debounce)
            const form = spectator.component.$form();
            form?.patchValue({ 'font-size': 20 });

            // Change activeContentlet AFTER form change but BEFORE debounce completes
            // This tests that the original activeContentlet is captured and used
            const newActiveContentlet = {
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
            };
            mockUveStore.activeContentlet.set(newActiveContentlet);

            // Wait for debounce to complete
            tick(STYLE_EDITOR_DEBOUNCE_TIME + 100);
            spectator.detectChanges();

            // Verify saveStyleEditor was called with the ORIGINAL activeContentlet
            // (captured at form change time, not the new one)
            expect(mockUveStore.saveStyleEditor).toHaveBeenCalledTimes(1);
            const saveCall = mockUveStore.saveStyleEditor.mock.calls[0][0];
            expect(saveCall.contentletIdentifier).toBe(
                initialActiveContentlet?.contentlet.identifier
            );
            expect(saveCall.containerIdentifier).toBe(
                initialActiveContentlet?.container.identifier
            );
            expect(saveCall.pageId).toBe(initialActiveContentlet?.pageId);
        }));
    });
});
