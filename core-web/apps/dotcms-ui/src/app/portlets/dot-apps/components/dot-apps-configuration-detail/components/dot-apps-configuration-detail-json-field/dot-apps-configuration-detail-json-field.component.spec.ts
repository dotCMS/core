import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { Component, input, output } from '@angular/core';
import { FormControl } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

import {
    DotAppsConfigurationDetailJsonFieldComponent,
    getJsonParseError
} from './dot-apps-configuration-detail-json-field.component';

/** Stands in for `ngx-monaco-editor`, which needs a global `monaco` that tests don't load. */
@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'ngx-monaco-editor',
    template: ''
})
class MonacoEditorStubComponent {
    options = input<Record<string, unknown>>();
    init = output<FakeEditor>();
}

/** Minimal editor: setValue fires the content listener, like Monaco does. */
class FakeEditor {
    value = '';
    #listener: () => void = () => {
        //
    };
    getValue = () => this.value;
    setValue = (value: string) => {
        this.value = value;
        this.#listener();
    };
    onDidChangeModelContent = (listener: () => void) => {
        this.#listener = listener;

        return {
            dispose: () => {
                //
            }
        };
    };
    /** Simulates the user typing. */
    type(value: string) {
        this.setValue(value);
    }
}

const VALID_JSON = '{"config":[{"pattern":".*","url":"https://myspa.com:3000"}]}';
const INVALID_JSON = '{"config": [';

describe('DotAppsConfigurationDetailJsonFieldComponent', () => {
    let spectator: Spectator<DotAppsConfigurationDetailJsonFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotAppsConfigurationDetailJsonFieldComponent,
        overrideComponents: [
            [
                DotAppsConfigurationDetailJsonFieldComponent,
                {
                    remove: { imports: [MonacoEditorModule] },
                    add: { imports: [MonacoEditorStubComponent] }
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, {
                get: (key: string, ...args: string[]) => [key, ...args].join(' ')
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({ props: { fieldId: 'configuration' } as unknown });
    });

    describe('getJsonParseError', () => {
        it('should return null for valid JSON', () => {
            expect(getJsonParseError(VALID_JSON)).toBeNull();
        });

        it('should return null for empty values', () => {
            expect(getJsonParseError('')).toBeNull();
            expect(getJsonParseError('   ')).toBeNull();
            expect(getJsonParseError(null)).toBeNull();
        });

        it('should return the parse error for invalid JSON', () => {
            expect(getJsonParseError(INVALID_JSON)).toEqual(expect.any(String));
        });
    });

    describe('validate', () => {
        it('should be valid for valid JSON', () => {
            expect(spectator.component.validate(new FormControl(VALID_JSON))).toBeNull();
        });

        it('should flag invalid JSON', () => {
            expect(spectator.component.validate(new FormControl(INVALID_JSON))).toEqual({
                invalidJson: { message: expect.any(String) }
            });
        });
    });

    it('should give the wrapper the field id so the label points at it', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('json-field-editor-wrapper')).id).toBe('configuration');
    });

    it('should show the valid message for valid JSON', () => {
        spectator.component.writeValue(VALID_JSON);
        spectator.detectChanges();

        expect(spectator.query(byTestId('json-field-valid'))).toBeTruthy();
        expect(spectator.query(byTestId('json-field-error'))).toBeFalsy();
    });

    it('should show the parse error for invalid JSON', () => {
        spectator.component.writeValue(INVALID_JSON);
        spectator.detectChanges();

        const error = spectator.query(byTestId('json-field-error'));
        expect(error.textContent).toContain('apps.json.field.invalid');
    });

    it('should format valid JSON and notify the form', () => {
        const onChange = vi.fn();
        spectator.component.registerOnChange(onChange);
        spectator.component.writeValue(VALID_JSON);
        spectator.detectChanges();

        spectator.click(byTestId('json-field-format'));

        const formatted = JSON.stringify(JSON.parse(VALID_JSON), null, 4);
        expect(spectator.component.$value()).toBe(formatted);
        expect(onChange).toHaveBeenCalledWith(formatted);
    });

    it('should disable the format button for invalid JSON', () => {
        spectator.component.writeValue(INVALID_JSON);
        spectator.detectChanges();

        expect(spectator.query<HTMLButtonElement>(byTestId('json-field-format')).disabled).toBe(
            true
        );
    });

    it('should make the editor read-only when the control is disabled', () => {
        spectator.component.setDisabledState(true);

        expect(spectator.component.$editorOptions().readOnly).toBe(true);
    });
    describe('editor sync', () => {
        const initEditor = (editor: FakeEditor) =>
            spectator.query(MonacoEditorStubComponent).init.emit(editor);

        it('should load the current value into a newly created editor without echoing it', () => {
            const onChange = vi.fn();
            spectator.component.registerOnChange(onChange);
            spectator.component.writeValue(VALID_JSON);
            spectator.detectChanges();

            const editor = new FakeEditor();
            initEditor(editor);

            expect(editor.value).toBe(VALID_JSON);
            expect(onChange).not.toHaveBeenCalled();
            expect(spectator.component.$value()).toBe(VALID_JSON);
        });

        it('should push user edits to the form', () => {
            const onChange = vi.fn();
            spectator.component.registerOnChange(onChange);
            spectator.detectChanges();
            const editor = new FakeEditor();
            initEditor(editor);

            editor.type(VALID_JSON);

            expect(onChange).toHaveBeenCalledWith(VALID_JSON);
        });

        it('should write values set later by the form into the editor', () => {
            spectator.detectChanges();
            const editor = new FakeEditor();
            initEditor(editor);

            spectator.component.writeValue(VALID_JSON);
            spectator.detectChanges();

            expect(editor.value).toBe(VALID_JSON);
        });
    });
});
