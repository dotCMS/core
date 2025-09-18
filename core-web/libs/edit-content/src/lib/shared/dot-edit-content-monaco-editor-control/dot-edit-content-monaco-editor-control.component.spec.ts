import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { FormGroup, FormControl, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { monacoMock } from '@dotcms/utils-testing';

import { DotEditContentMonacoEditorControlComponent } from './dot-edit-content-monaco-editor-control.component';

import { WYSIWYG_MOCK } from '../../fields/dot-edit-content-wysiwyg-field/mocks/dot-edit-content-wysiwyg-field.mock';
import {
    AvailableLanguageMonaco,
    DEFAULT_MONACO_LANGUAGE,
    DEFAULT_MONACO_CONFIG
} from '../../models/dot-edit-content-field.constant';

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
    field: DotCMSContentTypeField;
    forceLanguage?: AvailableLanguageMonaco;
}

describe('DotEditContentMonacoEditorControlComponent', () => {
    let spectator: SpectatorHost<DotEditContentMonacoEditorControlComponent, MockFormComponent>;
    let component: DotEditContentMonacoEditorControlComponent;

    const createHost = createHostFactory({
        component: DotEditContentMonacoEditorControlComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule, MonacoEditorModule],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-monaco-editor-control [field]="field" [formControlName]="field.variable" [forceLanguage]="forceLanguage" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [WYSIWYG_MOCK.variable]: new FormControl()
                    }),
                    field: WYSIWYG_MOCK
                }
            }
        );
        component = spectator.component;
    });

    it('should set default language', () => {
        spectator.detectChanges();
        expect(component.$language()).toBe(DEFAULT_MONACO_LANGUAGE);
    });

    it('should generate correct Monaco options', async () => {
        const expectedOptions = {
            ...DEFAULT_MONACO_CONFIG,
            theme: 'vs',
            language: 'plaintext' // due the auto detect language is plaintext
        };

        // Wait for any potential asynchronous operations to complete
        await spectator.fixture.whenStable();

        // Force change detection
        spectator.detectChanges();

        expect(component.$monacoOptions()).toEqual(expectedOptions);
    });

    it('should use forcedLanguage when provided', () => {
        expect(component.$language()).toBe(DEFAULT_MONACO_LANGUAGE);
        spectator.detectChanges();
        // Now set forced language
        spectator.setHostInput('forceLanguage', AvailableLanguageMonaco.Velocity);
        spectator.detectChanges();

        // Verify that the monaco options uses the forced language
        const options = component.$monacoOptions();
        expect(options.language).toBe(AvailableLanguageMonaco.Velocity);
    });

    it('should use forcedLanguage when provided', () => {
        spectator.detectChanges();
        spectator.setHostInput('forceLanguage', AvailableLanguageMonaco.Javascript);
        spectator.detectChanges();
        const options = component.$monacoOptions();
        expect(options.language).toBe(AvailableLanguageMonaco.Javascript);
    });

    it('should parse custom props from field variables', () => {
        const customProps = { theme: 'vs-dark' };
        const fieldWithVariables = {
            ...WYSIWYG_MOCK,
            fieldVariables: [
                {
                    key: 'monacoOptions',
                    value: JSON.stringify(customProps)
                }
            ]
        } as DotCMSContentTypeField;

        spectator.setHostInput('field', fieldWithVariables);
        spectator.detectChanges();

        const expectedOptions = {
            ...DEFAULT_MONACO_CONFIG,
            ...customProps,
            language: 'plaintext' // due the auto detect language is plaintext
        };
        expect(component.$monacoOptions()).toEqual(expectedOptions);
    });

    it('should register Velocity language when Monaco is loaded', () => {
        const registerSpy = jest.spyOn(component, 'registerVelocityLanguage');
        spectator.detectChanges();
        expect(registerSpy).toHaveBeenCalled();
    });
});
