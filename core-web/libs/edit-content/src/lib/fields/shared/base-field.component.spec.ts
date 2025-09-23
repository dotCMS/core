import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { Component, input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeTextField } from '@dotcms/utils-testing';

import { BaseFieldComponent } from './base-field.component';

const FIELD_MOCK = createFakeTextField({
    variable: 'field'
});

@Component({
    selector: 'dot-base-field-component',
    template: ''
})
export class BaseFieldComponentMock extends BaseFieldComponent {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    writeValue(_: unknown): void {
        // Do nothing
    }
}

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

describe('BaseFieldComponent', () => {
    let spectator: SpectatorHost<BaseFieldComponentMock, MockFormComponent>;

    const createHost = createHostFactory({
        component: BaseFieldComponentMock,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: []
    });

    beforeEach(() => {
        spectator = createHost(
            `<form [formGroup]="formGroup"><dot-base-field-component [field]="field" [formControlName]="field.variable" /></form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [FIELD_MOCK.variable]: new FormControl()
                    }),
                    field: FIELD_MOCK
                }
            }
        );
    });

    it('should create the component', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();

        spectator.hostComponent.formGroup.controls[FIELD_MOCK.variable].setValue('change');
        expect(spectator.component.formControl.value).toBe('change');
    });
});
