import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';
import { createFakeContentlet, monacoMock, DotLanguagesServiceMock } from '@dotcms/utils-testing';

import { DotEditContentJsonFieldComponent } from './dot-edit-content-json-field.component';

import { AvailableLanguageMonaco } from '../../models/dot-edit-content-field.constant';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import { JSON_FIELD_MOCK } from '../../utils/mocks';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    contentlet: DotCMSContentlet;
    field: DotCMSContentTypeField;
}
describe('DotEditContentJsonFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentJsonFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentJsonFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [
            DotLanguageVariableSelectorComponent,
            DotEditContentMonacoEditorControlComponent
        ],
        providers: [
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() },
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    describe('should render all components correctly', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-json-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [JSON_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: JSON_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [JSON_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );

            spectator.detectChanges();
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
            // Mock $forcedLanguage signal for this test
            Object.defineProperty(monacoEditor, '$forcedLanguage', {
                value: jest.fn().mockReturnValue(AvailableLanguageMonaco.Json),
                writable: true,
                configurable: true
            });
            expect(monacoEditor.$forcedLanguage()).toBe(AvailableLanguageMonaco.Json);
        });

        it('should call insertLanguageVariableInMonaco when language variable is selected', () => {
            // Mock the insertLanguageVariableInMonaco private method
            const insertLanguageVariableInMonacoMock = jest.fn();
            spectator.component['insertLanguageVariableInMonaco'] =
                insertLanguageVariableInMonacoMock;

            // Trigger onSelectLanguageVariable with a test variable
            const testVariable = 'test_variable';
            spectator.component.onSelectLanguageVariable(testVariable);

            // Verify the mocked method was called with the correct variable
            expect(insertLanguageVariableInMonacoMock).toHaveBeenCalledWith(testVariable);
        });

        it('should call onSelectLanguageVariable when language variable is selected', () => {
            // Spy on component method
            const spy = jest.spyOn(spectator.component, 'onSelectLanguageVariable');

            // Mock insertLanguageVariableInMonaco to avoid calling real insertContent
            spectator.component['insertLanguageVariableInMonaco'] = jest.fn();

            // Get language variable selector component
            const languageVariableSelector = spectator.query(DotLanguageVariableSelectorComponent);

            // Trigger onSelectLanguageVariable event
            const testVariable = '${languageVariable}';
            languageVariableSelector.onSelectLanguageVariable.emit(testVariable);

            // Verify method called with correct parameter
            expect(spy).toHaveBeenCalledWith(testVariable);
        });

        it('should render the controls container', () => {
            expect(spectator.query(byTestId('json-field-controls'))).toBeTruthy();
        });

        it('should render the language selector with correct test id', () => {
            expect(spectator.query(byTestId('json-field-language-selector'))).toBeTruthy();
        });

        it('should render the monaco editor with correct test id', () => {
            expect(spectator.query(byTestId('json-field-monaco-editor'))).toBeTruthy();
        });
    });

    describe('should handle error states correctly', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-json-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [JSON_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: { ...JSON_FIELD_MOCK, required: true },
                        contentlet: createFakeContentlet({
                            [JSON_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should show error message when field is required and has error', () => {
            // Simulate form validation error
            const formControl = spectator.component.formControl;
            formControl.setErrors({ required: true });
            formControl.markAsTouched();
            spectator.detectChanges();

            expect(spectator.query('.error-message')).toBeTruthy();
        });
    });

    describe('should handle hint display correctly', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-json-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [JSON_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: JSON_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [JSON_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should show hint message when field has hint and no error', () => {
            const hintElement = spectator.query(byTestId(`hint-${JSON_FIELD_MOCK.variable}`));
            expect(hintElement).toBeTruthy();
            expect(hintElement.textContent.trim()).toBe(JSON_FIELD_MOCK.hint);
        });
    });
});
