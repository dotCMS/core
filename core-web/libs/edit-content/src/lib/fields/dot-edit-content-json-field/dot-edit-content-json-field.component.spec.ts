import { MonacoEditorComponent, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { Spectator, byTestId } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import {
    ControlContainer,
    FormControl,
    FormGroup,
    FormGroupDirective,
    FormsModule,
    ReactiveFormsModule
} from '@angular/forms';

import { DotEditContentJsonFieldComponent } from './dot-edit-content-json-field.component';

import { JSON_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentJsonFieldComponent', () => {
    describe('test with value', () => {
        let spectator: Spectator<DotEditContentJsonFieldComponent>;

        const FAKE_FORM_GROUP = new FormGroup({
            json: new FormControl("{ 'test': 'test' }")
        });

        const createComponent = createComponentFactory({
            component: DotEditContentJsonFieldComponent,
            imports: [FormsModule, ReactiveFormsModule, MonacoEditorModule],
            declarations: [MockComponent(MonacoEditorComponent)],
            componentViewProviders: [
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock(FAKE_FORM_GROUP)
                }
            ],
            providers: [FormGroupDirective],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
            spectator.setInput('field', JSON_FIELD_MOCK);
            spectator.detectComponentChanges();
        });

        it('should render the Monoaco Editor with Current Value', () => {
            const monacoEditorComponent = spectator.query(MonacoEditorComponent);
            expect(monacoEditorComponent).not.toBeNull();
        });

        it('should have the form Variable as a FormControlName', () => {
            const element = spectator.query('ngx-monaco-editor');
            expect(element.getAttribute('ng-reflect-name')).toBe(JSON_FIELD_MOCK.variable);
        });

        it('should have the right editor options', () => {
            const monacoEditorComponent = spectator.query(MonacoEditorComponent);

            expect(monacoEditorComponent.options).toEqual({
                theme: 'vs',
                minimap: {
                    enabled: false
                },
                cursorBlinking: 'solid',
                overviewRulerBorder: false,
                mouseWheelZoom: false,
                lineNumbers: 'on',
                roundedSelection: false,
                automaticLayout: true,
                language: 'json'
            });
        });

        describe('container', () => {
            it('should be resizible and min-heigh 9.375rem', () => {
                const element = spectator.query(byTestId('json-field-container'));
                const style = getComputedStyle(element);
                const resizeValue = style.getPropertyValue('resize');
                expect(resizeValue).toBe('vertical');
            });
        });
    });
});
