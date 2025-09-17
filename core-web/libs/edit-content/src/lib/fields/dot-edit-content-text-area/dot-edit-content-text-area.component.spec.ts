import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';
import { createFakeContentlet, createFakeTextAreaField } from '@dotcms/utils-testing';

import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area.component';
import {
    AvailableEditorTextArea,
    TextAreaEditorOptions
} from './dot-edit-content-text-area.constants';

import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';

const TEXT_AREA_FIELD_MOCK = createFakeTextAreaField({
    variable: 'someTextArea'
});

describe('DotEditContentTextAreaComponent', () => {
    let spectator: SpectatorHost<DotEditContentTextAreaComponent>;
    let textArea: Element;

    const createHost = createHostFactory({
        component: DotEditContentTextAreaComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [
            DotLanguageVariableSelectorComponent,
            DotEditContentMonacoEditorControlComponent
        ],
        providers: [
            mockProvider(DotLanguagesService),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    describe('should have the variable as id', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_AREA_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: TEXT_AREA_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [TEXT_AREA_FIELD_MOCK.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
            textArea = spectator.query(byTestId(TEXT_AREA_FIELD_MOCK.variable));
        });

        it('should have the variable as id', () => {
            expect(textArea.getAttribute('id')).toBe(TEXT_AREA_FIELD_MOCK.variable);
        });

        it('should have min height as 9.375rem and resize as vertical', () => {
            expect(textArea.getAttribute('style')).toBe('min-height: 9.375rem; resize: vertical;');
        });

        it('should have editor selector dropdown', () => {
            const editorDropdown = spectator.query(byTestId('editor-selector'));
            expect(editorDropdown).toBeTruthy();
        });

        it('should have editor selector with correct options', () => {
            const dropdown = spectator.query(byTestId('editor-selector'));
            expect(dropdown).toBeTruthy();

            // Verify dropdown options
            const options = TextAreaEditorOptions;
            expect(options.length).toBe(2);
            expect(options).toEqual([
                { label: 'Plain Text', value: AvailableEditorTextArea.PlainText },
                { label: 'Code', value: AvailableEditorTextArea.Monaco }
            ]);
        });

        it('should have language variable selector component', () => {
            const languageVariableSelector = spectator.query(DotLanguageVariableSelectorComponent);
            expect(languageVariableSelector).toBeTruthy();
        });

        it('should change editor when dropdown value changes', () => {
            // Initial state
            expect(spectator.component.$displayedEditor()).toBe(AvailableEditorTextArea.PlainText);

            // Simulate editor change
            spectator.component.onEditorChange(AvailableEditorTextArea.Monaco);
            spectator.detectChanges();

            // Verify state change
            expect(spectator.component.$displayedEditor()).toBe(AvailableEditorTextArea.Monaco);
        });

        it('should update contentlet disabledWYSIWYG property when switching editors', () => {
            // Arrange: Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            spectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to Monaco editor
            spectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

            // Assert: Event should be emitted with updated array
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([
                `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
            ]);
        });

        it('should call onSelectLanguageVariable when language variable is selected', () => {
            // Spy on component method
            const spy = jest.spyOn(spectator.component, 'onSelectLanguageVariable');

            // Get language variable selector component
            const languageVariableSelector = spectator.query(DotLanguageVariableSelectorComponent);

            // Trigger onSelectLanguageVariable event
            const testVariable = '${languageVariable}';
            languageVariableSelector.onSelectLanguageVariable.emit(testVariable);

            // Verify method called with correct parameter
            expect(spy).toHaveBeenCalledWith(testVariable);
        });

        it('should switch to Monaco editor when user selects Code Editor option', () => {
            // Arrange: Spy on the method
            const spy = jest.spyOn(spectator.component, 'onEditorChange');

            // Act: Simulate user selecting Monaco editor from dropdown
            spectator.component.$selectedEditorDropdown.set(AvailableEditorTextArea.PlainText); // Initial state

            // Simulate onChange event without triggering detectChanges
            spectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

            // Assert: Method called and editor switched
            expect(spy).toHaveBeenCalledWith(AvailableEditorTextArea.Monaco);
            expect(spectator.component.$displayedEditor()).toBe(AvailableEditorTextArea.Monaco);
        });

        it('should handle inserting language variable when user selects it in plaintext mode', () => {
            // Mock the insertLanguageVariableInTextarea private method
            const insertLanguageVariableInTextareaMock = jest.fn();
            spectator.component['insertLanguageVariableInTextarea'] =
                insertLanguageVariableInTextareaMock;

            // Keep in PlainText mode
            spectator.component.$displayedEditor.set(AvailableEditorTextArea.PlainText);

            spectator.detectChanges();

            // Act: Simulate language variable selection
            const testVariable = '${testLanguageVariable}';
            spectator.component.onSelectLanguageVariable(testVariable);
            spectator.detectChanges();

            // Assert: Private method called with correct parameters
            expect(insertLanguageVariableInTextareaMock).toHaveBeenCalled();
            expect(insertLanguageVariableInTextareaMock.mock.calls[0][1]).toBe(testVariable);
        });

        it('should insert language variable into Monaco editor when in Monaco mode', () => {
            // Mock the insertLanguageVariableInMonaco private method
            const insertLanguageVariableInMonacoMock = jest.fn();
            spectator.component['insertLanguageVariableInMonaco'] =
                insertLanguageVariableInMonacoMock;

            // Switch to Monaco editor mode
            spectator.component.$displayedEditor.set(AvailableEditorTextArea.Monaco);

            // Act: Simulate language variable selection
            const testVariable = '${monacoTestVariable}';
            spectator.component.onSelectLanguageVariable(testVariable);

            // Assert: Private method called with correct parameter
            expect(insertLanguageVariableInMonacoMock).toHaveBeenCalledWith(testVariable);
        });
    });

    it('should preserve existing disabledWYSIWYG entries when updating editor', () => {
        // Arrange: Create a new spectator with existing disabledWYSIWYG entries
        const existingEntries = ['otherField', 'wysiwygField'];

        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                        disabledWYSIWYG: new FormControl(existingEntries)
                    }),
                    field: TEXT_AREA_FIELD_MOCK,
                    contentlet: createFakeContentlet({
                        disabledWYSIWYG: existingEntries,
                        [TEXT_AREA_FIELD_MOCK.variable]: ''
                    })
                }
            }
        );
        spectator.detectChanges();

        const disabledWYSIWYGChangeSpy = jest.fn();
        spectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

        // Act: Switch to Monaco editor
        spectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

        // Assert: Should preserve existing entries and add new one
        const expectedEntries = [
            ...existingEntries,
            `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
        ];
        expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith(expectedEntries);
    });

    it('should clear disabledWYSIWYG when switching back to PlainText editor', () => {
        // Arrange: Create a new spectator with Monaco editor as active

        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                        disabledWYSIWYG: new FormControl([
                            `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
                        ])
                    }),
                    field: TEXT_AREA_FIELD_MOCK,
                    contentlet: createFakeContentlet({
                        disabledWYSIWYG: [`${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`],
                        [TEXT_AREA_FIELD_MOCK.variable]: ''
                    })
                }
            }
        );
        spectator.detectChanges();

        const disabledWYSIWYGChangeSpy = jest.fn();
        spectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

        // Act: Switch back to PlainText editor
        spectator.component.onEditorChange(AvailableEditorTextArea.PlainText);

        // Assert: Should clear disabledWYSIWYG
        expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([]);
    });

    it('should handle contentlet without disabledWYSIWYG property gracefully', () => {
        // Arrange: Create a new spectator with contentlet without disabledWYSIWYG property
        const contentletWithoutProperty = createFakeContentlet({
            [TEXT_AREA_FIELD_MOCK.variable]: ''
        });
        // Explicitly remove the property to simulate real scenarios
        delete contentletWithoutProperty.disabledWYSIWYG;

        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [TEXT_AREA_FIELD_MOCK.variable]: new FormControl()
                    }),
                    field: TEXT_AREA_FIELD_MOCK,
                    contentlet: contentletWithoutProperty
                }
            }
        );
        spectator.detectChanges();

        const disabledWYSIWYGChangeSpy = jest.fn();
        spectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

        // Act: Switch to Monaco editor
        spectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

        // Assert: Should emit the new disabledWYSIWYG value
        expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([
            `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
        ]);
    });

    describe('disabledWYSIWYGChange', () => {
        it('should emit disabledWYSIWYGChange when switching from PlainText to Monaco editor', () => {
            // Arrange: Create a contentlet with empty content and no disabled settings
            const contentletMock = createFakeContentlet({
                [TEXT_AREA_FIELD_MOCK.variable]: '',
                disabledWYSIWYG: []
            });

            const switchSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([])
                        }),
                        field: TEXT_AREA_FIELD_MOCK,
                        contentlet: contentletMock
                    }
                }
            );
            switchSpectator.detectChanges();

            // Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            switchSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to Monaco editor
            switchSpectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

            // Assert: disabledWYSIWYGChange should be emitted with field variable + @ToggleEditor
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([
                `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
            ]);
            expect(switchSpectator.component.$displayedEditor()).toBe(
                AvailableEditorTextArea.Monaco
            );
        });

        it('should emit disabledWYSIWYGChange when switching from Monaco back to PlainText editor', () => {
            // Arrange: Create a contentlet with Monaco editor already enabled
            const contentletMock = createFakeContentlet({
                [TEXT_AREA_FIELD_MOCK.variable]: '',
                disabledWYSIWYG: [`${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`]
            });

            const switchBackSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([
                                `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
                            ])
                        }),
                        field: TEXT_AREA_FIELD_MOCK,
                        contentlet: contentletMock
                    }
                }
            );
            switchBackSpectator.detectChanges();

            // Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            switchBackSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to PlainText editor
            switchBackSpectator.component.onEditorChange(AvailableEditorTextArea.PlainText);

            // Assert: disabledWYSIWYG should be cleared for PlainText
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([]);
            expect(switchBackSpectator.component.$displayedEditor()).toBe(
                AvailableEditorTextArea.PlainText
            );
        });

        it('should correctly initialize with Monaco editor when contentlet has preference set', () => {
            // Arrange: Contentlet with Monaco editor preference already set
            const contentletWithMonaco = createFakeContentlet({
                [TEXT_AREA_FIELD_MOCK.variable]: '',
                disabledWYSIWYG: [`${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`]
            });

            const initSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([
                                `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
                            ])
                        }),
                        field: TEXT_AREA_FIELD_MOCK,
                        contentlet: contentletWithMonaco
                    }
                }
            );
            initSpectator.detectChanges();

            // Assert: Should initialize with Monaco editor
            expect(initSpectator.component.$contentEditorUsed()).toBe(
                AvailableEditorTextArea.Monaco
            );
            expect(initSpectator.component.$displayedEditor()).toBe(AvailableEditorTextArea.Monaco);
            expect(initSpectator.component.$selectedEditorDropdown()).toBe(
                AvailableEditorTextArea.Monaco
            );
        });

        it('should preserve other field entries when updating current field editor preference', () => {
            // Arrange: Contentlet with existing entries for other fields
            const contentletMock = createFakeContentlet({
                [TEXT_AREA_FIELD_MOCK.variable]: '',
                disabledWYSIWYG: ['otherField', 'wysiwygField']
            });

            const preserveSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl(['otherField', 'wysiwygField'])
                        }),
                        field: TEXT_AREA_FIELD_MOCK,
                        contentlet: contentletMock
                    }
                }
            );
            preserveSpectator.detectChanges();

            // Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            preserveSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to Monaco editor for current field
            preserveSpectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

            // Assert: Should preserve other entries and add current field with @ToggleEditor suffix
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([
                'otherField',
                'wysiwygField',
                `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
            ]);
        });

        it('should handle smooth editor switching workflow without conflicts', () => {
            // Arrange: Start with clean contentlet
            const contentletEmpty = createFakeContentlet({
                [TEXT_AREA_FIELD_MOCK.variable]: '',
                disabledWYSIWYG: []
            });

            const workflowSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([])
                        }),
                        field: TEXT_AREA_FIELD_MOCK,
                        contentlet: contentletEmpty
                    }
                }
            );
            workflowSpectator.detectChanges();

            const disabledWYSIWYGChangeSpy = jest.fn();
            workflowSpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act 1: Switch to Monaco
            workflowSpectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

            // Assert 1: Monaco active
            expect(workflowSpectator.component.$displayedEditor()).toBe(
                AvailableEditorTextArea.Monaco
            );
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([
                `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
            ]);

            // Act 2: Switch back to PlainText
            workflowSpectator.component.onEditorChange(AvailableEditorTextArea.PlainText);

            // Assert 2: PlainText active and preferences cleared
            expect(workflowSpectator.component.$displayedEditor()).toBe(
                AvailableEditorTextArea.PlainText
            );
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([]);
        });

        it('should handle contentlet without disabledWYSIWYG property', () => {
            // Arrange: Create a contentlet without disabledWYSIWYG property
            const contentletMock = createFakeContentlet({
                [TEXT_AREA_FIELD_MOCK.variable]: ''
            });
            // Explicitly remove the property to simulate real scenarios
            delete contentletMock.disabledWYSIWYG;

            const noPropertySpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-area [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_AREA_FIELD_MOCK.variable]: new FormControl(),
                            disabledWYSIWYG: new FormControl([])
                        }),
                        field: TEXT_AREA_FIELD_MOCK,
                        contentlet: contentletMock
                    }
                }
            );
            noPropertySpectator.detectChanges();

            // Spy on the output event
            const disabledWYSIWYGChangeSpy = jest.fn();
            noPropertySpectator.output('disabledWYSIWYGChange').subscribe(disabledWYSIWYGChangeSpy);

            // Act: Switch to Monaco editor
            noPropertySpectator.component.onEditorChange(AvailableEditorTextArea.Monaco);

            // Assert: disabledWYSIWYGChange should be emitted with new entry
            expect(disabledWYSIWYGChangeSpy).toHaveBeenCalledWith([
                `${TEXT_AREA_FIELD_MOCK.variable}@ToggleEditor`
            ]);
            expect(noPropertySpectator.component.$displayedEditor()).toBe(
                AvailableEditorTextArea.Monaco
            );
        });
    });
});
