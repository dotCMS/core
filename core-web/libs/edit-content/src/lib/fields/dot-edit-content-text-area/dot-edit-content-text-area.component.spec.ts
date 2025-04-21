import { expect, it, test } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';

import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area.component';
import {
    AvailableEditorTextArea,
    TextAreaEditorOptions
} from './dot-edit-content-text-area.constants';

import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import { createFormGroupDirectiveMock, TEXT_AREA_FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentTextAreaComponent', () => {
    let spectator: Spectator<DotEditContentTextAreaComponent>;
    let textArea: Element;
    let component: DotEditContentTextAreaComponent;

    const createComponent = createComponentFactory({
        component: DotEditContentTextAreaComponent,

        componentMocks: [
            DotLanguageVariableSelectorComponent,
            DotEditContentMonacoEditorControlComponent
        ],
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [
            FormGroupDirective,
            mockProvider(DotLanguagesService),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        spectator.setInput('field', TEXT_AREA_FIELD_MOCK);
        spectator.detectChanges();

        component = spectator.component;
        textArea = spectator.query(byTestId(TEXT_AREA_FIELD_MOCK.variable));
    });

    test.each([
        {
            variable: TEXT_AREA_FIELD_MOCK.variable,
            attribute: 'id'
        },
        {
            variable: TEXT_AREA_FIELD_MOCK.variable,
            attribute: 'ng-reflect-name'
        }
    ])('should have the $variable as $attribute', ({ variable, attribute }) => {
        expect(textArea.getAttribute(attribute)).toBe(variable);
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
        expect(component.$displayedEditor()).toBe(AvailableEditorTextArea.PlainText);

        // Simulate editor change
        component.onEditorChange(AvailableEditorTextArea.Monaco);
        spectator.detectChanges();

        // Verify state change
        expect(component.$displayedEditor()).toBe(AvailableEditorTextArea.Monaco);
    });

    it('should call onSelectLanguageVariable when language variable is selected', () => {
        // Spy on component method
        const spy = jest.spyOn(component, 'onSelectLanguageVariable');

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
        const spy = jest.spyOn(component, 'onEditorChange');

        // Act: Simulate user selecting Monaco editor from dropdown
        component.$selectedEditorDropdown.set(AvailableEditorTextArea.PlainText); // Initial state

        // Simulate onChange event without triggering detectChanges
        component.onEditorChange(AvailableEditorTextArea.Monaco);

        // Assert: Method called and editor switched
        expect(spy).toHaveBeenCalledWith(AvailableEditorTextArea.Monaco);
        expect(component.$displayedEditor()).toBe(AvailableEditorTextArea.Monaco);
    });

    it('should handle inserting language variable when user selects it in plaintext mode', () => {
        // Mock the insertLanguageVariableInTextarea private method
        const insertLanguageVariableInTextareaMock = jest.fn();
        component['insertLanguageVariableInTextarea'] = insertLanguageVariableInTextareaMock;

        // Keep in PlainText mode
        component.$displayedEditor.set(AvailableEditorTextArea.PlainText);

        // Act: Simulate language variable selection
        const testVariable = '${testLanguageVariable}';
        component.onSelectLanguageVariable(testVariable);

        // Assert: Private method called with correct parameters
        expect(insertLanguageVariableInTextareaMock).toHaveBeenCalled();
        expect(insertLanguageVariableInTextareaMock.mock.calls[0][1]).toBe(testVariable);
    });

    it('should insert language variable into Monaco editor when in Monaco mode', () => {
        // Mock the insertLanguageVariableInMonaco private method
        const insertLanguageVariableInMonacoMock = jest.fn();
        component['insertLanguageVariableInMonaco'] = insertLanguageVariableInMonacoMock;

        // Switch to Monaco editor mode
        component.$displayedEditor.set(AvailableEditorTextArea.Monaco);

        // Act: Simulate language variable selection
        const testVariable = '${monacoTestVariable}';
        component.onSelectLanguageVariable(testVariable);

        // Assert: Private method called with correct parameter
        expect(insertLanguageVariableInMonacoMock).toHaveBeenCalledWith(testVariable);
    });
});
