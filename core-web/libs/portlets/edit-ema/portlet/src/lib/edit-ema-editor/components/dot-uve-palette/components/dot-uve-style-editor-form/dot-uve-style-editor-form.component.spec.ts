import { InferInputSignals } from '@ngneat/spectator';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormGroup } from '@angular/forms';

import { Accordion, AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';

import { StyleEditorFormSchema } from '@dotcms/uve';

import { DotUveStyleEditorFormComponent } from './dot-uve-style-editor-form.component';

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
                        inputType: 'number',
                        defaultValue: 16
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
                        ],
                        defaultValue: 'Arial'
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
                        ],
                        defaultValue: {
                            underline: true,
                            overline: false
                        }
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
                        ],
                        defaultValue: 'left'
                    }
                }
            ]
        }
    ]
});

describe('DotUveStyleEditorFormComponent', () => {
    let spectator: Spectator<DotUveStyleEditorFormComponent>;

    const createComponent = createComponentFactory({
        component: DotUveStyleEditorFormComponent,
        imports: [AccordionModule, ButtonModule]
    });

    beforeEach(() => {
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

    describe('form controls', () => {
        it('should create form controls for input field', () => {
            const form = spectator.component.$form();
            expect(form).toBeTruthy();

            const fontSizeControl = form?.get('font-size');
            expect(fontSizeControl).toBeTruthy();
            expect(fontSizeControl?.value).toBe(16);
        });

        it('should create form controls for dropdown field', () => {
            const form = spectator.component.$form();
            const fontFamilyControl = form?.get('font-family');
            expect(fontFamilyControl).toBeTruthy();
            expect(fontFamilyControl?.value).toBe('Arial');
        });

        it('should create form group for checkboxGroup field', () => {
            const form = spectator.component.$form();
            const textDecorationGroup = form?.get('text-decoration') as FormGroup;
            expect(textDecorationGroup).toBeTruthy();
            expect(textDecorationGroup).toBeInstanceOf(FormGroup);

            const underlineControl = textDecorationGroup.get('underline');
            const overlineControl = textDecorationGroup.get('overline');
            expect(underlineControl?.value).toBe(true);
            expect(overlineControl?.value).toBe(false);
        });

        it('should create form controls for radio field', () => {
            const form = spectator.component.$form();
            const alignmentControl = form?.get('alignment');
            expect(alignmentControl).toBeTruthy();
            expect(alignmentControl?.value).toBe('left');
        });
    });

    describe('default values', () => {
        it('should use defaultValue for input field when provided', () => {
            const form = spectator.component.$form();
            const fontSizeControl = form?.get('font-size');
            expect(fontSizeControl?.value).toBe(16);
        });

        it('should use defaultValue for dropdown field when provided', () => {
            const form = spectator.component.$form();
            const fontFamilyControl = form?.get('font-family');
            expect(fontFamilyControl?.value).toBe('Arial');
        });

        it('should use defaultValue for checkboxGroup field when provided', () => {
            const form = spectator.component.$form();
            const textDecorationGroup = form?.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(true);
            expect(textDecorationGroup.get('overline')?.value).toBe(false);
        });

        it('should use defaultValue for radio field when provided', () => {
            const form = spectator.component.$form();
            const alignmentControl = form?.get('alignment');
            expect(alignmentControl?.value).toBe('left');
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

    describe('schema changes', () => {
        it('should rebuild form when schema changes', () => {
            const initialForm = spectator.component.$form();

            const newSchema: StyleEditorFormSchema = {
                contentType: 'new-content-type',
                sections: [
                    {
                        title: 'New Section',
                        fields: [
                            {
                                id: 'new-field',
                                label: 'New Field',
                                type: 'input',
                                config: {
                                    inputType: 'text',
                                    defaultValue: 'test'
                                }
                            }
                        ]
                    }
                ]
            };

            spectator.setInput('schema', newSchema);
            spectator.detectChanges();

            const newForm = spectator.component.$form();
            expect(newForm).toBeTruthy();
            expect(newForm).not.toBe(initialForm);
            expect(newForm?.get('new-field')?.value).toBe('test');
        });
    });
});
