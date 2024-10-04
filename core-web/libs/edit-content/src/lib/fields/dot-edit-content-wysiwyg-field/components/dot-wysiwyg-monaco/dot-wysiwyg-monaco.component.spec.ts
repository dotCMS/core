import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { ControlContainer } from '@angular/forms';

import { monacoMock } from '@dotcms/utils-testing';

import { DotWysiwygMonacoComponent } from './dot-wysiwyg-monaco.component';

import { createFormGroupDirectiveMock } from '../../../../utils/mocks';
import {
    DEFAULT_MONACO_LANGUAGE,
    DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG
} from '../../dot-edit-content-wysiwyg-field.constant';
import { WYSIWYG_MOCK } from '../../mocks/dot-edit-content-wysiwyg-field.mock';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

describe('DotWysiwygMonacoComponent', () => {
    let spectator: Spectator<DotWysiwygMonacoComponent>;

    const createComponent = createComponentFactory({
        component: DotWysiwygMonacoComponent,
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
    });

    it('should set default language', () => {
        expect(spectator.component.$language()).toBe(DEFAULT_MONACO_LANGUAGE);
    });

    it('should set custom language', () => {
        const customLanguage = 'javascript';
        spectator.setInput('language', customLanguage);
        expect(spectator.component.$language()).toBe(customLanguage);
    });

    it('should generate correct Monaco options', () => {
        const expectedOptions = {
            ...DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG,
            language: DEFAULT_MONACO_LANGUAGE
        };
        expect(spectator.component.$monacoOptions()).toEqual(expectedOptions);
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
            language: DEFAULT_MONACO_LANGUAGE
        };
        expect(spectator.component.$monacoOptions()).toEqual(expectedOptions);
    });
});
