import { MonacoEditorComponent, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { Spectator } from '@ngneat/spectator';
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
        });

        it('should render the Monoaco Editor with Current Value', () => {
            spectator.setInput('field', JSON_FIELD_MOCK);
            spectator.detectComponentChanges();
            const monacoEditorComponent = spectator.query(MonacoEditorComponent);
            expect(monacoEditorComponent).not.toBeNull();
        });

        it('should have the form Variable as a FormControlName', () => {
            spectator.setInput('field', JSON_FIELD_MOCK);
            spectator.detectComponentChanges();
            const element = spectator.query('ngx-monaco-editor');
            expect(element.getAttribute('ng-reflect-name')).toBe(JSON_FIELD_MOCK.variable);
        });
    });
});
