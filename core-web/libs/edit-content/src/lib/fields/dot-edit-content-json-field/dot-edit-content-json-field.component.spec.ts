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

import {
    DEFAULT_JSON_FIELD_EDITOR_CONFIG,
    DotEditContentJsonFieldComponent
} from './dot-edit-content-json-field.component';

import { createFormGroupDirectiveMock, JSON_FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentJsonFieldComponent', () => {
    describe('test with value', () => {
        let spectator: Spectator<DotEditContentJsonFieldComponent>;
        let controlContainer: ControlContainer;

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
            controlContainer = spectator.inject(ControlContainer, true);
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
            expect(monacoEditorComponent.options).toEqual(DEFAULT_JSON_FIELD_EDITOR_CONFIG);
        });

        it('should called markForCheck when the value changes', () => {
            const spy = jest.spyOn(spectator.component['cd'], 'markForCheck');

            controlContainer.control.get(JSON_FIELD_MOCK.variable).setValue('{ "test": "test" }');

            expect(spy).toHaveBeenCalled();
        });
    });
});
