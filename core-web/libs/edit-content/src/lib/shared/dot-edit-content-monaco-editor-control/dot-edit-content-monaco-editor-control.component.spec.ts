import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { ControlContainer } from '@angular/forms';

import { monacoMock } from '@dotcms/utils-testing';

import { DotEditContentMonacoEditorControlComponent } from './dot-edit-content-monaco-editor-control.component';

import {
    DEFAULT_MONACO_LANGUAGE,
    DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG
} from '../../fields/dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.constant';
import { WYSIWYG_MOCK } from '../../fields/dot-edit-content-wysiwyg-field/mocks/dot-edit-content-wysiwyg-field.mock';
import { createFormGroupDirectiveMock } from '../../utils/mocks';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

describe('DotEditContentMonacoEditorControlComponent', () => {
    let spectator: Spectator<DotEditContentMonacoEditorControlComponent>;
    let component: DotEditContentMonacoEditorControlComponent;

    const createComponent = createComponentFactory({
        component: DotEditContentMonacoEditorControlComponent,
        imports: [MonacoEditorModule],
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK
            } as unknown
        });

        component = spectator.component;
    });

    it('should set default language', () => {
        expect(component.$language()).toBe(DEFAULT_MONACO_LANGUAGE);
    });

    it('should generate correct Monaco options', async () => {
        const expectedOptions = {
            ...DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG,
            theme: 'vs',
            language: 'plaintext' // due the auto detect language is plaintext
        };

        // Wait for any potential asynchronous operations to complete
        await spectator.fixture.whenStable();

        // Force change detection
        spectator.detectChanges();

        expect(component.$monacoOptions()).toEqual(expectedOptions);
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
        };
        spectator.setInput('field', fieldWithVariables);

        const expectedOptions = {
            ...DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG,
            ...customProps,
            language: 'plaintext' // due the auto detect language is plaintext
        };
        expect(component.$monacoOptions()).toEqual(expectedOptions);
    });

    it('should register Velocity language when Monaco is loaded', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const registerSpy = jest.spyOn(component as any, 'registerVelocityLanguage');
        component.ngOnInit();
        expect(registerSpy).toHaveBeenCalled();
    });
});
