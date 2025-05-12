import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';
import { monacoMock } from '@dotcms/utils-testing';

import { DotEditContentJsonFieldComponent } from './dot-edit-content-json-field.component';

import { AvailableLanguageMonaco } from '../../models/dot-edit-content-field.constant';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import { JSON_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

describe('DotEditContentJsonFieldComponent', () => {
    let spectator: Spectator<DotEditContentJsonFieldComponent>;
    let component: DotEditContentJsonFieldComponent;

    const createComponent = createComponentFactory({
        component: DotEditContentJsonFieldComponent,
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
        providers: [FormGroupDirective, provideHttpClient(), provideHttpClientTesting()]
    });

    beforeEach(() => {
        // Limpiar cualquier llamada anterior a los mocks
        jest.clearAllMocks();

        spectator = createComponent({
            detectChanges: false
        });
        spectator.setInput('field', JSON_FIELD_MOCK);
        spectator.detectChanges();

        component = spectator.component;
    });

    it('should render the component container', () => {
        expect(spectator.query(byTestId('json-field-container'))).toBeTruthy();
    });

    it('should render the language variable selector', () => {
        const languageVariableSelector = spectator.query(DotLanguageVariableSelectorComponent);
        expect(languageVariableSelector).toBeTruthy();
    });

    it('should render the editor container', () => {
        expect(spectator.query(byTestId('json-field-editor'))).toBeTruthy();
    });

    it('should render the monaco editor component', () => {
        const monacoEditor = spectator.query(DotEditContentMonacoEditorControlComponent);
        expect(monacoEditor).toBeTruthy();
    });

    it('should pass JSON as forced language to monaco editor', () => {
        const monacoEditor = spectator.query(DotEditContentMonacoEditorControlComponent);
        expect(monacoEditor.$forcedLanguage()).toBe(AvailableLanguageMonaco.Json);
    });

    it('should call insertLanguageVariableInMonaco when language variable is selected', () => {
        // Mock the insertLanguageVariableInMonaco private method
        const insertLanguageVariableInMonacoMock = jest.fn();
        component['insertLanguageVariableInMonaco'] = insertLanguageVariableInMonacoMock;

        // Trigger onSelectLanguageVariable with a test variable
        const testVariable = 'test_variable';
        component.onSelectLanguageVariable(testVariable);

        // Verify the mocked method was called with the correct variable
        expect(insertLanguageVariableInMonacoMock).toHaveBeenCalledWith(testVariable);
    });
});
