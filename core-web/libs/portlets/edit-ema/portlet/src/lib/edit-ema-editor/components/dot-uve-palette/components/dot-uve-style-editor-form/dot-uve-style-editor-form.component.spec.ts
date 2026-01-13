import { InferInputSignals } from '@ngneat/spectator';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { HttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { Accordion, AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { DotMessageService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { DotUveStyleEditorFormComponent } from './dot-uve-style-editor-form.component';

import { DotPageApiService } from '../../../../../services/dot-page-api.service';
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

    beforeEach(() => {
        mockUveStore = {
            currentIndex: signal(0),
            activeContentlet: signal(null)
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

        it('should initialize form when schema is provided', () => {
            expect(spectator.component.$form()).toBeTruthy();
            expect(spectator.component.$form()).toBeInstanceOf(FormGroup);
        });

        it('should compute sections from schema', () => {
            const sections = spectator.component.$sections();
            expect(sections.length).toBe(2);
            expect(sections[0].title).toBe('Typography');
            expect(sections[1].title).toBe('Text Decoration');
        });
    });

    describe('form structure', () => {
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
        it('should use styleProperties from activeContentlet when available', () => {
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
                    styleProperties
                },
                container: {
                    acceptTypes: 'test',
                    identifier: 'test-container',
                    maxContentlets: 1,
                    variantId: 'test-variant',
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

            const form = spectator.component.$form();
            expect(form?.get('font-size')?.value).toBe(20);
            expect(form?.get('font-family')?.value).toBe('Helvetica');
            expect(form?.get('alignment')?.value).toBe('right');

            const textDecorationGroup = form?.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(true);
        });
    });
});
